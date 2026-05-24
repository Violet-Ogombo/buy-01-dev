package com.example.identity.service;

import com.example.identity.model.Role;
import com.example.identity.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String JWT_SECRET = "test-secret-key-32-chars-minimum!";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(JWT_SECRET);
    }

    @Test
    void generateToken_createsValidJwtWithUserInfo() {
        User user = new User();
        user.setId("user-123");
        user.setEmail("test@example.com");
        user.setRole(Role.SELLER);

        String token = jwtService.generateToken(user);

        assertThat(token).isNotNull().isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void generateToken_tokenContainsCorrectClaims() {
        User user = new User();
        user.setId("user-456");
        user.setEmail("seller@example.com");
        user.setRole(Role.CLIENT);

        String token = jwtService.generateToken(user);

        // Parse token to verify claims
        Key key = new SecretKeySpec(JWT_SECRET.getBytes(), "HmacSHA256");
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.get("userId", String.class)).isEqualTo("user-456");
        assertThat(claims.get("role", String.class)).isEqualTo("CLIENT");
        assertThat(claims.getSubject()).isEqualTo("seller@example.com");
    }

    @Test
    void generateToken_tokenHasExpirationTime() {
        User user = new User();
        user.setId("user-789");
        user.setEmail("buyer@example.com");
        user.setRole(Role.SELLER);

        String token = jwtService.generateToken(user);

        Key key = new SecretKeySpec(JWT_SECRET.getBytes(), "HmacSHA256");
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Date expirationTime = claims.getExpiration();
        assertThat(expirationTime).isAfter(new Date());
    }

    @Test
    void generateToken_multipleCallsProduceDifferentTokens() {
        User user = new User();
        user.setId("user-123");
        user.setEmail("test@example.com");
        user.setRole(Role.SELLER);

        String token1 = jwtService.generateToken(user);
        String token2 = jwtService.generateToken(user);

        // Tokens should be different due to issuedAt timestamp
        assertThat(token1).isNotEqualTo(token2);
    }
}
