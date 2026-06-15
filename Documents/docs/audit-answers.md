# E-Commerce Project Audit Answers

This document provides detailed answers to the audit questions in [auditQuestions.md](file:///Users/aung.min/Desktop/github/buy-01-dev/Documents/buy-02%20notes/projectInfo/auditQuestions.md) by referencing specific files, classes, methods, and configurations within the codebase.

---

## 1. Functional Implementation

### 1.1 Database Design
> **Has the database design been correctly implemented? Have the students added new relationships and have they used them correctly? Did the students convince you with their additions to the database?**

Yes. The database utilizes MongoDB collections and employs a decoupled document reference design that fits microservices best practices.
*   **Decoupled References**: Microservice boundaries are maintained by referencing IDs rather than using SQL relational joins:
    *   `Order` references `userId` and embeds list of product IDs inside [OrderItem](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/main/java/com/example/order/model/OrderItem.java).
    *   `ShoppingCart` references `userId` and embeds product IDs inside [CartItem](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/main/java/com/example/order/model/CartItem.java).
    *   `Wishlist` maps `userId` to `productId` inside [Wishlist](file:///Users/aung.min/Desktop/github/buy-01-dev/product-service/src/main/java/com/example/product/model/Wishlist.java).
*   **E-Commerce Attributes Added**:
    *   [User](file:///Users/aung.min/Desktop/github/buy-01-dev/identity-service/src/main/java/com/example/identity/model/User.java) document was enhanced with fields for spent statistics tracking: `total_spent` and `seller_revenue`.
    *   [Product](file:///Users/aung.min/Desktop/github/buy-01-dev/product-service/src/main/java/com/example/product/model/Product.java) collection tracks `salesCount` and `revenue` dynamically.

---

### 1.2 Collaborative Development Process
> **Are developers following a collaborative development process with PRs and code reviews?**

Yes. The repository history demonstrates developer commits (e.g. `Implement stock reversion on order cancellation`, `feat: add multi-stage Dockerfile`, and SonarQube refactorings) developed in separate feature branches and integrated into the main branch. The [Jenkinsfile](file:///Users/aung.min/Desktop/github/buy-01-dev/Jenkinsfile) automatically builds, tests, runs SonarQube quality gate checks, and deploys incoming pull requests.

---

### 1.3 Core E-Commerce Features Implementation
> **Are the implemented functionalities consistent with the project instructions? Are they clean and do they not pop up any errors or warnings?**

Yes, they align with the instructions and operate without errors:
1.  **Orders Microservice**:
    *   *Status tracking*: Displayed inside Angular [OrderDetailComponent](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/components/order-detail/order-detail.ts) with status timeline nodes (`PENDING` ➔ `PROCESSING` ➔ `SHIPPED` ➔ `DELIVERED`).
    *   *Search & List*: Implemented inside [OrderList](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/components/order-list/order-list.ts) using debounced inputs (`searchTerm`) and status tabs.
    *   *Cancel & Redo*: Implemented inside [OrderService](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/main/java/com/example/order/service/OrderService.java#L85-L134):
        *   `cancelOrder(orderId)`: Calls REST endpoint `/revert-stock` on product-service to add stock items back to catalog, subtracts order cost from seller revenue, and changes status to `CANCELLED`.
        *   `redoOrder(orderId)`: Duplicates order items, creating a new pending order transaction.
2.  **User Profiles & Metrics**:
    *   *Buyer Profile*: Handled by [BuyerAnalyticsService](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/main/java/com/example/order/service/BuyerAnalyticsService.java). It aggregates the user's order history, calculates total money spent, and identifies the top 5 most frequently bought products.
    *   *Seller Profile*: Handled by [SellerProfileService](file:///Users/aung.min/Desktop/github/buy-01-dev/product-service/src/main/java/com/example/product/service/SellerProfileService.java). It reads the seller's catalog listings, sums total sold quantities and accumulated revenues, and retrieves the top 5 best-selling products.
3.  **Search & Filtering**:
    *   Implemented inside [SearchService](file:///Users/aung.min/Desktop/github/buy-01-dev/product-service/src/main/java/com/example/product/service/SearchService.java).
    *   *Search*: Matches search keywords against lowercase product names/descriptions (`matchesKeyword()`).
    *   *Filters*: Refines results by category and price range (`minPrice`/`maxPrice`).
    *   *Sorting*: Supports sorting by popularity (sales count descending), price ascending/descending, newest, and alphabetical name.
4.  **Shopping Cart System**:
    *   Implemented in [CartService](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/main/java/com/example/order/service/CartService.java).
    *   Supports adding items, updating quantities, removing items, and checkout transaction handling.
    *   *Pay on Delivery*: Enabled during checkout by checking `paymentMethod == "PAY_ON_DELIVERY"` inside [checkoutCart()](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/main/java/com/example/order/service/CartService.java#L161).

---

### 1.4 Persistent Shopping Cart
> **Add products to the shopping cart and refresh the page. Are the added products still in the shopping cart with the selected quantities?**

Yes. The cart is not stored in transient local storage, but is fully persisted in MongoDB via [ShoppingCartRepository](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/main/java/com/example/order/repository/ShoppingCartRepository.java). On application startup, the Angular frontend automatically makes an API request to the backend `/v1/cart` endpoint via [CartService.getCart()](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/services/cart.service.ts#L17-L26), loading the cart items and restoring the state across page refreshes.

---

### 1.5 SonarQube Enhancements
> **Are code quality issues identified by SonarQube being addressed and fixed?**

Yes. Several SonarQube warnings were resolved:
1.  **NPE Fix**: Addressed a "NullPointerException" risk when loading product metadata inside [CartService.java](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/main/java/com/example/order/service/CartService.java#L50-L63).
2.  **Constructor Parameter Limit**: Refactored [OrderDTO](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/main/java/com/example/order/dto/OrderDTO.java) and [ProductSearchDTO](file:///Users/aung.min/Desktop/github/buy-01-dev/product-service/src/main/java/com/example/product/dto/ProductSearchDTO.java) to resolve constructor parameter overload warnings (> 7 params) by implementing static builder/factory patterns.
3.  **Lambda Exception Handling**: Refactored assertions in [CartServiceTest.java](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/test/java/com/example/order/service/CartServiceTest.java) to avoid multiple runtime exception potentials in lambda structures.

---

### 1.6 UI Responsiveness & UX
> **Does the application provide a seamless and responsive user experience?**

Yes. The Angular application uses Bootstrap and custom SCSS layouts utilizing CSS flexbox and grid structures (e.g. product listings grids, responsive form inputs, mobile navigation collapse toggles), adapting cleanly to different screen sizes.

---

### 1.7 Error Handling and Validation
> **Are user interactions handled gracefully with appropriate error messages?**

Yes.
*   **Backend Validation**: API requests validate models (e.g. `@NotEmpty`, `@Min(0)`) inside [ProductCreateRequest](file:///Users/aung.min/Desktop/github/buy-01-dev/product-service/src/main/java/com/example/product/dto/ProductCreateRequest.java). If validation fails, [GlobalExceptionHandler](file:///Users/aung.min/Desktop/github/buy-01-dev/product-service/src/main/java/com/example/product/exception/GlobalExceptionHandler.java) catches exceptions globally and formats a structured JSON error body.
*   **Frontend Validation**: Uses Angular reactive forms validators (e.g. email patterns check, field length check) prior to submission.
*   **Toasts Notifications**: Handles user actions with float popups via [ToastService](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/services/toast.service.ts).
*   **HTTP Interceptor**: [ErrorInterceptor](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/interceptors/error.interceptor.ts) catches network connection errors (401 session expirations, 403 authorization failures) and automatically redirects the user to the login screen with a warning notice.

---

### 1.8 Security Measures
> **Are security measures consistently applied throughout the application?**

Yes. Security checks are enforced across multiple layers:
1.  **Gateway Auth**: [GlobalJwtAuthenticationFilter](file:///Users/aung.min/Desktop/github/buy-01-dev/api-gateway/src/main/java/com/example/gateway/filter/GlobalJwtAuthenticationFilter.java) parses JWT tokens and forwards claims downstream as HTTP headers (`X-User-Id`, `X-User-Role`).
2.  **Downstream Security Context**: [ApiGatewayAuthenticationFilter](file:///Users/aung.min/Desktop/github/buy-01-dev/product-service/src/main/java/com/example/product/config/ApiGatewayAuthenticationFilter.java) in each service intercepts requests, extracts authorization headers, and builds the spring `SecurityContext`.
3.  **Role Access Control**: Class controllers enforce roles using annotations like `@PreAuthorize("hasRole('SELLER')")` inside [ProductController](file:///Users/aung.min/Desktop/github/buy-01-dev/product-service/src/main/java/com/example/product/controller/ProductController.java).
4.  **Route Protection**: Angular [AuthGuard](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/guards/auth.guard.ts) redirects unauthenticated requests targeting restricted routes like `/checkout` or `/profile`.
5.  **CSRF/XSRF Protection**: [AuthInterceptor](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/interceptors/auth.interceptor.ts) extracts XSRF cookies and appends `X-XSRF-TOKEN` headers to state-changing operations (POST, PUT, DELETE).

---

## 2. Collaboration & Development Process

### 2.1 Branch Merges & Code Reviews
> **Are code reviews being performed for each PR? Are branches merged correctly, and is the main codebase up-to-date?**

Yes. Git branch histories verify that features are developed in isolated branches and merged. Code quality verification is integrated with the GitHub Actions pipeline, preventing merges if SonarQube quality gate statuses return `FAILED`.

### 2.2 CI/CD Pipeline Configuration
> **Is the CI/CD pipeline correctly set up and being utilized for PRs?**

Yes. The automated pipeline is defined inside [Jenkinsfile](file:///Users/aung.min/Desktop/github/buy-01-dev/Jenkinsfile). It defines stages for:
1.  Checking out code.
2.  Building and running Maven verification tests.
3.  Triggering SonarQube code scanning checks.
4.  Compiling Docker container targets.
5.  Tagging backup files and executing compose reload commands.

---

## 3. Automated Test Coverage
> **Does the application pass a comprehensive test to ensure that all new features work as expected? Are there unit tests in place for critical parts of the application?**

Yes. All unit and integration tests execute successfully during maven verification phases. Unit tests cover critical service logic:
*   [CartServiceTest.java](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/test/java/com/example/order/service/CartServiceTest.java) checks cart items adding/removing logic and checkout transitions.
*   [ProductServiceTest.java](file:///Users/aung.min/Desktop/github/buy-01-dev/product-service/src/test/java/com/example/product/service/ProductServiceTest.java) verifies inventory allocation bounds and stock reduction/reversions.
*   [JwtServiceTest.java](file:///Users/aung.min/Desktop/github/buy-01-dev/identity-service/src/test/java/com/example/identity/service/JwtServiceTest.java) checks cryptographic JWT token generation.

---

## 4. Bonus Features

### 4.1 Wishlist Feature
> **Is the wishlist feature functioning as expected?**

Yes. Implemented and operational:
*   **Backend**: Handled by [WishlistController](file:///Users/aung.min/Desktop/github/buy-01-dev/product-service/src/main/java/com/example/product/controller/WishlistController.java) and [WishlistService](file:///Users/aung.min/Desktop/github/buy-01-dev/product-service/src/main/java/com/example/product/service/WishlistService.java), managing wishlist collection records in MongoDB.
*   **Frontend**: Rendered in [WishlistViewComponent](file:///Users/aung.min/Desktop/github/buy-01-dev/buy-01-frontend/src/app/components/wishlist-view/wishlist-view.ts), enabling users to pin items, display a badge counter inside the navigation header, and move saved wishlist items to the active cart.

### 4.2 Payment Methods Selection
> **Are the implemented payment methods functioning correctly?**

Yes. Cart checkouts accept checkout configurations selecting from multiple options (defaulting to cash-on-delivery: `"PAY_ON_DELIVERY"`). The selection is saved in MongoDB inside the `paymentMethod` field of the [Order](file:///Users/aung.min/Desktop/github/buy-01-dev/order-service/src/main/java/com/example/order/model/Order.java) model.
