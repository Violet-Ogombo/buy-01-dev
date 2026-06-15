# Architecture and File Structure

This document provides a map of how the Buy-01 project is organized on a macro and micro level.

## 1. High-Level Architecture

The project follows a standard Microservices Architecture. Instead of one giant application, it's divided into several specialized, independent services.

*   **`buy-01-frontend`**: The Angular web application that users interact with in their browser.
*   **`api-gateway`**: The entry point for all frontend requests. It routes traffic to the appropriate backend service (e.g., requests to `/api/products` go to the `product-service`).
*   **`discovery-server`**: An internal registry (Eureka). Services register themselves here so the API Gateway knows where to find them.
*   **`identity-service`**: Handles user registration, login, and generates JWT tokens for authentication.
*   **`product-service`**: Manages the product catalog, including adding, updating, and searching for items.
*   **`order-service`**: Handles the shopping cart, checkout process, and order history.
*   **`media-service`**: Responsible for uploading and serving images (e.g., product photos).

### Supporting Infrastructure
*   **MongoDB**: The primary database, used by the Product, Order, Identity, and Media services (each uses different collections/tables).
*   **Kafka**: The messaging system. Services use this to communicate asynchronously (e.g., Order Service tells Product Service to decrease stock after a purchase).
*   **Zookeeper**: Manages Kafka.
*   **Nginx**: The reverse proxy that sits in front of the API Gateway and frontend, handling HTTPS and serving the web files.

---

## 2. Typical Backend Microservice Structure

Let's look inside `product-service` as an example. Most Spring Boot services in this project follow this exact structure:

```text
product-service/
├── pom.xml                  # The Maven build file (lists dependencies like Spring Web, MongoDB)
├── Dockerfile               # Instructions to build the Docker image for this service
└── src/
    ├── main/
    │   ├── java/com/example/product/
    │   │   ├── ProductServiceApplication.java # The main entry point to start the app
    │   │   ├── config/      # Configuration classes (e.g., Security, Kafka setup, Authentication Filters)
    │   │   ├── controller/  # REST APIs (the endpoints the frontend calls, e.g., @GetMapping("/products"))
    │   │   ├── dto/         # Data Transfer Objects (objects used to send/receive data, hiding the real database models)
    │   │   ├── exception/   # Custom error handling and global exception interceptors
    │   │   ├── model/       # The actual database entities (e.g., Product class mapped to MongoDB)
    │   │   ├── repository/  # Interfaces that talk to MongoDB (e.g., ProductRepository)
    │   │   └── service/     # The "business logic" layer (Controllers call Services, Services call Repositories)
    │   └── resources/
    │       └── application.yml # Configuration properties (database URLs, server port, JWT secrets)
    └── test/                # Unit and Integration tests
```

### The Flow of Data
When a user requests a product:
1.  Request hits `ProductController`.
2.  Controller asks `ProductService` for the data.
3.  Service asks `ProductRepository` to fetch it from MongoDB.
4.  Data returns as a `Product` model.
5.  Service converts `Product` model to `ProductDTO` (to hide sensitive or unnecessary fields).
6.  Controller sends `ProductDTO` back to the frontend.

---

## 3. Frontend Architecture (`buy-01-frontend`)

The frontend is an Angular 17+ application. It is organized by features and technical responsibilities.

```text
buy-01-frontend/
├── package.json             # NPM dependencies (Angular, RxJS, Bootstrap, etc.)
├── angular.json             # Angular CLI configuration
├── Dockerfile               # Builds the frontend for Nginx serving
└── src/
    ├── index.html           # The main HTML file loaded by the browser
    ├── main.ts              # The entry point that starts the Angular app
    ├── styles.scss          # Global CSS styles
    └── app/                 # The core application code
        ├── app.component.*  # The root component
        ├── app.routes.ts    # Defines which URL shows which component (e.g., '/cart' -> CartViewComponent)
        ├── app.config.ts    # App-wide providers (HTTP client, router)
        │
        ├── components/      # UI Building Blocks (Folders containing .html, .scss, and .ts files)
        │   ├── login/       # Login screen
        │   ├── navbar/      # Top navigation bar
        │   ├── product-list/# Displays the grid of products
        │   ├── cart-view/   # Displays the shopping cart
        │   └── ...          # Many other UI components
        │
        ├── models/          # TypeScript interfaces (defines the shape of data, matches backend DTOs)
        │   ├── product.model.ts
        │   └── user.model.ts
        │
        ├── services/        # Logic classes that talk to the backend APIs
        │   ├── auth.service.ts    # Handles login/logout API calls
        │   ├── product.service.ts # Fetches products from the Product Service
        │   └── cart.service.ts    # Manages cart state and API calls
        │
        ├── interceptors/    # Middleware for outgoing HTTP requests
        │   ├── auth.interceptor.ts # Automatically adds the JWT token to requests
        │   └── error.interceptor.ts# Automatically handles 401 Unauthorized errors (logs user out)
        │
        └── guards/          # Route protection
            └── auth.guard.ts# Prevents users from going to '/checkout' if they aren't logged in
```

### Component Structure
Every folder inside `src/app/components/` represents a visual piece of the screen and contains three files:
1.  `component-name.html`: The structure (HTML).
2.  `component-name.scss`: The styling (CSS/SCSS).
3.  `component-name.ts`: The logic (TypeScript) that controls the HTML and calls `services` to get data.
