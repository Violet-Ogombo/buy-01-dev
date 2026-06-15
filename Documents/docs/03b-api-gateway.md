# API Gateway Low-Level Deep Dive

The API Gateway (`api-gateway`) acts as the single entryway for the frontend. It is responsible for routing requests downstream using Eureka service registration identifiers, configuring Cross-Origin Resource Sharing (CORS), checking authorization, validating incoming JWTs, injecting headers for downstream microservices, and logging latency.

---

## 1. Application Configuration (`application.yml`)
Path: [application.yml](file:///Users/aung.min/Desktop/github/buy-01-dev/api-gateway/src/main/resources/application.yml)

### Key Port and Global Settings:
*   `server.port: 8080`: Configures the gateway to listen on port `8080`.
*   `jwt.secret`: Loaded from environment variable `${JWT_SECRET}` (with a default fallback). Used to cryptographically verify signature blocks of JWT tokens.
*   `spring.cloud.gateway.httpclient`: Connect timeout set to 5000ms, Response timeout set to 5000ms. Insecure SSL trust manager is enabled (`use-insecure-trust-manager: true`) to allow localhost HTTPS/self-signed certificates.

### Routing Configuration:
The routing definitions map incoming paths using path-matching predicates and route them to downstream instances using Eureka load balancing (`lb://`).
1.  **`identity-service`**:
    *   Path Predicate: `/api/auth/**`
    *   Filters: `RewritePath=/api/auth/?(?<segment>.*), /${segment}` which strips the `/api/auth` prefix so `/api/auth/login` is sent to `identity-service` as `/login`.
2.  **`user-service`**:
    *   Path Predicate: `/api/users/**`
    *   Filters: Strips prefix `/api/users` and routes to `/me` or other user endpoints.
3.  **`product-service`**:
    *   Path Predicate: `/api/products/**`
    *   Filters: Strips `/api/products` prefix. Preserves proto headers.
4.  **`order-service-cart`**:
    *   Path Predicate: `/api/v1/cart/**`
    *   Filters: Routes to `/api/v1/cart/**` on `order-service` without prefix rewriting.
5.  **`order-service-orders`**:
    *   Path Predicate: `/api/v1/orders/**`
    *   Filters: Routes to `/api/v1/orders/**` on `order-service` without prefix rewriting.
6.  **`order-service-buyers`**:
    *   Path Predicate: `/api/v1/buyers/**`
    *   Filters: Routes to `/api/v1/buyers/**` on `order-service`.
7.  **`product-service-v1`**:
    *   Path Predicate: `/api/v1/**`
    *   Filters: Routes to `/api/v1/**` on `product-service` (used by Wishlist and Seller endpoints).
8.  **`media-service`**:
    *   Path Predicate: `/api/media/**`
    *   Filters: Strips the first two segments (`/api/media`), routing directly to `media-service`.

---

## 2. Core Security & CORS Configuration (`SecurityConfig.java`)
Path: [SecurityConfig.java](file:///Users/aung.min/Desktop/github/buy-01-dev/api-gateway/src/main/java/com/example/gateway/config/SecurityConfig.java)

This class implements security filters for Spring WebFlux.

### Methods:
*   `public CorsConfigurationSource corsConfigurationSource()`:
    *   **Parameters**: None
    *   **Return Type**: `CorsConfigurationSource`
    *   **Logic**:
        *   Instantiates a new `CorsConfiguration`.
        *   Configures allowed origins to: `https://localhost`, `https://localhost:443`, `http://localhost`, `http://localhost:80`.
        *   Allows HTTP methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, `PATCH`.
        *   Specifies allowed headers: `*`, `Authorization`, `Content-Type`, `X-Requested-With`, `X-XSRF-TOKEN`.
        *   Sets `AllowCredentials` to `true` to allow frontend to pass Cookies and authorization headers.
        *   Exposes `Authorization` and `Content-Type` headers to client JS scripts.
        *   Sets Preflight cache max age (`MaxAge`) to `86400` seconds (24 hours).
        *   Registers this config pattern for all paths (`/**`) and returns a reactive `UrlBasedCorsConfigurationSource`.
*   `public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, CorsConfigurationSource corsConfigurationSource)`:
    *   **Parameters**: `ServerHttpSecurity http` - Security DSL builder, `CorsConfigurationSource corsConfigurationSource` - Configured CORS source.
    *   **Return Type**: `SecurityWebFilterChain`
    *   **Logic**:
        *   Enables CORS using the passed configuration.
        *   Disables CSRF (stateless gateway behind reverse-proxy).
        *   Permits all exchanges (`anyExchange().permitAll()`) because route-level security validation is handled at the controller level inside each microservice.
        *   Builds and returns the security filter chain.

---

## 3. JWT & Header Mutation Filters

### `JwtAuthenticationFilter.java`
Path: [JwtAuthenticationFilter.java](file:///Users/aung.min/Desktop/github/buy-01-dev/api-gateway/src/main/java/com/example/gateway/filter/JwtAuthenticationFilter.java)

An explicit gateway filter factory that can be attached to routes to enforce JWT authentication.

#### Properties:
*   `jwtSecret`: Injected from `${jwt.secret}`. Holds the signature verification key.

#### Methods:
*   `public JwtAuthenticationFilter()`:
    *   Constructor calls `super(Config.class)` to register configuration settings.
*   `public GatewayFilter apply(Config config)`:
    *   **Parameters**: `Config config` - Holds filter custom configurations.
    *   **Return Type**: `GatewayFilter`
    *   **Logic**: Returns a lambda representing a gateway filter:
        1.  Extracts the incoming HTTP request path and HTTP method string.
        2.  Calls `isPublicPath(path, method)`. If `true`, calls `chain.filter(exchange)` to pass the request unmodified.
        3.  Retrieves `Authorization` header. If missing or not starting with `Bearer `, calls `onError` returning HTTP `401 Unauthorized` with a "Missing or invalid Authorization header" message.
        4.  Parses the token string (extracts index 7 to end).
        5.  Uses `Jwts.parserBuilder().setSigningKey(keyBytes).build().parseClaimsJws(token).getBody()` to validate and parse claims.
        6.  Extracts `userId` and `role` claims.
        7.  Mutates request headers, adding `X-User-Id` and `X-User-Role`.
        8.  Re-builds request and chain-passes mutated exchange downstream.
        9.  If validation throws exceptions (e.g. token expired/invalid), catches it and returns HTTP `401 Unauthorized` with an "Invalid or expired token" message.
*   `private boolean isPublicPath(String path, String method)`:
    *   **Parameters**: `path` (String), `method` (String)
    *   **Return Type**: `boolean`
    *   **Logic**: Checks if the request should bypass token checks:
        *   Always permits login (`/api/auth/login`) and register (`/api/auth/register`).
        *   If method is `GET`, permits:
            *   Product listings `/api/products` or details `/api/products/{id}`.
            *   Media images `/api/media/images/{id}` (except `/api/media/images/my-uploads` which is protected).
            *   Product listings for media files `/api/media/images/product/{id}`.
*   `private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message)`:
    *   **Parameters**: `ServerWebExchange exchange`, `HttpStatus status`, `String message`
    *   **Return Type**: `Mono<Void>` (Reactive Completion Signal)
    *   **Logic**: Mutates response status code to the given `HttpStatus`. Sets content type to `application/json`. Serializes `{"error": "<message>"}` error payload into response buffer and writes it using reactor publisher.

---

### `GlobalJwtAuthenticationFilter.java`
Path: [GlobalJwtAuthenticationFilter.java](file:///Users/aung.min/Desktop/github/buy-01-dev/api-gateway/src/main/java/com/example/gateway/filter/GlobalJwtAuthenticationFilter.java)

A global filter executed on *every* single route to extract JWT tokens if present, and forward claims as headers downstream.

#### Methods:
*   `public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)`:
    *   **Parameters**: `ServerWebExchange exchange` - HTTP Request/Response wrapper, `GatewayFilterChain chain` - Filter chain.
    *   **Return Type**: `Mono<Void>`
    *   **Logic**:
        1.  Extracts path and HTTP method.
        2.  Checks `isMultipartRequest(exchange)`.
        3.  If request content type is multipart (e.g., file uploads), calls `handleMultipartRequest`.
        4.  Otherwise, calls `handleRegularRequest`.
*   `private boolean isMultipartRequest(ServerWebExchange exchange)`:
    *   **Parameters**: `ServerWebExchange exchange`
    *   **Return Type**: `boolean`
    *   **Logic**: Returns true if the request header content-type exists and contains the substring `"multipart"`.
*   `private Mono<Void> handleMultipartRequest(...)`:
    *   **Logic**: Bypasses mutating request headers because Netty gateway might fail or buffer content during multipart serialization. It still validates the JWT token in the background using `validateJwtIfPresent()` but leaves the request headers unmodified.
*   `private Mono<Void> handleRegularRequest(...)`:
    *   **Logic**: Extracts and validates the token. If valid, extracts claims and returns `addAuthHeadersAndContinue(...)` which injects `X-User-Id`, `X-User-Email`, and `X-User-Role` headers. Otherwise, proceeds to next filter chain.
*   `private Optional<Claims> extractAndValidateJwt(...)`:
    *   **Logic**: Reads the authorization header. If starts with `Bearer `, parses it against signature `jwtSecret`. Returns parsed `Claims` wrapper, or `Optional.empty()` if invalid or missing.
*   `private Mono<Void> addAuthHeadersAndContinue(...)`:
    *   **Logic**: Mutates request exchange to inject authentication headers `X-User-Id`, `X-User-Email` and `X-User-Role` and passes the new exchange down the chain.
*   `public int getOrder()`:
    *   **Return Type**: `int` (value `-100`)
    *   **Logic**: Runs early in the gateway execution order so that downstream route filters can read mutated auth headers.

---

### `LoggingFilter.java`
Path: [LoggingFilter.java](file:///Users/aung.min/Desktop/github/buy-01-dev/api-gateway/src/main/java/com/example/gateway/filter/LoggingFilter.java)

Global filter to measure and log execution latency.

#### Methods:
*   `public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)`:
    *   **Logic**: Records startup timestamp `System.currentTimeMillis()`. Calls `chain.filter(exchange)` and chains `doFinally` reactive hook. When completed, calculates time difference and logs HTTP method, path, response status, duration, and `X-User-Id` header.
*   `public int getOrder()`:
    *   **Return Type**: `int` (value `Ordered.HIGHEST_PRECEDENCE`)
    *   **Logic**: Runs before all filters to capture full latency.

---

## 4. Test Summary

### `GlobalJwtAuthenticationFilterTest.java`
Path: [GlobalJwtAuthenticationFilterTest.java](file:///Users/aung.min/Desktop/github/buy-01-dev/api-gateway/src/test/java/com/example/gateway/filter/GlobalJwtAuthenticationFilterTest.java)

This JUnit test file validates `GlobalJwtAuthenticationFilter` behaviors using Mockito framework.

#### Behaviors Verified:
*   **Order and Implementation Assertions**: Ensures it implements `GlobalFilter`, `Ordered`, and returns order `-100`.
*   **Valid JWT handling**: Simulates a valid JWT Bearer header and verifies that downstream exchange contains injected `X-User-Id` and `X-User-Role` headers.
*   **Anonymous Requests**: Verifies that requests without Authorization headers pass successfully without header mutations.
*   **Invalid & Malformed Tokens**: Parameterized tests verifying that invalid headers (e.g. malformed prefix, wrong secret keys) or expired tokens pass through to downstream without setting authentication headers.
*   **Multipart Upload Bypass**: Asserts that requests containing multipart content skip header modifications.
*   **Missing Claims**: Confirms that tokens missing optional email or userId fields still pass verification without throwing errors.
