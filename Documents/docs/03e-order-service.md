# Order Service Low-Level Deep Dive

The Order Service (`order-service`) handles the customer's shopping cart, checkout processing, order lifecycle events, and logs buyer analytics. It communicates synchronously with the `product-service` via REST template mappings to fetch metadata, reduce stock during checkout, and revert stock when orders are cancelled.

---

## 1. Application Configuration (`application.yml`)
Path: [application.yml](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/main/resources/application.yml)

### Configurations:
*   **`server.port: 8084`**: Runs on port `8084`.
*   **`spring.data.mongodb.uri`**: Connects to the Mongo database `buy01` on host `mongodb:27017`.
*   **`spring.data.mongodb.auto-index-creation: true`**: Automatically creates MongoDB index collections.
*   **`product.service.url`**: Configures downstream `product-service` address (defaults to `http://localhost:8082` or `${PRODUCT_SERVICE_URL}`).

---

## 2. Models & DTOs

### `OrderStatus.java` (Enum)
Path: [OrderStatus.java](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/main/java/com/example/order/model/OrderStatus.java)
Defines order lifecycle phases: `PENDING`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`.

### `Order.java` & `OrderItem.java` (Models)
Path: [Order.java](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/main/java/com/example/order/model/Order.java)
*   **`Order` fields**: `id`, `orderNumber`, `userId`, `items` (List of `OrderItem`), `totalAmount`, `status` (OrderStatus), `paymentMethod`, `shippingAddress`, `trackingNumber`, `createdAt`, `updatedAt`.
*   **`OrderItem` fields**: `productId`, `quantity` (int), `unitPrice` (`BigDecimal`). Calculates `totalPrice` as `unitPrice * quantity`.

### `ShoppingCart.java` & `CartItem.java` (Models)
Path: [ShoppingCart.java](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/main/java/com/example/order/model/ShoppingCart.java)
*   **`ShoppingCart` fields**: `id`, `userId`, `items` (List of `CartItem`), `totalAmount`, `createdAt`, `updatedAt`.
*   **`CartItem` fields**: `productId`, `quantity` (int), `priceAtTime` (`BigDecimal`).

### DTO Classes:
*   **`ProductDTO.java`**: Holds product details fetched from product-service (`id`, `name`, `description`, `price`, `quantity`).
*   **`CartDTO.java` / `CartItemDTO.java`**: Formats active shopping cart data for the frontend UI.
*   **`OrderDTO.java` / `OrderItemDTO.java`**: Formats completed order history records.
*   **`CheckoutRequest.java`**: Contains checkout inputs (`shippingAddress`, `paymentMethod`).
*   **`BuyerAnalyticsDTO.java`**: Aggregates customer total purchase spending and top products list.
*   **`ReduceStockItem.java` / `RevertStockItem.java`**: Maps request bodies for inventory updates sent to the product-service.

---

## 3. Services

### `OrderService.java`
Path: [OrderService.java](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/main/java/com/example/order/service/OrderService.java)

#### Methods:
*   `public OrderDTO getOrderById(String orderId)`:
    *   **Logic**: Fetches order from database. Converts it to DTO, fetching product names via `getProductName()` helper.
*   `public Page<OrderDTO> getUserOrders(String userId, Pageable pageable)`:
    *   **Logic**: Paginated fetch of orders matching `userId`. Returns converted DTO page.
*   `public Page<OrderDTO> getUserOrdersByStatus(...)`:
    *   **Logic**: Paginated fetch filtering by status.
*   `public Page<OrderDTO> searchUserOrders(String userId, String orderNumber, Pageable pageable)`:
    *   **Logic**: Searches orders matching search substring of `orderNumber`.
*   `public OrderDTO updateOrderStatus(String orderId, OrderStatus newStatus)`:
    *   **Logic**: Checks status transition validity using `validateStatusTransition()`. Updates status and updates timestamp.
*   `public void cancelOrder(String orderId)`:
    *   **Logic**: Handles order cancellations and returns stock to inventory:
        1.  Validates that order is not already delivered or cancelled.
        2.  Creates a list of `RevertStockItem` containing the product ID, quantity, and item subtotal.
        3.  Executes a POST request to `/internal/products/revert-stock` in the product-service using `RestTemplate`.
        4.  Catches communications errors and throws `ServiceUnavailableException`.
        5.  Updates status to `CANCELLED` and saves.
*   `public OrderDTO redoOrder(String orderId)`:
    *   **Logic**: Duplicates items from a previous order into a new order in `PENDING` state. (Note: Frontend intercepts this to reload items into the active cart for checkout confirmation).
*   `private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus)`:
    *   **Logic**: Validates state progression rules:
        *   `PENDING` -> `PROCESSING` or `CANCELLED`
        *   `PROCESSING` -> `SHIPPED` or `CANCELLED`
        *   `SHIPPED` -> `DELIVERED`
        *   Throws `IllegalArgumentException` on invalid transitions.

### `CartService.java`
Path: [CartService.java](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/main/java/com/example/order/service/CartService.java)

#### Methods:
*   `public CartDTO getCart(String userId)`:
    *   **Logic**: Finds cart or creates a new one.
*   `public CartDTO addToCart(String userId, String productId, int quantity)`:
    *   **Logic**: Adds product to cart:
        1.  Calls `fetchProduct(productId)` via rest request to verify existence and fetch inventory.
        2.  If inventory less than requested, throws `IllegalArgumentException`.
        3.  Finds/creates user cart. Checks if item already exists. If yes, increments quantity (re-verifying stock limit). If no, adds new `CartItem`.
        4.  Recalculates total and saves.
*   `public CartDTO updateCartItem(String userId, String itemId, int quantity)`:
    *   **Logic**: Changes quantity of cart item. Removes item if quantity is <= 0. Otherwise, verifies inventory and updates quantity.
*   `public CartDTO removeCartItem(String userId, String itemId)`:
    *   **Logic**: Filters out item, recalculates total, and saves.
*   `public CartDTO clearCart(String userId)`:
    *   **Logic**: Clears items array, sets `totalAmount` to zero, and saves.
*   `public OrderDTO checkoutCart(String userId, CheckoutRequest request)`:
    *   **Logic**: Processes order checkout:
        1.  Verifies cart is not empty.
        2.  Verifies each item has sufficient stock by querying product-service.
        3.  Creates a list of `ReduceStockItem` containing product IDs and quantities.
        4.  Executes a POST request to `/internal/products/reduce-stock` in the product-service.
        5.  On success, creates a new `Order` document with generated order number (`ORD-{timestamp}`), items, total price, and 25% tax calculations.
        6.  Saves order, clears cart, and returns order DTO.

### `BuyerAnalyticsService.java`
Path: [BuyerAnalyticsService.java](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/main/java/com/example/order/service/BuyerAnalyticsService.java)

#### Methods:
*   `public BuyerAnalyticsDTO getBuyerAnalytics(String userId)`:
    *   **Logic**: Fetches all orders belonging to `userId`. Loops through items, aggregating totals:
        *   Accumulates quantity, totalPrice, and purchase count per product using a helper class `ProductPurchaseStats` mapped by product ID.
        *   Sorts product stats map descending by quantity, limits to top 5, queries product-service to attach names, and returns completed analytics DTO.

---

## 4. Controllers

*   **`OrderController.java`**: Matches `/api/v1/orders` pathway. Provides endpoints for fetching orders, searching, changing order status (Requires ADMIN role), cancelling, and redoing orders.
*   **`CartController.java`**: Matches `/api/v1/cart` pathway. Provides endpoints for retrieving active cart, adding items, updating quantities, removing items, clearing cart, and checking out.
*   **`BuyerAnalyticsController.java`**: Exposes GET `/api/v1/buyers/{userId}/analytics` endpoint to retrieve user spending stats.

---

## 5. Test Summary

### Core Test Files:
1.  **`CartServiceTest.java`**: Asserts cart CRUDS, inventory checks during adding items, stock reductions on checkouts, and transactional exceptions handling.
2.  **`OrderServiceTest.java`**: Validates status transition rules, order cancellations, stock reversion triggers, and order duplication logic.
3.  **`OrderModelTest.java`**: Asserts models mapping and default constructor assignments.
