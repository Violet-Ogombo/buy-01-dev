# Backend Microservices Deep Dive Index

This document serves as the entry point and index for the low-level, class-by-class and method-by-method documentation of the Spring Boot backend microservices.

---

## Microservices Catalog

### 1. [Discovery Server (discovery-server)](file:///Users/aung.min/Desktop/github/buy-01-dev/docs/03a-discovery-server.md)
*   **Purpose**: Service registry using Netflix Eureka.
*   **Key Topics**: Service registry settings, client registration bypass configurations, and main application bootstrap annotations.

### 2. [API Gateway (api-gateway)](file:///Users/aung.min/Desktop/github/buy-01-dev/docs/03b-api-gateway.md)
*   **Purpose**: Single gateway entry point routing traffic dynamically.
*   **Key Topics**: CORS configuration beans, route predicate mapping rules, JWT extraction filters, global headers injection (`X-User-Id`, `X-User-Role`), multipart request detection, and latency measurement logging.

### 3. [Identity Service (identity-service)](file:///Users/aung.min/Desktop/github/buy-01-dev/docs/03c-identity-service.md)
*   **Purpose**: Authentication, authorization, user credentials, and user profiling.
*   **Key Topics**: Dual-layer password hashing mechanism (BCrypt on top of SHA-256), token generation, user profiles stats fetching, gateway header parser integration (`HeaderAuthenticationFilter`), and Kafka event publishing on user registrations.

### 4. [Product Service (product-service)](file:///Users/aung.min/Desktop/github/buy-01-dev/docs/03d-product-service.md)
*   **Purpose**: Catalog catalog listings, wishlists, search indices, and seller profiles.
*   **Key Topics**: Stock reduction transactions (`reduceStock`), stock reversion updates (`revertStock`), seller sales calculations, image mapping logic with external token verification, keyword filters, and Kafka event listeners for image uploads.

### 5. [Order Service (order-service)](file:///Users/aung.min/Desktop/github/buy-01-dev/docs/03e-order-service.md)
*   **Purpose**: Cart operations, checkout processing, order history, and buyer analytics.
*   **Key Topics**: Checkout validations, transaction management, stock reduction HTTP triggers, order status transition rules, order cancellations, stock reversion triggers, duplicate order logic (`redoOrder`), and buyer purchase statistics calculations.

### 6. [Media Service (media-service)](file:///Users/aung.min/Desktop/github/buy-01-dev/docs/03f-media-service.md)
*   **Purpose**: Image uploading and local disk storage management.
*   **Key Topics**: Size constraints, MIME filtering, file signature validations (magic bytes verification to prevent spoofing), directory traversal security cleanses, absolute disk operations, and Kafka upload notification emissions.
