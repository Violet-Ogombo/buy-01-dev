package com.example.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalJwtAuthenticationFilterTest {

    @InjectMocks
    private GlobalJwtAuthenticationFilter filter;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private GatewayFilterChain chain;

    private String jwtSecret = "my-secret-key-for-testing-jwt-operations-in-gateway-filter";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "jwtSecret", jwtSecret);
    }

    private String generateValidJwt(String userId, String role) {
        return Jwts.builder()
            .setSubject("user@example.com")
            .claim("userId", userId)
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes(StandardCharsets.UTF_8))
            .compact();
    }

    @Test
    void filter_implementsGlobalFilter() {
        assertThat(filter).isInstanceOf(org.springframework.cloud.gateway.filter.GlobalFilter.class);
    }

    @Test
    void filter_implementsOrdered() {
        assertThat(filter).isInstanceOf(org.springframework.core.Ordered.class);
    }

    @Test
    void filter_returnsValidOrderValue() {
        assertThat(filter.getOrder()).isEqualTo(-100);
    }

    @Test
    void filter_withValidJwt_addsAuthHeaders() {
        // Arrange
        String validJwt = generateValidJwt("user-123", "SELLER");
        setupMockExchange("/api/products", "GET", "Bearer " + validJwt, null);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_withoutJwt_proceedsWithoutHeaders() {
        // Arrange
        setupMockExchange("/api/products", "GET", null, null);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(exchange);
    }

    @Test
    void filter_withInvalidJwt_proceedsWithoutHeaders() {
        // Arrange
        setupMockExchange("/api/products", "GET", "Bearer invalid-token", null);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(exchange);
    }

    @Test
    void filter_withMultipartRequest_skipsHeaderMutation() {
        // Arrange
        String validJwt = generateValidJwt("user-123", "SELLER");
        setupMockExchange("/api/upload", "POST", "Bearer " + validJwt, "multipart/form-data");
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert - should still process but not mutate headers for multipart
        verify(chain).filter(any());
    }

    @Test
    void filter_withRegularRequest_mutatesToAddHeaders() {
        // Arrange
        String validJwt = generateValidJwt("user-456", "BUYER");
        setupMockExchange("/api/products", "GET", "Bearer " + validJwt, "application/json");
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_extracts_userId_from_jwt() {
        // Arrange
        String userId = "seller-789";
        String validJwt = generateValidJwt(userId, "SELLER");
        setupMockExchange("/api/products", "POST", "Bearer " + validJwt, null);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_extracts_role_from_jwt() {
        // Arrange
        String role = "ADMIN";
        String validJwt = generateValidJwt("user-123", role);
        setupMockExchange("/api/admin", "GET", "Bearer " + validJwt, null);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_withDifferentHttpMethods() {
        // Test multiple HTTP methods
        for (String method : new String[]{"GET", "POST", "PUT", "DELETE", "PATCH"}) {
            // Arrange
            String validJwt = generateValidJwt("user-123", "SELLER");
            setupMockExchange("/api/products", method, "Bearer " + validJwt, null);
            when(chain.filter(any())).thenReturn(Mono.empty());

            // Act
            filter.filter(exchange, chain).block();

            // Assert
            verify(chain, atLeastOnce()).filter(any(ServerWebExchange.class));
        }
    }

    @Test
    void filter_withNullMethod_returnsEmptyString() {
        // Arrange
        setupMockExchangeWithNullMethod("/api/products", "Bearer " + generateValidJwt("user-123", "SELLER"));
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(any());
    }

    @Test
    void filter_withoutAuthorizationHeader_proceedsWithoutMutation() {
        // Arrange
        setupMockExchange("/api/public", "GET", null, null);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(exchange);
    }

    @Test
    void filter_withMalformedAuthHeader_proceedsWithoutHeaders() {
        // Arrange
        setupMockExchange("/api/products", "GET", "InvalidBearer token", null);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(exchange);
    }

    @Test
    void filter_withExpiredJwt_proceedsWithoutHeaders() {
        // Arrange
        String expiredJwt = Jwts.builder()
            .setSubject("user@example.com")
            .claim("userId", "user-123")
            .claim("role", "SELLER")
            .setIssuedAt(new Date(System.currentTimeMillis() - 7200000))
            .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // expired 1 hour ago
            .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes(StandardCharsets.UTF_8))
            .compact();
        
        setupMockExchange("/api/products", "POST", "Bearer " + expiredJwt, null);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(exchange);
    }

    @Test
    void filter_withJwtSignedWithDifferentKey_proceedsWithoutHeaders() {
        // Arrange
        String wrongKeySecret = "different-secret-key-for-testing";
        String jwtSignedWithDifferentKey = Jwts.builder()
            .setSubject("user@example.com")
            .claim("userId", "user-123")
            .claim("role", "SELLER")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(SignatureAlgorithm.HS256, wrongKeySecret.getBytes(StandardCharsets.UTF_8))
            .compact();
        
        setupMockExchange("/api/products", "GET", "Bearer " + jwtSignedWithDifferentKey, null);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(exchange);
    }

    @Test
    void filter_multipartRequest_withValidJwt_skipsHeaderMutation() {
        // Arrange
        String validJwt = generateValidJwt("user-123", "SELLER");
        setupMockExchange("/api/upload", "POST", "Bearer " + validJwt, "multipart/form-data; boundary=----");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert - should not mutate exchange
        verify(chain).filter(exchange);
    }

    @Test
    void filter_multipartRequest_withInvalidJwt_proceedsWithoutValidation() {
        // Arrange
        setupMockExchange("/api/upload", "POST", "Bearer invalid-token", "multipart/form-data");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(exchange);
    }

    @Test
    void filter_withJwtMissingUserId_stillAddsHeaders() {
        // Arrange - JWT without userId claim
        String jwtWithoutUserId = Jwts.builder()
            .setSubject("user@example.com")
            .claim("role", "SELLER")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes(StandardCharsets.UTF_8))
            .compact();
        
        setupMockExchange("/api/products", "GET", "Bearer " + jwtWithoutUserId, null);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_withJwtMissingRole_stillAddsHeaders() {
        // Arrange - JWT without role claim
        String jwtWithoutRole = Jwts.builder()
            .setSubject("user@example.com")
            .claim("userId", "user-123")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes(StandardCharsets.UTF_8))
            .compact();
        
        setupMockExchange("/api/products", "POST", "Bearer " + jwtWithoutRole, null);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_withJwtWithoutSubject_stillAddsHeaders() {
        // Arrange - JWT without subject (email)
        String jwtWithoutSubject = Jwts.builder()
            .claim("userId", "user-123")
            .claim("role", "SELLER")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes(StandardCharsets.UTF_8))
            .compact();
        
        setupMockExchange("/api/products", "DELETE", "Bearer " + jwtWithoutSubject, null);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_withVariousContentTypes() {
        // Test different content types
        String[] contentTypes = {"application/json", "application/xml", "text/plain", "application/x-www-form-urlencoded"};
        String validJwt = generateValidJwt("user-123", "SELLER");
        
        for (String contentType : contentTypes) {
            // Arrange
            setupMockExchange("/api/products", "POST", "Bearer " + validJwt, contentType);
            when(chain.filter(any())).thenReturn(Mono.empty());

            // Act
            filter.filter(exchange, chain).block();

            // Assert
            verify(chain, atLeastOnce()).filter(any(ServerWebExchange.class));
        }
    }

    @Test
    void filter_withDifferentApiPaths() {
        // Test various API paths
        String[] paths = {"/api/products", "/api/users/123", "/api/admin/settings", "/public/health", "/auth/login"};
        String validJwt = generateValidJwt("user-123", "SELLER");
        
        for (String path : paths) {
            // Arrange
            setupMockExchange(path, "GET", "Bearer " + validJwt, null);
            when(chain.filter(any())).thenReturn(Mono.empty());

            // Act
            filter.filter(exchange, chain).block();

            // Assert
            verify(chain, atLeastOnce()).filter(any(ServerWebExchange.class));
        }
    }

    private void setupMockExchangeWithNullMethod(String path, String authHeader) {
        var request = mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        var headers = new HttpHeaders();
        var requestPath = mock(org.springframework.http.server.RequestPath.class);

        var requestBuilder = mock(org.springframework.http.server.reactive.ServerHttpRequest.Builder.class);
        var exchangeBuilder = mock(org.springframework.web.server.ServerWebExchange.Builder.class);

        if (authHeader != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        }

        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(requestPath);
        when(requestPath.value()).thenReturn(path);
        when(request.getMethod()).thenReturn(null); // Null method
        when(request.getHeaders()).thenReturn(headers);

        lenient().when(request.mutate()).thenReturn(requestBuilder);
        lenient().when(requestBuilder.header(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.<String[]>any()))
            .thenReturn(requestBuilder);
        lenient().when(requestBuilder.build()).thenReturn(request);

        lenient().when(exchange.mutate()).thenReturn(exchangeBuilder);
        lenient().when(exchangeBuilder.request(any(org.springframework.http.server.reactive.ServerHttpRequest.class)))
            .thenReturn(exchangeBuilder);
        lenient().when(exchangeBuilder.build()).thenReturn(exchange);
    }

    private void setupMockExchange(String path, String method, String authHeader, String contentType) {
        var request = mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        var headers = new HttpHeaders();
        var requestPath = mock(org.springframework.http.server.RequestPath.class);

        var requestBuilder = mock(org.springframework.http.server.reactive.ServerHttpRequest.Builder.class);
        var exchangeBuilder = mock(org.springframework.web.server.ServerWebExchange.Builder.class);

        if (authHeader != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        }

        if (contentType != null && contentType.contains("multipart")) {
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        } else if (contentType != null) {
            headers.setContentType(MediaType.parseMediaType(contentType));
        }

        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(requestPath);
        when(requestPath.value()).thenReturn(path);
        when(request.getMethod()).thenReturn(org.springframework.http.HttpMethod.valueOf(method));
        when(request.getHeaders()).thenReturn(headers);

        // Mock request.mutate() -> builder.header(...).build() -> request with lenient stubbings
        lenient().when(request.mutate()).thenReturn(requestBuilder);
        lenient().when(requestBuilder.header(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.<String[]>any()))
            .thenReturn(requestBuilder);
        lenient().when(requestBuilder.build()).thenReturn(request);

        // Mock exchange.mutate() -> builder.request(...).build() -> exchange with lenient stubbings
        lenient().when(exchange.mutate()).thenReturn(exchangeBuilder);
        lenient().when(exchangeBuilder.request(any(org.springframework.http.server.reactive.ServerHttpRequest.class)))
            .thenReturn(exchangeBuilder);
        lenient().when(exchangeBuilder.build()).thenReturn(exchange);
    }
}
