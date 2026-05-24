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
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

        when(userService.register("John Doe", "john@example.com", "hashed_password", Role.SELLER))
            .thenReturn(registeredUser);

        // Act
        ResponseEntity<?> response = authController.register(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKeys("id", "email", "name", "role");
        assertThat(body.get("id")).isEqualTo("user-456");

        verify(userService).register("John Doe", "john@example.com", "hashed_password", Role.SELLER);
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

        when(userService.register(any(), any(), any(), any()))
            .thenThrow(new RuntimeException("Email already exists"));

        // Act
        ResponseEntity<?> response = authController.register(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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
        when(jwtService.generateToken(testUser))
            .thenReturn(token);

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKeys("token", "id", "email", "name", "role");
        assertThat(body.get("token")).isEqualTo(token);

        verify(userService).authenticate("test@example.com", "correct_password");
        verify(jwtService).generateToken(testUser);
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

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateProfile_successfullyUpdatesUserProfile() {
        // Arrange
        Map<String, String> request = Map.of(
            "name", "Updated Name",
            "email", "test@example.com",
            "oldPassword", "old",
            "newPassword", "new"
        );

        User updatedUser = new User();
        updatedUser.setId("user-123");
        updatedUser.setEmail("test@example.com");
        updatedUser.setName("Updated Name");
        updatedUser.setRole(Role.SELLER);

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@example.com");
        
        when(userService.updateProfileWithPassword("test@example.com", "Updated Name", "test@example.com", "old", "new"))
            .thenReturn(Optional.of(updatedUser));

        // Act
        ResponseEntity<?> response = authController.updateProfile(request, auth);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKeys("id", "email", "name");
        assertThat(body.get("name")).isEqualTo("Updated Name");

        verify(userService).updateProfileWithPassword("test@example.com", "Updated Name", "test@example.com", "old", "new");
    }

    @Test
    void updateProfile_returnsNotFoundForNonExistentUser() {
        // Arrange
        Map<String, String> request = Map.of(
            "name", "New Name",
            "email", "nonexistent@example.com",
            "oldPassword", "old",
            "newPassword", "new"
        );

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("nonexistent@example.com");

        when(userService.updateProfileWithPassword(any(), any(), any(), any(), any()))
            .thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = authController.updateProfile(request, auth);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getProfile_returnsAuthenticatedUserProfile() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@example.com");
        
        when(userService.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<?> response = authController.getProfile(auth);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKeys("id", "email", "name", "role");
        assertThat(body.get("email")).isEqualTo("test@example.com");

        verify(userService).findByEmail("test@example.com");
    }
}
