package com.example.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class GlobalJwtAuthenticationFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(GlobalJwtAuthenticationFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod() != null ? 
                       exchange.getRequest().getMethod().name() : "";

        log.debug("🔍 GlobalJwtAuthenticationFilter processing: {} {}", method, path);

        // For multipart requests (file uploads), DO NOT mutate the request
        // The Authorization header is already in the request; services read it directly
        var contentTypeObj = exchange.getRequest().getHeaders().getContentType();
        String contentType = contentTypeObj != null ? contentTypeObj.toString() : "";
        
        if (contentType.contains("multipart")) {
            log.debug("⏭️ Skipping header mutation for multipart request {} {}", method, path);
            // For multipart, just validate JWT and pass through
            // The downstream service will read Authorization header directly
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
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
                    log.info("✅ JWT valid for multipart {} {}: userId={}, role={}", method, path, userId, role);
                } catch (Exception e) {
                    log.warn("JWT validation failed for {} {}: {}", method, path, e.getMessage());
                }
            }
            // Pass through WITHOUT mutating request (this breaks multipart streaming)
            return chain.filter(exchange);
        }
        
        // For non-multipart requests, mutate to add headers
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        // Try to extract and validate JWT
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
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
                String email = claims.getSubject(); // Email is in the 'sub' claim

                log.info("✅ JWT valid - userId: {}, email: {}, role: {} for {} {}", userId, email, role, method, path);

                // Mutate request to add headers - ONLY for non-multipart
                return chain.filter(
                    exchange.mutate()
                        .request(exchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Email", email != null ? email : "")
                            .header("X-User-Role", role)
                            .build())
                        .build()
                );
            } catch (Exception e) {
                log.warn("JWT validation failed for {} {}: {}", method, path, e.getMessage());
            }
        } else {
            log.debug("⚠️ No Authorization header for {} {}", method, path);
        }

        // Pass through without auth headers (for public endpoints)
        log.debug("→ Allowing {} {} to proceed (auth may be optional)", method, path);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Run early to extract JWT and set headers for downstream services
        return -100;
    }
}
