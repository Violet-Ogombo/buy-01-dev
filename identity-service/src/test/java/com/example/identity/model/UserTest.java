package com.example.identity.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void userGettersAndSetters() {
        // Given
        User user = new User();
        
        // When
        user.setId("user-123");
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setPassword("encrypted-password");
        user.setRole(Role.SELLER);
        
        // Then
        assertThat(user.getId()).isEqualTo("user-123");
        assertThat(user.getName()).isEqualTo("John Doe");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
        assertThat(user.getPassword()).isEqualTo("encrypted-password");
        assertThat(user.getRole()).isEqualTo(Role.SELLER);
    }

    @Test
    void userWithNullValues() {
        // Given
        User user = new User();
        user.setId("user-456");
        
        // When / Then (should not throw)
        user.setName(null);
        user.setEmail(null);
        user.setPassword(null);
        user.setRole(null);
        
        assertThat(user.getId()).isEqualTo("user-456");
        assertThat(user.getName()).isNull();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getPassword()).isNull();
        assertThat(user.getRole()).isNull();
    }

    @Test
    void userClientRole() {
        // Given
        User user = new User();
        
        // When
        user.setRole(Role.CLIENT);
        
        // Then
        assertThat(user.getRole()).isEqualTo(Role.CLIENT);
    }
}
