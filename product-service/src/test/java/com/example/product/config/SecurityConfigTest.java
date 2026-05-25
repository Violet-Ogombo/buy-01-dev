package com.example.product.config;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.*;

class SecurityConfigTest {

    @Test
    void classHasConfigurationAndEnableMethodSecurity() {
        Class<?> cls = SecurityConfig.class;
        boolean hasConfig = cls.isAnnotationPresent(org.springframework.context.annotation.Configuration.class);
        boolean hasEnable = cls.isAnnotationPresent(org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity.class);
        assertThat(hasConfig).isTrue();
        assertThat(hasEnable).isTrue();
    }

    @Test
    void filterChainMethodExists() throws Exception {
        var method = SecurityConfig.class.getDeclaredMethod("filterChain", org.springframework.security.config.annotation.web.builders.HttpSecurity.class);
        assertThat(method).isNotNull();
        assertThat(method.getReturnType()).isEqualTo(org.springframework.security.web.SecurityFilterChain.class);
    }
}
