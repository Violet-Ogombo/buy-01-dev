# Buy-02 E-Commerce Platform - Complete Implementation Guide

## Overview
This document provides a detailed step-by-step implementation plan for completing the e-commerce platform with shopping cart, user profiles, order management, and search/filtering functionality.

**Timeline Estimate:** 4-6 weeks (depending on team size and complexity)

**Development Workflow:**
1. Work on single `buy-02` branch without committing
2. Implement all database → backend → frontend features locally
3. When complete, create ONE PR for local pipeline validation on second computer
4. Run SonarQube & Jenkins checks, fix issues
5. Merge when all validations pass

---

## Phase 1: Database Design & Implementation

### Step 1.1: Create Development Branch
```bash
cd /Users/aung.min/Desktop/github/buy-01-dev
git checkout main
git pull origin main
git checkout -b buy-02
```

This branch will contain all database, backend, and frontend work. Do NOT commit or push until all features are complete.

### Step 1.2: Database Schema Analysis
Review current MongoDB collections and identify gaps:
- [ ] Check existing collections in MongoDB (buy01 database)
- [ ] Identify missing collections for shopping cart
- [ ] Identify missing collections for orders
- [ ] Identify missing fields for user profiles
- [ ] Document all relationships and references

### Step 1.3: Design New MongoDB Collections

Create initialization script for MongoDB in the respective microservice resources.

**Shopping Cart Collection:**
```json
// Collection: shopping_cart
{
  "_id": ObjectId,
  "userId": ObjectId,
  "items": [
    {
      "productId": ObjectId,
      "quantity": number,
      "priceAtTime": number,
      "addedAt": ISODate
    }
  ],
  "totalAmount": number,
  "createdAt": ISODate,
  "updatedAt": ISODate
}
```

**Orders Collection:**
```json
// Collection: orders
{
  "_id": ObjectId,
  "userId": ObjectId,
  "orderNumber": string (unique),
  "items": [
    {
      "productId": ObjectId,
      "quantity": number,
      "unitPrice": number,
      "totalPrice": number
    }
  ],
  "status": string, // pending, processing, shipped, delivered, cancelled
  "totalAmount": number,
  "paymentMethod": string, // pay_on_delivery, credit_card, etc.
  "shippingAddress": string,
  "trackingNumber": string,
  "createdAt": ISODate,
  "updatedAt": ISODate,
  "deliveredAt": ISODate
}
```

**User Profile Extensions:**
```json
// Extend existing users collection
{
  "_id": ObjectId,
  "username": string,
  "email": string,
  "password": string,
  "phone": string,
  "address": string,
  "totalSpent": number,
  "isSeller": boolean,
  "sellerRevenue": number,
  "createdAt": ISODate,
  "updatedAt": ISODate
}
```

**Seller Products Collection:**
```json
// Collection: seller_products
{
  "_id": ObjectId,
  "sellerId": ObjectId,
  "productId": ObjectId,
  "salesCount": number,
  "revenue": number,
  "createdAt": ISODate
}
```

**Wishlist Collection (Optional/Bonus):**
```json
// Collection: wishlist
{
  "_id": ObjectId,
  "userId": ObjectId,
  "productId": ObjectId,
  "addedAt": ISODate
}
```

### Step 1.4: Implement MongoDB Collections & Indexes
- [ ] Create MongoDB initialization script in `product-service/src/main/resources/mongodb/init-collections.js`
- [ ] Use Spring Data MongoDB for entity mapping
- [ ] Test collections on local MongoDB instance
- [ ] Create indexes for performance in MongoDB initializer:
  ```javascript
  db.shopping_cart.createIndex({ "userId": 1 });
  db.orders.createIndex({ "userId": 1 });
  db.orders.createIndex({ "status": 1 });
  db.orders.createIndex({ "orderNumber": 1 }, { unique: true });
  db.seller_products.createIndex({ "sellerId": 1 });
  db.seller_products.createIndex({ "productId": 1 });
  db.wishlist.createIndex({ "userId": 1, "productId": 1 }, { unique: true });
  ```
- [ ] Update MongoDB configuration in each microservice's `application.yml`:
  ```yaml
  spring:
    data:
      mongodb:
        uri: mongodb://mongodb:27017/buy01
        auto-index-creation: true
  ```
- [ ] Document schema design and indexing strategy

### Step 1.5: Create Spring Data MongoDB Entities
- [ ] Create entity classes in each microservice:
  - `product-service/src/main/java/com/example/products/model/ShoppingCart.java`
  - `order-service/src/main/java/com/example/orders/model/Order.java`
  - `identity-service/src/main/java/com/example/identity/model/User.java`
- [ ] Use Spring Data MongoDB annotations: `@Document`, `@Id`, `@Field`
- [ ] Define repositories extending `MongoRepository`

**⚠️ Do NOT commit yet** - Database design is complete, continue to Phase 2 for backend APIs.

---

## Phase 2: Backend API Development (All Microservices)

### Step 2.0: Create MongoDB Entities & Repositories

**Shopping Cart Entity:**
**File: `product-service/src/main/java/com/example/products/model/ShoppingCart.java`**

```java
@Document(collection = "shopping_cart")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingCart {
    
    @Id
    private String id;
    
    @Field("user_id")
    private String userId;
    
    @Field("items")
    private List<CartItem> items;
    
    @Field("total_amount")
    private BigDecimal totalAmount;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class CartItem {
    private String productId;
    private int quantity;
    private BigDecimal priceAtTime;
    private LocalDateTime addedAt;
}
```

**Shopping Cart Repository:**
```java
@Repository
public interface ShoppingCartRepository extends MongoRepository<ShoppingCart, String> {
    Optional<ShoppingCart> findByUserId(String userId);
}
```

**Order Entity:**
**File: `order-service/src/main/java/com/example/orders/model/Order.java`**

```java
@Document(collection = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    private String id;
    
    @Field("user_id")
    private String userId;
    
    @Field("order_number")
    @Indexed(unique = true)
    private String orderNumber;
    
    @Field("items")
    private List<OrderItem> items;
    
    @Field("status")
    @Indexed
    private OrderStatus status;
    
    @Field("total_amount")
    private BigDecimal totalAmount;
    
    @Field("payment_method")
    private String paymentMethod;
    
    @Field("shipping_address")
    private String shippingAddress;
    
    @Field("tracking_number")
    private String trackingNumber;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    @Field("delivered_at")
    private LocalDateTime deliveredAt;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class OrderItem {
    private String productId;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}

enum OrderStatus {
    PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
}
```

**Order Repository:**
```java
@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    Page<Order> findByUserId(String userId, Pageable pageable);
    Page<Order> findByUserIdAndStatus(String userId, OrderStatus status, Pageable pageable);
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findByUserIdAndOrderNumberContaining(String userId, String orderNumber, Pageable pageable);
}
```

**User Entity Extension:**
**File: `identity-service/src/main/java/com/example/identity/model/User.java`**

```java
@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    private String id;
    
    @Field("username")
    @Indexed(unique = true)
    private String username;
    
    @Field("email")
    @Indexed(unique = true)
    private String email;
    
    @Field("password")
    private String password;
    
    @Field("phone")
    private String phone;
    
    @Field("address")
    private String address;
    
    @Field("total_spent")
    private BigDecimal totalSpent;
    
    @Field("is_seller")
    private boolean isSeller;
    
    @Field("seller_revenue")
    private BigDecimal sellerRevenue;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    @Field("roles")
    private List<String> roles;
}
```

**Wishlist Entity (Bonus):**
```java
@Document(collection = "wishlist")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wishlist {
    
    @Id
    private String id;
    
    @Field("user_id")
    @Indexed
    private String userId;
    
    @Field("product_id")
    @Indexed
    private String productId;
    
    @Field("added_at")
    private LocalDateTime addedAt;
}
```

**Wishlist Repository:**
```java
@Repository
public interface WishlistRepository extends MongoRepository<Wishlist, String> {
    List<Wishlist> findByUserId(String userId);
    Optional<Wishlist> findByUserIdAndProductId(String userId, String productId);
    void deleteByUserIdAndProductId(String userId, String productId);
    long countByUserId(String userId);
}
```

### Step 2.1: Create Orders Microservice APIs

**File: `order-service/src/main/java/com/example/orders/controller/OrderController.java`**

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest request) {
        // Validate request
        // Create order from cart
        // Clear cart
        // Return order details
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable UUID orderId) {
        // Get order by ID
    }
    
    @GetMapping
    public ResponseEntity<Page<OrderDTO>> getUserOrders(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String search
    ) {
        // Get user's orders with pagination
        // Support filtering by status
    }
    
    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
        @PathVariable UUID orderId,
        @RequestBody UpdateStatusRequest request
    ) {
        // Update order status (admin only)
    }
    
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID orderId) {
        // Cancel order (only if status is pending)
    }
}
```

### Step 2.2: Create Shopping Cart APIs

**File: `api-gateway/src/main/java/com/example/gateway/controller/CartController.java`**

```java
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {
    
    @GetMapping
    public ResponseEntity<CartDTO> getCart() {
        // Get user's current cart
    }
    
    @PostMapping("/items")
    public ResponseEntity<CartDTO> addToCart(@RequestBody AddToCartRequest request) {
        // Add product to cart
        // Validate product exists and has stock
        // Increment quantity if already in cart
    }
    
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDTO> updateCartItem(
        @PathVariable UUID itemId,
        @RequestBody UpdateCartItemRequest request
    ) {
        // Update quantity
        // Remove if quantity is 0
    }
    
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDTO> removeFromCart(@PathVariable UUID itemId) {
        // Remove item from cart
    }
    
    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        // Clear all items from cart
    }
    
    @PostMapping("/checkout")
    public ResponseEntity<OrderDTO> checkout(@RequestBody CheckoutRequest request) {
        // Validate cart is not empty
        // Validate payment method
        // Create order
        // Update product stock
        // Clear cart
        // Return order
    }
}
```

### Step 2.3: Create User Profile APIs

**File: `identity-service/src/main/java/com/example/identity/controller/UserProfileController.java`**

```java
@RestController
@RequestMapping("/api/v1/users/profile")
public class UserProfileController {
    
    @GetMapping
    public ResponseEntity<UserProfileDTO> getUserProfile() {
        // Get current user's profile
        // Include: total_spent, order_count, wishlist_count
    }
    
    @PutMapping
    public ResponseEntity<UserProfileDTO> updateUserProfile(
        @RequestBody UpdateProfileRequest request
    ) {
        // Update profile information
        // Email, phone, address, etc.
    }
    
    @GetMapping("/order-history")
    public ResponseEntity<Page<OrderDTO>> getOrderHistory(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        // Get user's orders with pagination
    }
    
    @GetMapping("/seller")
    public ResponseEntity<SellerProfileDTO> getSellerProfile() {
        // For sellers: best-selling products, revenue, etc.
    }
    
    @GetMapping("/stats")
    public ResponseEntity<UserStatsDTO> getUserStats() {
        // Return: favorite_products, frequently_bought, total_spent
    }
}
```

### Step 2.4: Create Search & Filtering APIs

**File: `product-service/src/main/java/com/example/products/controller/SearchController.java`**

```java
@RestController
@RequestMapping("/api/v1/products")
public class SearchController {
    
    @GetMapping("/search")
    public ResponseEntity<Page<ProductDTO>> searchProducts(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        // Search by product name, description, category
        // Use full-text search or LIKE queries
    }
    
    @GetMapping("/filter")
    public ResponseEntity<Page<ProductDTO>> filterProducts(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) Double minRating,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "name") String sortBy,
        @RequestParam(defaultValue = "ASC") String sortOrder
    ) {
        // Filter by category, price range, rating
        // Support sorting
    }
    
    @GetMapping("/{id}/similar")
    public ResponseEntity<List<ProductDTO>> getSimilarProducts(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "5") int limit
    ) {
        // Get similar products from same category
    }
}
```

### Step 2.5: Implement Order Service Logic

**File: `order-service/src/main/java/com/example/orders/service/OrderService.java`**

```java
@Service
public class OrderService {
    
    public Order createOrderFromCart(UUID userId, CheckoutRequest request) {
        // 1. Validate user exists
        // 2. Get user's shopping cart
        // 3. Validate cart is not empty
        // 4. Calculate total amount
        // 5. Validate payment method
        // 6. Create Order entity
        // 7. For each cart item:
        //    - Create OrderItem
        //    - Update product stock (decrease by quantity)
        //    - Update seller revenue
        // 8. Update user's total_spent
        // 9. Clear cart
        // 10. Return created order
    }
    
    public Order updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        // Validate order exists
        // Validate status transition is allowed
        // If status = DELIVERED, update delivery date
        // Return updated order
    }
    
    public Order cancelOrder(UUID orderId) {
        // Validate order exists
        // Validate status is PENDING or PROCESSING
        // Refund: reverse stock and seller revenue
        // Update status to CANCELLED
        // Return updated order
    }
    
    public Page<Order> getUserOrders(UUID userId, Pageable pageable, 
                                     Optional<OrderStatus> status, 
                                     Optional<String> searchTerm) {
        // Query orders with filtering and pagination
    }
}
```

### Step 2.6: Implement Shopping Cart Service Logic

**File: `product-service/src/main/java/com/example/products/service/CartService.java`**

```java
@Service
public class CartService {
    
    public Cart addToCart(UUID userId, UUID productId, int quantity) {
        // 1. Get or create cart for user
        // 2. Validate product exists and has stock
        // 3. If product already in cart, increment quantity
        // 4. Otherwise, create new cart item
        // 5. Calculate and update cart total
        // 6. Persist cart
        // 7. Return cart
    }
    
    public Cart updateCartItem(UUID cartItemId, int newQuantity) {
        // 1. Get cart item
        // 2. Validate new quantity
        // 3. If quantity is 0, delete item
        // 4. Update quantity
        // 5. Recalculate cart total
        // 6. Return cart
    }
    
    public Cart removeFromCart(UUID cartItemId) {
        // Delete cart item
        // Recalculate cart total
        // Return cart
    }
    
    public Cart getCart(UUID userId) {
        // Get user's cart
        // Validate prices haven't changed
        // Return cart with all items
    }
}
```

### Step 2.7: Error Handling & Validation

**Create: `product-service/src/main/java/com/example/products/exception/GlobalExceptionHandler.java`**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("PRODUCT_NOT_FOUND", ex.getMessage()));
    }
    
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INSUFFICIENT_STOCK", ex.getMessage()));
    }
    
    @ExceptionHandler(InvalidOrderStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderStatus(InvalidOrderStatusException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INVALID_ORDER_STATUS", ex.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // Handle validation errors with field-level details
    }
}
```

### Step 2.8: Add Input Validation

**Create: `order-service/src/main/java/com/example/orders/dto/CreateOrderRequest.java`**

```java
public class CreateOrderRequest {
    
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;
    
    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;
    
    @NotNull(message = "Cart items are required")
    @NotEmpty(message = "Cart cannot be empty")
    private List<CartItemDTO> cartItems;
}
```

### Step 2.9: Unit Tests for Microservices

**Create: `order-service/src/test/java/com/example/orders/service/OrderServiceTest.java`**

```java
@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    
    @InjectMocks
    private OrderService orderService;
    
    @Mock
    private OrderRepository orderRepository;
    
    @Test
    public void testCreateOrderSuccess() {
        // Setup mock data
        // Call createOrderFromCart
        // Assert order is created with correct status
        // Assert cart is cleared
        // Assert inventory is updated
    }
    
    @Test
    public void testCreateOrderWithInsufficientStock() {
        // Setup: product has quantity 1, trying to order 5
        // Assert throws InsufficientStockException
    }
    
    @Test
    public void testCancelOrderSuccess() {
        // Setup: existing order with PENDING status
        // Call cancelOrder
        // Assert status is CANCELLED
        // Assert inventory is restored
    }
}
```

**⚠️ Do NOT commit yet** - Backend APIs are complete, continue to Phase 3 for frontend development.

---

## Phase 3: Frontend Development (Angular)

### Step 3.1: Create Shopping Cart Component

**Create: `buy-01-frontend/src/app/components/shopping-cart/shopping-cart.component.ts`**

```typescript
@Component({
  selector: 'app-shopping-cart',
  templateUrl: './shopping-cart.component.html',
  styleUrls: ['./shopping-cart.component.scss']
})
export class ShoppingCartComponent implements OnInit {
  
  cartItems: CartItem[] = [];
  totalAmount: number = 0;
  isLoading: boolean = false;
  errorMessage: string = '';
  
  constructor(private cartService: CartService) {}
  
  ngOnInit() {
    this.loadCart();
  }
  
  loadCart() {
    this.isLoading = true;
    this.cartService.getCart().subscribe({
      next: (cart) => {
        this.cartItems = cart.items;
        this.totalAmount = cart.totalAmount;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load cart';
        this.isLoading = false;
      }
    });
  }
  
  updateQuantity(itemId: string, newQuantity: number) {
    if (newQuantity <= 0) {
      this.removeItem(itemId);
      return;
    }
    
    this.cartService.updateCartItem(itemId, newQuantity).subscribe({
      next: (cart) => {
        this.cartItems = cart.items;
        this.totalAmount = cart.totalAmount;
      },
      error: (err) => {
        this.errorMessage = 'Failed to update cart item';
      }
    });
  }
  
  removeItem(itemId: string) {
    this.cartService.removeFromCart(itemId).subscribe({
      next: (cart) => {
        this.cartItems = cart.items;
        this.totalAmount = cart.totalAmount;
      },
      error: (err) => {
        this.errorMessage = 'Failed to remove item';
      }
    });
  }
  
  checkout() {
    // Navigate to checkout page
  }
}
```

**Create: `buy-01-frontend/src/app/components/shopping-cart/shopping-cart.component.html`**

```html
<div class="shopping-cart">
  <h2>Shopping Cart</h2>
  
  <div *ngIf="errorMessage" class="alert alert-danger">
    {{ errorMessage }}
  </div>
  
  <div *ngIf="isLoading" class="spinner">
    Loading...
  </div>
  
  <div *ngIf="cartItems.length === 0 && !isLoading" class="empty-cart">
    <p>Your cart is empty</p>
  </div>
  
  <div *ngIf="cartItems.length > 0" class="cart-items">
    <table class="table table-responsive">
      <thead>
        <tr>
          <th>Product</th>
          <th>Price</th>
          <th>Quantity</th>
          <th>Total</th>
          <th>Action</th>
        </tr>
      </thead>
      <tbody>
        <tr *ngFor="let item of cartItems">
          <td>{{ item.productName }}</td>
          <td>${{ item.price.toFixed(2) }}</td>
          <td>
            <input 
              type="number" 
              [value]="item.quantity"
              min="1"
              (change)="updateQuantity(item.id, $any($event.target).value)"
            />
          </td>
          <td>${{ (item.price * item.quantity).toFixed(2) }}</td>
          <td>
            <button (click)="removeItem(item.id)" class="btn btn-sm btn-danger">
              Remove
            </button>
          </td>
        </tr>
      </tbody>
    </table>
    
    <div class="cart-summary">
      <h4>Total: ${{ totalAmount.toFixed(2) }}</h4>
      <button (click)="checkout()" class="btn btn-primary btn-lg">
        Proceed to Checkout
      </button>
    </div>
  </div>
</div>
```

### Step 3.2: Create Checkout Component

**Create: `buy-01-frontend/src/app/components/checkout/checkout.component.ts`**

```typescript
@Component({
  selector: 'app-checkout',
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss']
})
export class CheckoutComponent implements OnInit {
  
  checkoutForm: FormGroup;
  cartTotal: number = 0;
  isProcessing: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';
  
  paymentMethods = [
    { id: 'pay_on_delivery', name: 'Pay on Delivery' },
    { id: 'credit_card', name: 'Credit Card' }
  ];
  
  constructor(
    private formBuilder: FormBuilder,
    private orderService: OrderService,
    private cartService: CartService,
    private router: Router
  ) {
    this.checkoutForm = this.createForm();
  }
  
  ngOnInit() {
    this.loadCartTotal();
  }
  
  createForm(): FormGroup {
    return this.formBuilder.group({
      shippingAddress: ['', [Validators.required]],
      paymentMethod: ['pay_on_delivery', [Validators.required]],
      phone: ['', [Validators.required, Validators.pattern(/^\d{10}$/)]]
    });
  }
  
  loadCartTotal() {
    this.cartService.getCart().subscribe({
      next: (cart) => {
        this.cartTotal = cart.totalAmount;
      }
    });
  }
  
  submitCheckout() {
    if (this.checkoutForm.invalid) {
      this.errorMessage = 'Please fill all required fields';
      return;
    }
    
    this.isProcessing = true;
    const checkoutRequest = {
      ...this.checkoutForm.value,
      cartItems: [] // Will be fetched by backend
    };
    
    this.orderService.createOrder(checkoutRequest).subscribe({
      next: (order) => {
        this.successMessage = `Order created successfully! Order #${order.orderNumber}`;
        this.isProcessing = false;
        setTimeout(() => {
          this.router.navigate(['/orders', order.id]);
        }, 2000);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to create order';
        this.isProcessing = false;
      }
    });
  }
}
```

### Step 3.3: Create Order History Component

**Create: `buy-01-frontend/src/app/components/order-history/order-history.component.ts`**

```typescript
@Component({
  selector: 'app-order-history',
  templateUrl: './order-history.component.html',
  styleUrls: ['./order-history.component.scss']
})
export class OrderHistoryComponent implements OnInit {
  
  orders: Order[] = [];
  currentPage: number = 0;
  pageSize: number = 10;
  totalElements: number = 0;
  selectedStatus: string = '';
  searchTerm: string = '';
  isLoading: boolean = false;
  
  statusOptions = [
    { value: '', label: 'All Statuses' },
    { value: 'pending', label: 'Pending' },
    { value: 'processing', label: 'Processing' },
    { value: 'shipped', label: 'Shipped' },
    { value: 'delivered', label: 'Delivered' },
    { value: 'cancelled', label: 'Cancelled' }
  ];
  
  constructor(private orderService: OrderService) {}
  
  ngOnInit() {
    this.loadOrders();
  }
  
  loadOrders() {
    this.isLoading = true;
    this.orderService.getUserOrders(
      this.currentPage,
      this.pageSize,
      this.selectedStatus || undefined,
      this.searchTerm || undefined
    ).subscribe({
      next: (response) => {
        this.orders = response.content;
        this.totalElements = response.totalElements;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load orders', err);
        this.isLoading = false;
      }
    });
  }
  
  onStatusFilter(status: string) {
    this.selectedStatus = status;
    this.currentPage = 0;
    this.loadOrders();
  }
  
  onSearch(term: string) {
    this.searchTerm = term;
    this.currentPage = 0;
    this.loadOrders();
  }
  
  onPageChange(page: number) {
    this.currentPage = page;
    this.loadOrders();
  }
  
  cancelOrder(orderId: string) {
    if (confirm('Are you sure you want to cancel this order?')) {
      this.orderService.cancelOrder(orderId).subscribe({
        next: () => {
          this.loadOrders();
        },
        error: (err) => {
          alert('Failed to cancel order');
        }
      });
    }
  }
}
```

### Step 3.4: Create Search & Filter Component

**Create: `buy-01-frontend/src/app/components/product-search/product-search.component.ts`**

```typescript
@Component({
  selector: 'app-product-search',
  templateUrl: './product-search.component.html',
  styleUrls: ['./product-search.component.scss']
})
export class ProductSearchComponent implements OnInit {
  
  products: Product[] = [];
  searchTerm: string = '';
  selectedCategory: string = '';
  minPrice: number = 0;
  maxPrice: number = 10000;
  minRating: number = 0;
  sortBy: string = 'name';
  sortOrder: string = 'ASC';
  currentPage: number = 0;
  isLoading: boolean = false;
  
  categories: string[] = [];
  
  constructor(
    private productService: ProductService,
    private activatedRoute: ActivatedRoute
  ) {}
  
  ngOnInit() {
    this.loadCategories();
    this.activatedRoute.queryParams.subscribe(params => {
      this.searchTerm = params['keyword'] || '';
      this.search();
    });
  }
  
  loadCategories() {
    this.productService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
      }
    });
  }
  
  search() {
    this.isLoading = true;
    this.productService.filterProducts(
      this.selectedCategory || undefined,
      this.minPrice > 0 ? this.minPrice : undefined,
      this.maxPrice < 10000 ? this.maxPrice : undefined,
      this.minRating > 0 ? this.minRating : undefined,
      this.currentPage,
      12,
      this.sortBy,
      this.sortOrder
    ).subscribe({
      next: (response) => {
        this.products = response.content;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Search failed', err);
        this.isLoading = false;
      }
    });
  }
  
  onKeywordSearch(keyword: string) {
    this.searchTerm = keyword;
    this.currentPage = 0;
    if (this.searchTerm) {
      this.productService.searchProducts(this.searchTerm, 0, 12).subscribe({
        next: (response) => {
          this.products = response.content;
        }
      });
    }
  }
}
```

### Step 3.5: Create User Profile Component

**Create: `buy-01-frontend/src/app/components/user-profile/user-profile.component.ts`**

```typescript
@Component({
  selector: 'app-user-profile',
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.scss']
})
export class UserProfileComponent implements OnInit {
  
  userProfile: UserProfile;
  userStats: UserStats;
  favoriteProducts: Product[] = [];
  isLoading: boolean = false;
  isEditing: boolean = false;
  
  profileForm: FormGroup;
  
  constructor(
    private userService: UserService,
    private formBuilder: FormBuilder
  ) {
    this.profileForm = this.createForm();
  }
  
  ngOnInit() {
    this.loadUserProfile();
    this.loadUserStats();
  }
  
  createForm(): FormGroup {
    return this.formBuilder.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', Validators.required],
      address: ['', Validators.required]
    });
  }
  
  loadUserProfile() {
    this.isLoading = true;
    this.userService.getUserProfile().subscribe({
      next: (profile) => {
        this.userProfile = profile;
        this.profileForm.patchValue(profile);
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load profile', err);
        this.isLoading = false;
      }
    });
  }
  
  loadUserStats() {
    this.userService.getUserStats().subscribe({
      next: (stats) => {
        this.userStats = stats;
        this.favoriteProducts = stats.favoriteProducts;
      }
    });
  }
  
  updateProfile() {
    if (this.profileForm.invalid) return;
    
    this.userService.updateUserProfile(this.profileForm.value).subscribe({
      next: (profile) => {
        this.userProfile = profile;
        this.isEditing = false;
        alert('Profile updated successfully');
      },
      error: (err) => {
        alert('Failed to update profile');
      }
    });
  }
}
```

### Step 3.6: Add Services

**Create: `buy-01-frontend/src/app/services/cart.service.ts`**

```typescript
@Injectable({
  providedIn: 'root'
})
export class CartService {
  
  constructor(private http: HttpClient) {}
  
  getCart(): Observable<Cart> {
    return this.http.get<Cart>('/api/v1/cart');
  }
  
  addToCart(productId: string, quantity: number): Observable<Cart> {
    return this.http.post<Cart>('/api/v1/cart/items', {
      productId,
      quantity
    });
  }
  
  updateCartItem(itemId: string, quantity: number): Observable<Cart> {
    return this.http.put<Cart>(`/api/v1/cart/items/${itemId}`, {
      quantity
    });
  }
  
  removeFromCart(itemId: string): Observable<Cart> {
    return this.http.delete<Cart>(`/api/v1/cart/items/${itemId}`);
  }
  
  clearCart(): Observable<void> {
    return this.http.delete<void>('/api/v1/cart');
  }
}
```

### Step 3.7: Update App Routes

**Edit: `buy-01-frontend/src/app/app.routes.ts`**

Add new routes:
```typescript
{
  path: 'cart',
  component: ShoppingCartComponent,
  canActivate: [AuthGuard]
},
{
  path: 'checkout',
  component: CheckoutComponent,
  canActivate: [AuthGuard]
},
{
  path: 'orders',
  component: OrderHistoryComponent,
  canActivate: [AuthGuard]
},
{
  path: 'orders/:id',
  component: OrderDetailComponent,
  canActivate: [AuthGuard]
},
{
  path: 'profile',
  component: UserProfileComponent,
  canActivate: [AuthGuard]
},
{
  path: 'search',
  component: ProductSearchComponent
}
```

**⚠️ All Frontend Features Complete** - All database, backend, and frontend implementation is now done on the `buy-02` branch. Proceed to Phase 4 for local testing and validation.

---

## Phase 4: Local Testing & Validation

### Step 4.1: End-to-End Testing

**Create: `buy-01-frontend/cypress/e2e/shopping-cart.cy.ts`**

```typescript
describe('Shopping Cart E2E Tests', () => {
  
  beforeEach(() => {
    cy.visit('http://localhost:4200');
    cy.login('testuser@example.com', 'password123');
  });
  
  it('should add product to cart', () => {
    cy.visit('/products');
    cy.contains('button', 'Add to Cart').first().click();
    cy.visit('/cart');
    cy.contains('Shopping Cart').should('be.visible');
    cy.get('table tbody tr').should('have.length', 1);
  });
  
  it('should update product quantity in cart', () => {
    cy.addToCart('product-id', 1);
    cy.visit('/cart');
    cy.get('input[type="number"]').type('{selectAll}3');
    cy.get('button').contains('Update').click();
    cy.contains('3').should('be.visible');
  });
  
  it('should remove product from cart', () => {
    cy.addToCart('product-id', 1);
    cy.visit('/cart');
    cy.contains('button', 'Remove').click();
    cy.contains('Your cart is empty').should('be.visible');
  });
  
  it('should persist cart after page refresh', () => {
    cy.addToCart('product-id', 2);
    cy.reload();
    cy.visit('/cart');
    cy.contains('2').should('be.visible');
  });
  
  it('should complete checkout successfully', () => {
    cy.addToCart('product-id', 1);
    cy.visit('/cart');
    cy.contains('button', 'Proceed to Checkout').click();
    cy.get('input[name="shippingAddress"]').type('123 Main St');
    cy.get('select[name="paymentMethod"]').select('pay_on_delivery');
    cy.contains('button', 'Place Order').click();
    cy.contains('Order created successfully').should('be.visible');
  });
});
```

### Step 4.2: Integration Tests

**Create: `order-service/src/test/java/com/example/orders/integration/OrderIntegrationTest.java`**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class OrderIntegrationTest {
  
  @Container
  static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
  
  @LocalServerPort
  private int port;
  
  @Autowired
  private OrderRepository orderRepository;
  
  @Autowired
  private ShoppingCartRepository cartRepository;
  
  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
  }
  
  @Test
  public void testCreateOrderEndToEnd() {
    // 1. Add items to cart via API
    // 2. Call checkout endpoint
    // 3. Verify order is created in MongoDB
    // 4. Verify cart is cleared
    // 5. Verify order document has all fields
  }
  
  @Test
  public void testOrderStatusUpdate() {
    // 1. Create order in MongoDB
    // 2. Update status to SHIPPED
    // 3. Verify status change in MongoDB
    // 4. Update to DELIVERED
    // 5. Verify delivery date is set
  }
}
```

**Add TestContainers Dependency to `pom.xml`:**
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.1</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mongodb</artifactId>
    <version>1.19.1</version>
    <scope>test</scope>
</dependency>
```

### Step 4.3: Load Testing

Use Apache JMeter or Gatling to test:
- [ ] Add 100+ items to cart simultaneously
- [ ] Search with 50+ concurrent users
- [ ] Order creation under load
- [ ] Document performance metrics

**✅ Local Testing Complete** - All integration and E2E tests are passing. The implementation is ready for the next phase.

---

## Phase 5: Bonus Features (Optional)

### Step 5.1: Wishlist Feature

**MongoDB Collection:**
```json
// Collection: wishlist
{
  "_id": ObjectId,
  "user_id": ObjectId,
  "product_id": ObjectId,
  "added_at": ISODate
}

// Create unique index:
db.wishlist.createIndex({ "user_id": 1, "product_id": 1 }, { unique: true })
```

**API Endpoints:**
- `GET /api/v1/users/wishlist` - Get user's wishlist
- `POST /api/v1/users/wishlist/{productId}` - Add to wishlist
- `DELETE /api/v1/users/wishlist/{productId}` - Remove from wishlist

**Frontend Component:**
- Add wishlist icon to product cards
- Create wishlist page
- Allow bulk "Add to Cart" from wishlist

### Step 5.2: Multiple Payment Methods

**Support:**
- Credit Card (integrate Stripe or Payment Gateway)
- Debit Card
- Digital Wallets (PayPal, Apple Pay)
- Bank Transfer

**Implementation:**
- Create payment service integration
- Add payment method selection in checkout
- Handle payment success/failure callbacks
- Store transaction records

### Step 5.3: Order Tracking

**Real-time Updates:**
- WebSocket notifications for order status changes
- Email notifications for status updates
- SMS notifications for delivery
- Shipment tracking integration

---

## Phase 6: Create PR & Validate with Second Computer (SonarQube + Jenkins)

### Step 6.1: Prepare Code for Pull Request

Before creating the PR, ensure everything is ready:

**Local Validation Checklist:**
- [ ] All tests pass locally: `npm run test && mvn test`
- [ ] Frontend builds successfully: `ng build`
- [ ] All microservices compile: `mvn clean compile` in each service directory
- [ ] No console errors or warnings
- [ ] Code follows project conventions
- [ ] Responsive design verified on multiple screen sizes
- [ ] No hardcoded credentials or secrets in code
- [ ] Environment variables are properly configured

### Step 6.2: Commit and Create Pull Request

```bash
# Make sure you're on buy-02 branch
git status

# Stage all changes
git add .

# Commit with descriptive message
git commit -m "feat: Complete buy-02 e-commerce platform with shopping cart, orders, search, and user profiles

- Added MongoDB collections for shopping_cart, orders, users, and seller_products
- Implemented backend APIs: OrderController, CartController, UserProfileController, SearchController
- Implemented frontend components: ShoppingCartComponent, CheckoutComponent, OrderHistoryComponent, ProductSearchComponent, UserProfileComponent
- Added comprehensive unit tests with >80% coverage
- Added integration tests using TestContainers
- Added E2E tests using Cypress
- Responsive design for mobile, tablet, and desktop
- All services properly integrated with Eureka service discovery
- Error handling and validation implemented
"

# Push to remote
git push origin buy-02

# Create PR on GitHub
# Go to https://github.com/Violet-Ogombo/buy-01-dev/pull/new/buy-02
```

**Pull Request Description Template:**

```markdown
## 🎯 Overview
Complete implementation of buy-02 e-commerce platform features.

## 📋 Changes Made

### Database (Phase 1)
- ✅ Shopping cart collection with embedded items
- ✅ Orders collection with order items and status tracking
- ✅ User profile extensions (seller stats, preferences)
- ✅ Seller products collection
- ✅ MongoDB indexes for performance

### Backend APIs (Phase 2)
- ✅ Cart API (add, remove, update, checkout)
- ✅ Order API (create, list, cancel, status updates)
- ✅ User Profile API (get, update, order history)
- ✅ Search API (keyword search, filtering)
- ✅ Unit tests for all services (>80% coverage)

### Frontend (Phase 3)
- ✅ Shopping Cart component with persist functionality
- ✅ Checkout component with form validation
- ✅ Order History component with filtering/pagination
- ✅ Product Search component with multi-filter support
- ✅ User Profile component with editable fields
- ✅ Responsive design (mobile/tablet/desktop)

### Testing (Phase 4)
- ✅ E2E tests with Cypress
- ✅ Integration tests with TestContainers
- ✅ Unit tests for all critical paths
- ✅ Performance testing results

## 🧪 Testing Instructions

1. Run all tests locally:
   \`\`\`bash
   npm run test
   mvn test
   npm run e2e
   \`\`\`

2. Verify with second computer:
   - Run SonarQube analysis
   - Check Jenkins pipeline
   - Validate code quality gates
   - Review security scan results

## 📊 Quality Metrics
- Code Coverage: [XX]%
- Test Count: [XX] tests
- SonarQube Score: [Pending - to be checked on second computer]
- Critical Issues: [XX]
- Security Vulnerabilities: [XX]

## ⚠️ Breaking Changes
None

## 🚀 Ready for Review
This PR is ready for review on the second computer's local Jenkins and SonarQube instance.
```

### Step 6.3: Set Up Second Computer for Pipeline Review

On the second computer with Jenkins and SonarQube:

**A. Pull the PR branch:**
```bash
cd /path/to/buy-01-dev
git fetch origin buy-02
git checkout buy-02
```

**B. Run SonarQube Analysis:**
```bash
# Navigate to project root
cd /Users/aung.min/Desktop/github/buy-01-dev

# Run SonarQube scanner for Java projects
mvn clean org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.projectKey=buy-02-ecommerce \
  -Dsonar.projectName="Buy-02 E-Commerce" \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=admin \
  -Dsonar.password=admin

# For frontend (Angular)
# Ensure sonar-scanner CLI is installed, then run from buy-01-frontend:
cd buy-01-frontend
sonar-scanner \
  -Dsonar.projectKey=buy-02-frontend \
  -Dsonar.sources=src \
  -Dsonar.exclusions=**/*.spec.ts,**/node_modules/** \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=admin \
  -Dsonar.password=admin
```

**C. Trigger Jenkins Pipeline:**
```bash
# Open Jenkins at http://localhost:8085
# Navigate to job for buy-02
# Click "Build Now" or set up webhook for automatic triggering
# Monitor build logs for:
#   - Checkout success
#   - Build completion
#   - Test results
#   - SonarQube integration
#   - Quality gate status
```

### Step 6.4: Review SonarQube Results

In SonarQube dashboard (`http://localhost:9000`):

**Check the following:**
- [ ] Overall Quality Gate: PASS/FAIL
- [ ] Code Smells: Count and severity
- [ ] Security Hotspots: Any critical issues?
- [ ] Bugs: Zero high-priority bugs?
- [ ] Vulnerabilities: Any CVEs detected?
- [ ] Code Duplications: < 5%?
- [ ] Test Coverage: > 80%?
- [ ] Maintainability Index: > 80?

**Common Issues to Address:**
1. **Code Smells**: Simplify complex methods (split into smaller functions)
2. **Duplicated Code**: Extract common patterns into utilities
3. **Security Hotspots**: Review authentication, input validation
4. **Low Coverage**: Add unit tests for untested paths
5. **Performance**: Optimize database queries, reduce N+1 queries

### Step 6.5: Review Jenkins Pipeline Execution

In Jenkins (`http://localhost:8085`):

**Pipeline Stages to Monitor:**
1. **Checkout** - Did Git clone succeed?
2. **Build** - Did Maven/npm builds complete without errors?
3. **Test** - How many tests ran? Any failures?
4. **SonarQube Analysis** - Analysis submitted successfully?
5. **Quality Gate** - Did it pass?
6. **Code Coverage** - Is it above 80%?

**Common Pipeline Issues:**
- Credential errors (SSH key, tokens)
- JDK/Maven not found in path
- Test failures due to missing dependencies
- Docker services not running
- Port conflicts

---

---

## Phase 7: Fix Issues & Prepare for Merge

### Step 7.1: Analyze SonarQube Issues

From the SonarQube dashboard, categorize all issues:

**CRITICAL (Fix immediately):**
```
- Security vulnerabilities
- Authentication/authorization flaws
- Data exposure risks
- SQL/NoSQL injection vulnerabilities
```

**HIGH (Fix before merge):**
```
- Bugs that affect functionality
- Performance bottlenecks (N+1 queries)
- Null pointer exceptions
- Resource leaks
```

**MEDIUM (Address in future):**
```
- Code smells (complexity, duplication)
- Maintainability issues
- Test coverage gaps
```

**LOW (Optional improvements):**
```
- Code style inconsistencies
- Documentation gaps
- Minor optimizations
```

### Step 7.2: Fix Issues Locally

For each CRITICAL and HIGH issue:

```bash
# 1. Checkout buy-02 branch
git checkout buy-02

# 2. Make fixes locally
# - Edit the file mentioned in SonarQube issue
# - Address the root cause (don't just suppress the warning)
# - Test the fix: npm run test / mvn test

# 3. Verify the fix
# - Re-run affected tests
# - Check for regressions
# - Validate edge cases

# 4. If fixing HIGH priority items, commit the fix
git add .
git commit -m "fix: Address SonarQube issue - [issue-id] [description]"
git push origin buy-02

# 5. Re-run pipeline on second computer to verify fix
```

### Step 7.3: Common Fixes by Category

**Security Issues:**
```java
// ❌ WRONG: Hardcoded credentials
String dbPassword = "pass123";

// ✅ CORRECT: Use environment variables
String dbPassword = System.getenv("DB_PASSWORD");
if (dbPassword == null) {
    throw new IllegalArgumentException("DB_PASSWORD not set");
}
```

**Performance Issues:**
```java
// ❌ WRONG: N+1 query problem
List<Order> orders = orderRepository.findAll(); // 1 query
for (Order order : orders) {
    User user = userRepository.findById(order.getUserId()); // N queries
}

// ✅ CORRECT: Eager loading
@Query("SELECT o FROM Order o LEFT JOIN FETCH o.user")
List<Order> findAllWithUser();
```

**Code Smells:**
```java
// ❌ WRONG: Complex method
public ResponseEntity<OrderDTO> createOrder(OrderRequest req) {
    // 50+ lines of logic
}

// ✅ CORRECT: Extract to service
public ResponseEntity<OrderDTO> createOrder(OrderRequest req) {
    OrderDTO result = orderService.processOrder(req);
    return ResponseEntity.ok(result);
}
```

### Step 7.4: Update PR After Fixes

```bash
# After fixing issues locally and verifying tests pass:
# The fixes are already committed to the buy-02 branch

# On second computer, re-run the pipeline:
# Jenkins will automatically pick up the latest commits
# Monitor the new build for quality gate status
```

### Step 7.5: Approve and Merge

When SonarQube and Jenkins both pass:

```bash
# On GitHub, approve the PR
# Check for:
# ✅ All CI checks passing
# ✅ SonarQube quality gate: PASS
# ✅ Code coverage >= 80%
# ✅ No unresolved conversations
# ✅ At least 2 approvals (if required)

# Merge the PR
# Option 1: Squash and merge (recommended for cleaner history)
# Option 2: Create a merge commit (preserves individual commits)
# Option 3: Rebase and merge (linear history)

# After merge, delete the branch
git branch -d buy-02
```

---

## Phase 8: Jenkins & SonarQube Optimization (Optional)

### Step 8.1: Configure Jenkinsfile for Automated SonarQube Analysis

**Update: `Jenkinsfile`**

```groovy
pipeline {
    agent any
    
    environment {
        SONAR_HOST_URL = credentials('sonar-host-url')
        SONAR_LOGIN = credentials('sonar-token')
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/buy-02']],
                    userRemoteConfigs: [[
                        url: 'git@github.com:Violet-Ogombo/buy-01-dev.git',
                        credentialsId: 'github-ssh-key'
                    ]]
                ])
            }
        }
        
        stage('Build Backend') {
            steps {
                sh 'mvn clean compile'
            }
        }
        
        stage('Build Frontend') {
            steps {
                dir('buy-01-frontend') {
                    sh 'npm install && npm run build'
                }
            }
        }
        
        stage('Test') {
            steps {
                sh 'mvn test'
                dir('buy-01-frontend') {
                    sh 'npm test'
                }
            }
        }
        
        stage('SonarQube Analysis - Backend') {
            steps {
                sh '''
                    mvn clean org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                        -Dsonar.projectKey=buy-02-backend \
                        -Dsonar.projectName="Buy-02 Backend" \
                        -Dsonar.host.url=${SONAR_HOST_URL} \
                        -Dsonar.login=${SONAR_LOGIN} \
                        -Dsonar.sources=.
                '''
            }
        }
        
        stage('SonarQube Analysis - Frontend') {
            steps {
                dir('buy-01-frontend') {
                    sh '''
                        sonar-scanner \
                            -Dsonar.projectKey=buy-02-frontend \
                            -Dsonar.projectName="Buy-02 Frontend" \
                            -Dsonar.sources=src \
                            -Dsonar.host.url=${SONAR_HOST_URL} \
                            -Dsonar.login=${SONAR_LOGIN}
                    '''
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                script {
                    timeout(time: 5, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }
        }
    }
    
    post {
        always {
            junit 'target/surefire-reports/**/*.xml'
            publishHTML([
                reportDir: 'target/site/jacoco',
                reportFiles: 'index.html',
                reportName: 'Code Coverage Report'
            ])
        }
        success {
            echo '✅ Pipeline succeeded! Ready for merge.'
        }
        failure {
            echo '❌ Pipeline failed. Review logs and fix issues.'
        }
    }
}
```

### Step 8.2: Configure SonarQube Quality Gates

In SonarQube UI (`http://localhost:9000`):

1. **Go to:** Administration → Quality Gates
2. **Create New Quality Gate:** "Buy-02 Standards"
3. **Add Conditions:**

```
✅ Coverage >= 80%
✅ Duplications <= 5%
✅ Maintainability Rating: A
✅ Reliability Rating: A
✅ Security Rating: A
✅ Security Hotspots Reviewed: 100%
✅ New Issues: 0 (CRITICAL or HIGH)
```

4. **Set as Default:** Check "Set as default"

### Step 8.3: Configure Code Coverage Tracking

**Add to `pom.xml` (all Java microservices):**

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Add to `package.json` (Angular frontend):**

```json
{
  "scripts": {
    "test:coverage": "ng test --code-coverage --watch=false",
    "sonar": "sonar-scanner"
  },
  "devDependencies": {
    "sonar-scanner": "^3.1.0"
  }
}
```

### Step 8.4: Set Up Slack Notifications

In Jenkins:

1. Go to Manage Jenkins → Manage Plugins
2. Search for "Slack Notification"
3. Install and restart
4. Go to Manage Jenkins → Configure System
5. Find "Slack" section:
   - Workspace: your-workspace
   - Credential: Add GitHub token
   - Channel: #deployments
6. Update Jenkinsfile post section:

```groovy
post {
    always {
        slackSend(
            channel: '#deployments',
            message: '''
                Buy-02 Pipeline: ${BUILD_STATUS}
                Build: ${BUILD_NUMBER}
                Commit: ${GIT_COMMIT}
                Branch: ${GIT_BRANCH}
            ''',
            color: currentBuild.result == 'SUCCESS' ? 'good' : 'danger'
        )
    }
}
```

### Step 8.5: Configure Webhook for Automatic Pipeline Triggers

**On GitHub (Repository Settings → Webhooks):**

1. Add Webhook:
   - Payload URL: `http://jenkins-server:8085/github-webhook/`
   - Content type: application/json
   - Events: Push, Pull request
   - Active: ✅

2. Jenkins will automatically trigger pipeline on push/PR

### Step 8.6: Monitor Pipeline Performance

Create Jenkins dashboard to track:

```
📊 Build Time Trends
- Backend build time
- Frontend build time
- Test execution time
- SonarQube analysis time

📈 Quality Metrics
- Test success rate
- Code coverage trend
- SonarQube score trend
- Issue count over time
```

---

## Phase 9: Production Deployment & Standards (Optional/Future)

### Step 9.1: Establish Code Review Standards

## Code Review Checklist

Before submitting a PR, ensure:
- [ ] Code follows project style guide
- [ ] All tests pass locally
- [ ] No SonarQube critical issues
- [ ] Code coverage >= 80% for new code
- [ ] PR description clearly explains changes
- [ ] Commits are squashed and meaningful

## Code Review Criteria

Reviewers should check:
1. **Functionality:** Does the code do what it's supposed to?
2. **Design:** Is the architecture sound?
3. **Testing:** Is there adequate test coverage?
4. **Performance:** Could this cause performance issues?
5. **Security:** Are there any security concerns?
6. **Documentation:** Is the code well documented?

## Approval Process

- Minimum 2 approvals required for merge
- All CI checks must pass
- SonarQube quality gate must pass
- No unresolved conversations
```

### Step 9.2: PR Template

**Create: `.github/pull_request_template.md`**

```markdown
## Description
[Brief description of changes]

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] E2E tests added/updated
- [ ] Manual testing completed

## Screenshots (if applicable)
[Add screenshots for UI changes]

## SonarQube Report
[Link to SonarQube analysis]

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex logic
- [ ] Documentation updated
- [ ] No new warnings generated
```

### Step 9.3: Review Schedule

**Weekly Review Meetings:**
- **Monday 10 AM:** Discuss new PRs, assign reviewers
- **Wednesday 3 PM:** Follow-up on review comments
- **Friday 4 PM:** Merge approved PRs, plan next week

### Step 9.4: Team Communication

**Slack Integration:**
- Link GitHub to Slack
- Get notifications for new PRs
- Comment notifications
- Merge confirmations

### Step 9.5: Metrics & Reporting

Track the following metrics:
- [ ] Average PR review time
- [ ] Number of iterations per PR
- [ ] Test coverage trend
- [ ] Code quality score trend
- [ ] Build success rate
- [ ] Deployment frequency

**Create: `metrics/weekly-report.md`**

Report on:
1. PRs merged this week
2. Code quality improvements
3. Test coverage changes
4. SonarQube issues fixed
5. Performance improvements

---

## Testing Checklist

### Functional Testing
- [ ] Add product to cart
- [ ] Update cart quantities
- [ ] Remove items from cart
- [ ] Persist cart after refresh
- [ ] Complete checkout
- [ ] View order history
- [ ] Filter orders by status
- [ ] Search products
- [ ] Filter products by category/price
- [ ] View user profile
- [ ] Update user profile
- [ ] Cancel order (pending status only)

### Non-Functional Testing
- [ ] Performance: Cart operations < 200ms
- [ ] Search results load < 500ms
- [ ] Mobile responsiveness (480px - 1920px)
- [ ] Browser compatibility (Chrome, Firefox, Safari)
- [ ] Error message clarity
- [ ] Input validation
- [ ] Security: No sensitive data in URLs
- [ ] SSL/HTTPS enforcement

### Security Testing
- [ ] NoSQL injection prevention (MongoDB injection attacks)
- [ ] XSS prevention
- [ ] CSRF token validation
- [ ] Authentication/authorization checks
- [ ] Data encryption for sensitive fields
- [ ] Rate limiting on APIs
- [ ] Input sanitization
- [ ] Secure password hashing (bcrypt/PBKDF2)

---

## Deployment Checklist

Before deploying to production:
- [ ] All tests pass
- [ ] SonarQube quality gate passes
- [ ] Code review approved
- [ ] Database migrations tested
- [ ] Performance testing completed
- [ ] Security audit passed
- [ ] Load testing completed
- [ ] Documentation updated
- [ ] Rollback plan documented

---

## Success Criteria

The project will be considered complete when:

✅ All required features implemented and tested
✅ Shopping cart persists across sessions
✅ Orders can be created and tracked
✅ User profiles display statistics
✅ Search and filtering work correctly
✅ SonarQube code quality score > 80%
✅ Test coverage > 80%
✅ All PRs reviewed and approved
✅ CI/CD pipeline passing
✅ Zero critical security issues
✅ Responsive design on all devices
✅ Documentation complete

---

## Timeline

| Phase | Duration | Status | Work Location |
|-------|----------|--------|----------------|
| Phase 1: Database Design | 1 week | Pending | 🔴 Local (buy-02 branch) |
| Phase 2: Backend API Development | 2 weeks | Pending | 🔴 Local (buy-02 branch) |
| Phase 3: Frontend Development | 2 weeks | Pending | 🔴 Local (buy-02 branch) |
| Phase 4: Local Testing & Validation | 1 week | Pending | 🔴 Local (buy-02 branch) |
| Phase 5: Bonus Features (Optional) | 1-2 weeks | Optional | 🟡 Local (buy-02 branch) |
| Phase 6: Create PR & Validate | 3-5 days | Pending | 💻 2nd Computer (SonarQube + Jenkins) |
| Phase 7: Fix Issues & Merge | 3-7 days | Pending | 🔄 Local fixes + 2nd Computer validation |
| Phase 8: Jenkins & SonarQube Optimization | 1 week | Optional | 💻 2nd Computer (pipeline setup) |
| Phase 9: Production Deployment & Standards | Ongoing | Optional | 📦 Production environment |

**Execution Flow:**
1. **Phases 1-4:** Complete all implementation on `buy-02` branch locally (no commits yet)
2. **Phase 5 (Optional):** Add bonus features if time permits
3. **Phase 6:** Commit, push, and create PR; validate on 2nd computer with SonarQube + Jenkins
4. **Phase 7:** Fix any issues found, re-run validation, then merge when all checks pass
5. **Phase 8 (Optional):** Optimize Jenkins pipeline and SonarQube setup on 2nd computer
6. **Phase 9 (Optional):** Deploy to production with deployment standards

**Total Estimated Time:**
- Core implementation (Phases 1-4): 4-6 weeks
- Bonus features (Phase 5): +1-2 weeks (optional)
- PR validation & fixes (Phases 6-7): +1 week
- Pipeline optimization (Phase 8): +1 week (optional)
- Production standards (Phase 9): Ongoing (optional)

---

## Quick Reference: What to Do When

### 💻 On Your Local Machine
- Work on Phases 1-4 (database, backend, frontend)
- Run tests locally before committing
- When ready, commit and push to `buy-02` branch
- Create PR with detailed description

### 🖥️ On Second Computer (With SonarQube & Jenkins)
- Pull the `buy-02` branch
- Trigger Jenkins pipeline (build, test, SonarQube analysis)
- Review SonarQube results and quality gates
- Identify issues that need fixing
- Wait for fixes from local machine, then re-run pipeline

### 🔄 Back on Local Machine
- Review SonarQube/Jenkins results
- Fix CRITICAL and HIGH priority issues
- Push fixes to `buy-02` branch
- 2nd computer's Jenkins re-runs automatically (or manually trigger)
- Repeat until all checks pass

### ✅ When Everything Passes
- Approve PR on GitHub
- Merge to main/develop branch
- Celebrate 🎉

---

## Questions & Support

For questions or issues:
1. Check existing PRs and issues
2. Post in team Slack channel
3. Schedule pair programming session
4. Create GitHub issue with detailed description

---

**Last Updated:** May 26, 2026
**Version:** 2.0 (Reorganized for single-branch workflow with second computer validation)
