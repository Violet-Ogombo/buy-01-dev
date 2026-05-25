package com.example.mediaservice.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigTest {

    private WebConfig webConfig = new WebConfig();

    @Test
    void webConfig_implementsWebMvcConfigurer() {
        assertThat(webConfig).isInstanceOf(org.springframework.web.servlet.config.annotation.WebMvcConfigurer.class);
    }

    @Test
    void webConfig_isConfigurable() {
        assertThat(WebConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class)).isTrue();
    }

    @Test
    void addCorsMappings_methodIsPresent() throws Exception {
        // The addCorsMappings method is defined and will be called by Spring
        // to configure CORS mappings
        var method = webConfig.getClass().getMethod("addCorsMappings", 
            org.springframework.web.servlet.config.annotation.CorsRegistry.class);
        assertThat(method).isNotNull();
    }
}

