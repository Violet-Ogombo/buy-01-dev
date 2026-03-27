package com.example.identity.filter;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Reads authentication headers from API Gateway and sets Spring Security context
 * This allows the @PreAuthorize("isAuthenticated()") annotation to work
 */
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Read headers forwarded by API Gateway
        String userId = request.getHeader("X-User-Id");
        String userEmail = request.getHeader("X-User-Email");
        String userRole = request.getHeader("X-User-Role");
        
        // If headers exist, create authentication
        if (userId != null && !userId.isEmpty()) {
            // Create authorities from role
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            if (userRole != null && !userRole.isEmpty()) {
                // Add ROLE_ prefix for Spring Security compatibility
                authorities.add(new SimpleGrantedAuthority("ROLE_" + userRole));
            }
            
            // Use email as principal (for database lookup), or userId if email not available
            String principal = (userEmail != null && !userEmail.isEmpty()) ? userEmail : userId;
            
            // Create authentication token with detailed information
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,    // principal (email preferred, fallback to userId)
                null,         // credentials (no password)
                authorities   // authorities (with proper roles)
            );
            auth.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetailsSource()
                .buildDetails(request));
            
            // Set in security context so @PreAuthorize works
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        
        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}
