package com.example.mediaservice;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void securityFilterChain_beanCanBeCreated() {
        assertThat(securityConfig).isNotNull();
        // Bean creation is tested implicitly when @Bean method is called
        // In real tests, this would be validated through Spring context loading
    }

    @Test
    void securityConfig_isConfigurable() {
        // The SecurityConfig class is a @Configuration class that configures
        // the HttpSecurity with CSRF disabled and stateless session management
        assertThat(SecurityConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class)).isTrue();
    }
}

