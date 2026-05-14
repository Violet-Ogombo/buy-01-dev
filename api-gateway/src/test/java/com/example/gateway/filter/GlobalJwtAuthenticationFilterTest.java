package com.example.gateway.filter;

import io.jsonwebtoken.Claims;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchangeBuilder;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalJwtAuthenticationFilterTest {

    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private GlobalJwtAuthenticationFilter filter;

    private String jwtSecret = "my-secret-key-for-testing-jwt-operations-in-gateway-filter";

    @BeforeEach
    void setUp() {
        // Inject the JWT secret via reflection
        ReflectionTestUtils.setField(filter, "jwtSecret", jwtSecret);
    }

    private String generateValidJwt(String userId, String role) {
        return Jwts.builder()
            .setSubject("user-subject")
            .claim("userId", userId)
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
            .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes(StandardCharsets.UTF_8))
            .compact();
    }

    @Test
    void filter_returnsOrderValue() {
        // Act
        int order = filter.getOrder();

        // Assert
        assertThat(order).isNotNull();
    }

    @Test
    void filter_processesRequestWithValidJwt() {
        // Arrange
        String userId = "user-123";
        String role = "SELLER";
        String token = generateValidJwt(userId, role);

        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();

        ServerWebExchange exchange = new DefaultServerWebExchangeBuilder(request)
            .response(new MockServerHttpResponse())
            .build();

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(exchange);
    }

    @Test
    void filter_processesRequestWithoutAuthorizationHeader() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products")
            .build();

        ServerWebExchange exchange = new DefaultServerWebExchangeBuilder(request)
            .response(new MockServerHttpResponse())
            .build();

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(exchange);
    }

    @Test
    void filter_processesRequestWithMalformedAuthHeader() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products")
            .header(HttpHeaders.AUTHORIZATION, "NotBearer token")
            .build();

        ServerWebExchange exchange = new DefaultServerWebExchangeBuilder(request)
            .response(new MockServerHttpResponse())
            .build();

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(exchange);
    }

    @Test
    void filter_skipsHeaderMutationForMultipartRequest() {
        // Arrange
        String userId = "user-123";
        String role = "SELLER";
        String token = generateValidJwt(userId, role);

        MockServerHttpRequest request = MockServerHttpRequest
            .post("/api/media/upload")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
            .build();

        ServerWebExchange exchange = new DefaultServerWebExchangeBuilder(request)
            .response(new MockServerHttpResponse())
            .build();

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(exchange);
    }

    @Test
    void filter_handlesMissingJwtSecret() {
        // Arrange
        ReflectionTestUtils.setField(filter, "jwtSecret", null);

        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.here")
            .build();

        ServerWebExchange exchange = new DefaultServerWebExchangeBuilder(request)
            .response(new MockServerHttpResponse())
            .build();

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(exchange);
    }

    @Test
    void filter_processesGetRequest() {
        // Arrange
        String token = generateValidJwt("user-123", "SELLER");

        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();

        ServerWebExchange exchange = new DefaultServerWebExchangeBuilder(request)
            .response(new MockServerHttpResponse())
            .build();

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(exchange);
        assertThat(exchange.getRequest().getMethod()).isEqualTo(HttpMethod.GET);
    }

    @Test
    void filter_processesPostRequest() {
        // Arrange
        String token = generateValidJwt("user-123", "SELLER");

        MockServerHttpRequest request = MockServerHttpRequest
            .post("/api/products")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .build();

        ServerWebExchange exchange = new DefaultServerWebExchangeBuilder(request)
            .response(new MockServerHttpResponse())
            .build();

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(exchange);
        assertThat(exchange.getRequest().getMethod()).isEqualTo(HttpMethod.POST);
    }
}
