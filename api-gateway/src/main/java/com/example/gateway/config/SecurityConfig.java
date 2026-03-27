package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Enhanced security configuration for API Gateway with comprehensive security headers
 */
@Configuration
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Allow HTTPS frontend origin
        corsConfig.addAllowedOrigin("https://localhost");
        corsConfig.addAllowedOrigin("https://localhost:443");
        corsConfig.addAllowedOrigin("http://localhost");
        corsConfig.addAllowedOrigin("http://localhost:80");
        
        // Allow specific HTTP methods
        corsConfig.addAllowedMethod(HttpMethod.GET);
        corsConfig.addAllowedMethod(HttpMethod.POST);
        corsConfig.addAllowedMethod(HttpMethod.PUT);
        corsConfig.addAllowedMethod(HttpMethod.DELETE);
        corsConfig.addAllowedMethod(HttpMethod.OPTIONS);
        corsConfig.addAllowedMethod(HttpMethod.PATCH);
        
        // Allow specific headers
        corsConfig.addAllowedHeader("*");
        corsConfig.addAllowedHeader("Authorization");
        corsConfig.addAllowedHeader("Content-Type");
        corsConfig.addAllowedHeader("X-Requested-With");
        corsConfig.addAllowedHeader("X-XSRF-TOKEN");
        
        // Allow credentials (cookies, authorization headers)
        corsConfig.setAllowCredentials(true);
        
        // Expose certain headers to client
        corsConfig.addExposedHeader("Authorization");
        corsConfig.addExposedHeader("Content-Type");
        
        // Max age for preflight cache (24 hours)
        corsConfig.setMaxAge(86400L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, CorsConfigurationSource corsConfigurationSource) {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrfSpec -> csrfSpec.disable())
            .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
        
        return http.build();
    }

    /**
     * Disabled: Security headers WebFilter was causing read-only header exceptions
     * when trying to add headers after response was already committed.
     * This caused all API requests to fail with 502 Bad Gateway.
     * Consider using Spring Cloud Gateway response filters or Nginx for header injection.
     */
    // @Bean
    // public WebFilter securityHeadersWebFilter() { ... }
}
