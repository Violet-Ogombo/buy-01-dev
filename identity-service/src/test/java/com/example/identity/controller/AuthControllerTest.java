package com.example.identity.controller;

import com.example.identity.model.Role;
import com.example.identity.model.User;
import com.example.identity.service.JwtService;
import com.example.identity.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-123");
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setRole(Role.SELLER);
    }

    @Test
    void register_successfullyCreatesNewUser() {
        // Arrange
        Map<String, String> request = Map.of(
            "name", "John Doe",
            "email", "john@example.com",
            "password", "hashed_password",
            "role", "SELLER",
            "avatar", "avatar.jpg"
        );

        User registeredUser = new User();
        registeredUser.setId("user-456");
        registeredUser.setName("John Doe");
        registeredUser.setEmail("john@example.com");
        registeredUser.setRole(Role.SELLER);

        when(userService.register("John Doe", "john@example.com", "hashed_password", Role.SELLER, "avatar.jpg"))
            .thenReturn(registeredUser);

        // Act
        ResponseEntity<Map<String, Object>> response = authController.register(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKeys("id", "email", "name", "role");
        assertThat(response.getBody().get("id")).isEqualTo("user-456");

        verify(userService).register("John Doe", "john@example.com", "hashed_password", Role.SELLER, "avatar.jpg");
    }

    @Test
    void register_returnsErrorWhenEmailExists() {
        // Arrange
        Map<String, String> request = Map.of(
            "name", "Duplicate",
            "email", "existing@example.com",
            "password", "hashed_password",
            "role", "SELLER",
            "avatar", "avatar.jpg"
        );

        when(userService.register(any(), eq("existing@example.com"), any(), any(), any()))
            .thenThrow(new RuntimeException("Email already exists"));

        // Act & Assert
        try {
            authController.register(request);
            throw new AssertionError("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Email already exists");
        }
    }

    @Test
    void login_successfullyAuthenticatesAndReturnsToken() {
        // Arrange
        Map<String, String> request = Map.of(
            "email", "test@example.com",
            "password", "correct_password"
        );

        when(userService.authenticate("test@example.com", "correct_password"))
            .thenReturn(Optional.of(testUser));

        String token = "jwt.token.here";
        when(jwtService.generateToken(testUser.getId(), testUser.getRole().name()))
            .thenReturn(token);

        // Act
        ResponseEntity<Map<String, Object>> response = authController.login(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys("token", "user");
        assertThat(response.getBody().get("token")).isEqualTo(token);

        verify(userService).authenticate("test@example.com", "correct_password");
        verify(jwtService).generateToken(testUser.getId(), "SELLER");
    }

    @Test
    void login_returnsUnauthorizedForInvalidCredentials() {
        // Arrange
        Map<String, String> request = Map.of(
            "email", "test@example.com",
            "password", "wrong_password"
        );

        when(userService.authenticate("test@example.com", "wrong_password"))
            .thenReturn(Optional.empty());

        // Act & Assert
        try {
            authController.login(request);
            throw new AssertionError("Expected exception for invalid credentials");
        } catch (Exception e) {
            // Expected behavior - should return unauthorized
            assertThat(e).isNotNull();
        }
    }

    @Test
    void login_returnsUnauthorizedForNonExistentUser() {
        // Arrange
        Map<String, String> request = Map.of(
            "email", "nonexistent@example.com",
            "password", "any_password"
        );

        when(userService.authenticate("nonexistent@example.com", "any_password"))
            .thenReturn(Optional.empty());

        // Act & Assert
        try {
            authController.login(request);
            throw new AssertionError("Expected exception");
        } catch (Exception e) {
            // Expected
            assertThat(e).isNotNull();
        }
    }

    @Test
    void updateProfile_successfullyUpdatesUserProfile() {
        // Arrange
        Map<String, String> request = Map.of(
            "email", "test@example.com",
            "name", "Updated Name",
            "avatar", "new-avatar.jpg"
        );

        User updatedUser = new User();
        updatedUser.setId("user-123");
        updatedUser.setEmail("test@example.com");
        updatedUser.setName("Updated Name");
        updatedUser.setAvatar("new-avatar.jpg");
        updatedUser.setRole(Role.SELLER);

        when(userService.updateProfile("test@example.com", "Updated Name", "new-avatar.jpg"))
            .thenReturn(Optional.of(updatedUser));

        // Act
        ResponseEntity<Map<String, Object>> response = authController.updateProfile(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys("id", "email", "name", "avatar");
        assertThat(response.getBody().get("name")).isEqualTo("Updated Name");

        verify(userService).updateProfile("test@example.com", "Updated Name", "new-avatar.jpg");
    }

    @Test
    void updateProfile_returnsNotFoundForNonExistentUser() {
        // Arrange
        Map<String, String> request = Map.of(
            "email", "nonexistent@example.com",
            "name", "New Name",
            "avatar", "avatar.jpg"
        );

        when(userService.updateProfile("nonexistent@example.com", "New Name", "avatar.jpg"))
            .thenReturn(Optional.empty());

        // Act & Assert
        try {
            authController.updateProfile(request);
            throw new AssertionError("Expected exception");
        } catch (Exception e) {
            // Expected - user not found
            assertThat(e).isNotNull();
        }
    }

    @Test
    void refreshToken_generatesNewTokenForValidUser() {
        // Arrange
        Map<String, String> request = Map.of(
            "userId", "user-123",
            "role", "SELLER"
        );

        String newToken = "new.jwt.token.here";
        when(jwtService.generateToken("user-123", "SELLER"))
            .thenReturn(newToken);

        // Act
        ResponseEntity<Map<String, String>> response = authController.refreshToken(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("token", newToken);

        verify(jwtService).generateToken("user-123", "SELLER");
    }
}
