# Frontend Low-Level Deep Dive

This document provides a low-level, class-by-class and function-by-function analysis of the Angular 17+ web application inside `buy-01-frontend`.

---

## 1. Data Models (`src/app/models/`)

### `user.model.ts`
Path: [user.model.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/models/user.model.ts)
*   **Properties**:
    *   `id`: String, user ID.
    *   `name`: String, user display name.
    *   `email`: String, email address.
    *   `role`: `'CLIENT' | 'SELLER'`, role definition.
    *   `avatar`: String (Optional), URL of profile avatar.
    *   `token`: String (Optional), auth token returned on login.

### `product.model.ts`
Path: [product.model.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/models/product.model.ts)
*   **Properties**:
    *   `id`: String, product UUID.
    *   `name`: String, title.
    *   `description`: String, product details.
    *   `price`: Number, retail cost.
    *   `quantity`: Number, stock count.
    *   `category`: String, category label.
    *   `userId`: String, seller ID.
    *   `imageUrls`: String[], list of image URLs.
    *   `salesCount`: Number, total units sold.
    *   `revenue`: Number, total earnings.
    *   `createdAt`: Date, timestamp.
    *   `updatedAt`: Date, timestamp.

### `cart.model.ts`
Path: [cart.model.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/models/cart.model.ts)
*   **`CartItemDTO` fields**: `id`, `productId`, `name`, `quantity`, `price`, `subtotal`.
*   **`CartDTO` fields**: `id`, `userId`, `items` (CartItemDTO[]), `totalPrice`, `itemCount`.
*   **`CheckoutRequest` fields**: `shippingAddress`, `paymentMethod`.

### `order.model.ts`
Path: [order.model.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/models/order.model.ts)
*   **`OrderStatus`**: `'PENDING' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'`.
*   **`OrderItemDTO` fields**: `productId`, `productName`, `quantity`, `unitPrice`, `subtotal`.
*   **`OrderDTO` fields**: `id`, `orderNumber`, `userId`, `items` (OrderItemDTO[]), `totalAmount`, `status` (OrderStatus), `paymentMethod`, `shippingAddress`, `trackingNumber`, `createdAt`, `updatedAt`.
*   **`PagedResponse<T>` fields**: `content` (`T[]`), `totalPages` (`number`), `totalElements` (`number`), `size` (`number`), `number` (`number`), `last` (`boolean`).

---

## 2. Core Angular Services (`src/app/services/`)

### `SessionService`
Path: [session.service.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/services/session.service.ts)
Manages token and user session persistence.

#### Methods:
*   `private getActiveStorage()`:
    *   **Return Type**: `Storage` (Browser API)
    *   **Logic**: Scans `localStorage` then `sessionStorage` for the key `token` or `user`. Returns matching storage engine, defaulting to `localStorage`.
*   `setToken(token: string, remember?: boolean)`:
    *   **Logic**: Removes `token` from both storages, then writes `token` value to target storage (`localStorage` if `remember` is true, otherwise `sessionStorage`).
*   `getToken()`:
    *   **Return Type**: `string | null`
    *   **Logic**: Checks `localStorage` then `sessionStorage` for the token key and returns the value.
*   `setUser(user: User, remember?: boolean)`:
    *   **Logic**: Removes user key, serializes user object to JSON string, and writes to target storage.
*   `getUser()`:
    *   **Return Type**: `User | null`
    *   **Logic**: Retrieves user string from storage. Parses it as `User` and returns, or returns `null` if key is missing.
*   `clear()`:
    *   **Logic**: Clears token and user keys from both `localStorage` and `sessionStorage`.
*   `isAuthenticated()`:
    *   **Return Type**: `boolean`
    *   **Logic**: Returns true if `getToken()` yields a non-null token string.
*   `getRole()`:
    *   **Return Type**: `'CLIENT' | 'SELLER' | null`
    *   **Logic**: Returns user role property if user is logged in, else null.

### `AuthService`
Path: [auth.service.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/services/auth.service.ts)
Handles login and registration HTTP endpoints.

#### Methods:
*   `login(credentials)`:
    *   **Return Type**: `Observable<any>`
    *   **Logic**: Performs a POST call to `/auth/login` containing email and password credentials payload.
*   `register(data)`:
    *   **Return Type**: `Observable<User>`
    *   **Logic**: Performs a POST call to `/auth/register` with user details (name, email, password, role).
*   `logout()`:
    *   **Logic**: Triggers `this.session.clear()` to purge credentials.

### `CartService`
Path: [cart.service.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/services/cart.service.ts)
Manages the user's active shopping cart state.

#### Properties:
*   `cartSubject`: A `BehaviorSubject` holding `CartDTO | null` (current cart value).
*   `cart$`: Exposed observable from `cartSubject` for UI bindings.

#### Methods:
*   `getCart()`:
    *   **Return Type**: `Observable<CartDTO>`
    *   **Logic**: Performs GET call to `/v1/cart`. Intercepts response using `tap` operator to push new cart state to `cartSubject` observers. Times out after 10000ms.
*   `addToCart(productId: string, quantity: number)`:
    *   **Return Type**: `Observable<CartItemDTO>`
    *   **Logic**: Performs POST to `/v1/cart/items?productId={productId}&quantity={quantity}`. Uses `tap` hook to call `this.getCart().subscribe()` to refresh the shopping cart state.
*   `updateCartItem(itemId: string, quantity: number)`:
    *   **Logic**: PUT call to `/v1/cart/items/{itemId}?quantity={quantity}`. Reloads cart upon completion.
*   `removeCartItem(itemId: string)`:
    *   **Logic**: DELETE call to `/v1/cart/items/{itemId}`. Reloads cart.
*   `clearCart()`:
    *   **Logic**: DELETE to `/v1/cart`. Pushes `null` to `cartSubject`.
*   `checkout(request: CheckoutRequest)`:
    *   **Return Type**: `Observable<OrderDTO>`
    *   **Logic**: POST call to `/v1/cart/checkout` with address and payment method. Resets `cartSubject` to null upon success.

### `OrderService`
Path: [order.service.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/services/order.service.ts)
Processes orders.

#### Methods:
*   `getOrderById(id)`: Returns single `OrderDTO` detail record via GET `/v1/orders/{id}`.
*   `getUserOrders(page, size)`: Returns paginated `PagedResponse<OrderDTO>` matching current user via GET `/v1/orders`.
*   *   `getOrdersByStatus(status, page, size)`: GET `/v1/orders/status/{status}`.
*   `searchOrders(orderNumber, page, size)`: GET `/v1/orders/search` filtering by query parameter `orderNumber`.
*   `updateOrderStatus(orderId, status)`: PUT `/v1/orders/{orderId}/status` passing the new status in the request body.
*   `cancelOrder(orderId)`: DELETE `/v1/orders/{orderId}` to trigger cancellation.
*   `redoOrder(orderId)`: POST `/v1/orders/{orderId}/redo` to duplicate order items.

---

## 3. UI Components (`src/app/components/`)

### `OrderList` (Component)
Path: [order-list.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/components/order-list/order-list.ts)
Displays order history logs with filters.

#### Properties:
*   `orders`: Array of OrderDTOs.
*   `searchTerm`: Query string matching order numbers.
*   `selectedStatus`: Filters active tab (`PENDING`, `SHIPPED`, etc.).
*   `currentPage`, `pageSize`, `totalPages`: Pagination control states.
*   `searchSubject`: Subject stream capturing user inputs for debouncing.

#### Methods:
*   `ngOnInit()`:
    *   Subscribes to `searchSubject` stream. Chains `debounceTime(300)` and `distinctUntilChanged()` to trigger `loadOrders()` only after typing ceases. Calls `loadOrders()` initially.
*   `onSearch(event)`:
    *   Pushes input field value to `searchSubject`.
*   `loadOrders()`:
    *   Queries `orderService` dynamically depending on component state:
        *   If `searchTerm` is populated, calls `orderService.searchOrders()`.
        *   Else if `selectedStatus !== 'ALL'`, calls `orderService.getOrdersByStatus()`.
        *   Otherwise, calls `orderService.getUserOrders()`.
        *   Binds result array to `orders`, updates paging counters, disables loading spinner, and runs change detection using `cdr.markForCheck()`.
*   `cancelOrder(event, orderId)`:
    *   Stops propagation to avoid row navigation clicks. Confirms action, then calls `orderService.cancelOrder()` and triggers a reload.
*   `redoOrder(event, orderId)`:
    *   Calls `orderService.redoOrder()`. Navigates to `/cart` upon duplication.

### `OrderDetailComponent` (Component)
Path: [order-detail.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/components/order-detail/order-detail.ts)
Detailed single order tracker.

#### Methods:
*   `ngOnInit()`: Parses ActivatedRoute parameter `id` and calls `loadOrderDetail(id)`.
*   `loadOrderDetail(id)`: Invokes `orderService.getOrderById()`.
*   `cancelOrder()`: Calls `orderService.cancelOrder(id)` and refreshes details page.
*   `redoOrder()`: "Buy again" button action. Takes all items in order and maps them to `cartService.addToCart()` observables. Chains them using `forkJoin` so that all requests run concurrently, displays success toast, and navigates to the `/cart` page.

### `ProductList` (Component)
Path: [product-list.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/components/product-list/product-list.ts)
Displays homepage product grid with category links. Binds filters and maps details view routes.

### `ProductDetail` (Component)
Path: [product-detail.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/components/product-detail/product-detail.ts)
Product page. Features quantity increment inputs and adds item to cart via `cartService.addToCart()`.

### `CartViewComponent` (Component)
Path: [cart-view.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/components/cart-view/cart-view.ts)
Displays current cart items list. Binds `cartService.updateCartItem()` to handle item additions/subtractions, and removals. Includes checkout buttons navigating to `/checkout`.

### `CartCheckout` (Component)
Path: [cart-checkout.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/components/cart-checkout/cart-checkout.ts)
Collects delivery details (address, payment method) and calls `cartService.checkout()` to complete purchase.

### `SellerDashboard` (Component)
Path: [seller-dashboard.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/components/seller-dashboard/seller-dashboard.ts)
Merchant workspace showing products list, total sales, and revenues aggregated from `product-service` backend.

### `ProductForm` (Component)
Path: [product-form.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/components/product-form/product-form.ts)
Form used by merchants to add or edit product items (inputs validation, category select).

### `SellerMedia` (Component)
Path: [seller-media.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/components/seller-media/seller-media.ts)
Handles image uploads for merchant products. Utilizes `FormData` to send files to the `media-service`.

---

## 4. Guards & Interceptors

### `AuthInterceptor`
Path: [auth.interceptor.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/interceptors/auth.interceptor.ts)
An HTTP interceptor. Checks if token exists using `SessionService.getToken()`. If present, clones request and adds headers:
`Authorization: Bearer <token>`
Also reads cookie CSRF token using `HttpXsrfTokenExtractor` and attaches `X-XSRF-TOKEN` header for state-changing HTTP requests (POST, PUT, DELETE), unless path matches public endpoints (`/api/auth/register` or `/api/auth/login`).

### `ErrorInterceptor`
Path: [error.interceptor.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/interceptors/error.interceptor.ts)
Catches global HTTP response errors:
*   `401 Unauthorized`: Purges session cache using `session.clear()`, displays session expired toast warning, and redirects the browser to `/login`.
*   `403 Forbidden`: Displays authorization failure toast.
*   `4xx` / `5xx`: Displays general connection failure warnings.

### `AuthGuard`
Path: [auth.guard.ts](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/guards/auth.guard.ts)
Protects routes from anonymous access:
*   `authGuard`: Validates `isAuthenticated()`. If false, redirects to `/login`.
*   `sellerGuard`: Validates logged-in user role is `'SELLER'`. If not, redirects to index `/`.
*   `clientGuard`: Validates role is `'CLIENT'`.
