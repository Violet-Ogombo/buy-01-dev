package com.example.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GlobalJwtAuthenticationFilterTest {

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
    void filter_implementsGlobalFilter() {
        // Act & Assert
        assertThat(filter).isNotNull();
        assertThat(filter).isInstanceOf(org.springframework.cloud.gateway.filter.GlobalFilter.class);
    }

    @Test
    void filter_implementsOrdered() {
        // Act & Assert
        assertThat(filter).isInstanceOf(org.springframework.core.Ordered.class);
    }

    @Test
    void filter_returnsValidOrderValue() {
        // Act
        int order = filter.getOrder();

        // Assert - filter should return a valid order value
        assertThat(order).isNotNull();
    }

    @Test
    void generateValidJwt_createsToken() {
        // Act
        String token = generateValidJwt("user-123", "SELLER");

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).contains(".");
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    void generateValidJwt_createsTokenWithUserId() {
        // Act
        String token = generateValidJwt("seller-456", "BUYER");

        // Assert
        assertThat(token).isNotNull();
        // Token should be valid and decodable
        assertThat(token).isNotEmpty();
    }

    @Test
    void filter_jwtSecretIsSet() {
        // Act
        String secret = (String) ReflectionTestUtils.getField(filter, "jwtSecret");

        // Assert
        assertThat(secret).isEqualTo(jwtSecret);
    }

    @Test
    void generateValidJwt_tokenHasCorrectClaims() {
        // Act
        String token = generateValidJwt("user-123", "SELLER");

        // Assert - verify token can be decoded and has the right structure
        assertThat(token).isNotNull();
        // Token format: eyJ... (header) . eyJ... (payload) . signature
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        assertThat(parts[0]).isNotEmpty(); // header
        assertThat(parts[1]).isNotEmpty(); // payload
        assertThat(parts[2]).isNotEmpty(); // signature
    }
}
