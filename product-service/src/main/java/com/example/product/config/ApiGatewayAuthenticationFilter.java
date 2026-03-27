package com.example.product.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class ApiGatewayAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ApiGatewayAuthenticationFilter.class);
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");
        
        log.debug("ApiGatewayAuthenticationFilter - Path: {}, userId: {}, userRole: {}", 
                  request.getRequestURI(), userId, userRole);
        
        if (userId != null && userRole != null) {
            log.info("✅ Setting authentication - userId: {}, role: {}", userId, userRole);
            // Create authentication based on headers from API Gateway
            List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + userRole),
                new SimpleGrantedAuthority("ROLE_USER")
            );
            
            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
            
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else {
            log.warn("❌ Headers missing - userId: {}, userRole: {} - NO AUTHENTICATION SET", userId, userRole);
        }
        
        filterChain.doFilter(request, response);
    }
}