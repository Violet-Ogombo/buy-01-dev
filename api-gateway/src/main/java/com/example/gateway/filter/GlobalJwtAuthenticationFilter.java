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
        String method = getMethod(exchange);

        log.debug("🔍 GlobalJwtAuthenticationFilter processing: {} {}", method, path);

        if (isMultipartRequest(exchange)) {
            return handleMultipartRequest(exchange, chain, method, path);
        }

        return handleRegularRequest(exchange, chain, method, path);
    }

    private String getMethod(ServerWebExchange exchange) {
        return java.util.Optional.ofNullable(exchange.getRequest().getMethod())
            .map(m -> m.name())
            .orElse("");
    }

    private boolean isMultipartRequest(ServerWebExchange exchange) {
        var contentTypeObj = exchange.getRequest().getHeaders().getContentType();
        String contentType = contentTypeObj != null ? contentTypeObj.toString() : "";
        return contentType.contains("multipart");
    }

    private Mono<Void> handleMultipartRequest(ServerWebExchange exchange, GatewayFilterChain chain, 
                                               String method, String path) {
        log.debug("⏭️ Skipping header mutation for multipart request {} {}", method, path);
        validateJwtIfPresent(exchange, method, path);
        return chain.filter(exchange);
    }

    private Mono<Void> handleRegularRequest(ServerWebExchange exchange, GatewayFilterChain chain, 
                                            String method, String path) {
        var claims = extractAndValidateJwt(exchange, method, path);
        
        if (claims != null) {
            return addAuthHeadersAndContinue(exchange, chain, claims);
        }

        log.debug("→ Allowing {} {} to proceed (auth may be optional)", method, path);
        return chain.filter(exchange);
    }

    private void validateJwtIfPresent(ServerWebExchange exchange, String method, String path) {
        String authHeader = getAuthorizationHeader(exchange);
        if (authHeader != null) {
            extractAndValidateJwt(exchange, method, path);
        }
    }

    private Claims extractAndValidateJwt(ServerWebExchange exchange, String method, String path) {
        String authHeader = getAuthorizationHeader(exchange);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("⚠️ No Authorization header for {} {}", method, path);
            return null;
        }

        try {
            String token = authHeader.substring(7);
            byte[] keyBytes = getJwtKeyBytes();
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(keyBytes)
                .build()
                .parseClaimsJws(token)
                .getBody();

            logJwtSuccess(claims, method, path);
            return claims;
        } catch (Exception e) {
            log.warn("JWT validation failed for {} {}: {}", method, path, e.getMessage());
            return null;
        }
    }

    private String getAuthorizationHeader(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    }

    private byte[] getJwtKeyBytes() {
        return jwtSecret != null ? jwtSecret.getBytes(StandardCharsets.UTF_8) : new byte[0];
    }

    private void logJwtSuccess(Claims claims, String method, String path) {
        String userId = claims.get("userId", String.class);
        String role = claims.get("role", String.class);
        String email = claims.getSubject();
        log.info("✅ JWT valid - userId: {}, email: {}, role: {} for {} {}", userId, email, role, method, path);
    }

    private Mono<Void> addAuthHeadersAndContinue(ServerWebExchange exchange, GatewayFilterChain chain, 
                                                  Claims claims) {
        String userId = claims.get("userId", String.class);
        String role = claims.get("role", String.class);
        String email = claims.getSubject();

        return chain.filter(
            exchange.mutate()
                .request(exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-User-Role", role)
                    .build())
                .build()
        );
    }

    @Override
    public int getOrder() {
        // Run early to extract JWT and set headers for downstream services
        return -100;
    }
}
