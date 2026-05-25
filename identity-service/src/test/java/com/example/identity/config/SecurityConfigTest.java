package com.example.identity.config;

import com.example.identity.filter.HeaderAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

class SecurityConfigTest {

    private SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void passwordEncoder_returnsValidBCryptPasswordEncoder() {
        BCryptPasswordEncoder encoder = securityConfig.passwordEncoder();
        assertThat(encoder).isNotNull();
        
        String plainPassword = "testPassword123";
        String encoded = encoder.encode(plainPassword);
        
        assertThat(encoded).isNotNull();
        assertThat(encoded).isNotEqualTo(plainPassword);
        assertThat(encoder.matches(plainPassword, encoded)).isTrue();
    }

    @Test
    void passwordEncoder_encodesDifferentlyEachTime() {
        BCryptPasswordEncoder encoder = securityConfig.passwordEncoder();
        String plainPassword = "password";
        
        String encoded1 = encoder.encode(plainPassword);
        String encoded2 = encoder.encode(plainPassword);
        
        // BCrypt uses salt, so different calls produce different hashes
        assertThat(encoded1).isNotEqualTo(encoded2);
        assertThat(encoder.matches(plainPassword, encoded1)).isTrue();
        assertThat(encoder.matches(plainPassword, encoded2)).isTrue();
    }
}
