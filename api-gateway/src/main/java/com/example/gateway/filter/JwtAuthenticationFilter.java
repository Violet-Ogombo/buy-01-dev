package com.example.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    public JwtAuthenticationFilter() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();
            String method = request.getMethod() != null ? request.getMethod().name() : "";

            // Allow public endpoints without authentication
            if (isPublicPath(path, method)) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            try {
                @SuppressWarnings("null")
                byte[] keyBytes = jwtSecret != null ? jwtSecret.getBytes(StandardCharsets.UTF_8) : new byte[0];
                
                Claims claims = Jwts.parserBuilder()
                    .setSigningKey(keyBytes)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

                String userId = claims.get("userId", String.class);
                String role = claims.get("role", String.class);

                log.info("✅ JWT validated - userId: {}, role: {}", userId, role);
                
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .build();
                
                log.info("✅ Headers added to request - X-User-Id: {}, X-User-Role: {}", userId, role);

                ServerWebExchange modifiedExchange = exchange.mutate().request(modifiedRequest).build();
                return chain.filter(modifiedExchange);
            } catch (Exception e) {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            }
        };
    }

    private boolean isPublicPath(String path, String method) {
        String m = method != null ? method.toUpperCase() : "";

        // Auth endpoints handled by identity-service route (no filter attached),
        // but keep here for completeness if applied later.
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
            return true;
        }

        if ("GET".equals(m)) {
            // Public product listing & details
            if (path.equals("/api/products") || path.startsWith("/api/products/")) {
                return true;
            }

            // Public image access by id
            if (path.startsWith("/api/media/images/")) {
                // Seller-only uploads listing must remain protected
                if (path.startsWith("/api/media/images/my-uploads")) {
                    return false;
                }
                return true;
            }

            // Public product images listing
            if (path.startsWith("/api/media/images/product/")) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("null")
    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"error\":\"" + (message != null ? message : "Unknown error") + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Flux.just(buffer));
    }
    
    public static class Config {
        // Configuration properties if needed
    }
}
