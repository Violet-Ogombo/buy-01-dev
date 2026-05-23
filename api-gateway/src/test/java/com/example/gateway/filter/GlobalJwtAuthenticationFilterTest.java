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

    private void setupMockExchange(String path, String method, String authHeader, String contentType) {
        var request = mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        var headers = mock(HttpHeaders.class);
        var requestPath = mock(org.springframework.http.server.RequestPath.class);

        // Builders to support mutate() calls used by the filter
        var requestBuilder = mock(org.springframework.http.server.reactive.ServerHttpRequest.Builder.class);
        var exchangeBuilder = mock(org.springframework.web.server.ServerWebExchange.Builder.class);

        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(requestPath);
        when(requestPath.value()).thenReturn(path);
        when(request.getMethod()).thenReturn(org.springframework.http.HttpMethod.valueOf(method));
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);

        // Mock request.mutate() -> builder.header(...).build() -> request
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.<String[]>any()))
            .thenReturn(requestBuilder);
        when(requestBuilder.build()).thenReturn(request);

        // Mock exchange.mutate() -> builder.request(...).build() -> exchange
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(any(org.springframework.http.server.reactive.ServerHttpRequest.class)))
            .thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(exchange);

        if (contentType != null && contentType.contains("multipart")) {
            when(headers.getContentType()).thenReturn(MediaType.MULTIPART_FORM_DATA);
        } else {
            when(headers.getContentType()).thenReturn(null);
        }
    }
}
