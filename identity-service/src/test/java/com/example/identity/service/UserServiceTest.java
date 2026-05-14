package com.example.identity.service;

import com.example.identity.model.Role;
import com.example.identity.model.User;
import com.example.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class UserServiceTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private UserService userService;

    @Test
    void register_createsNewUserWithEncryptedPassword() {
        String name = "John Doe";
        String email = "john@example.com";
        String password = "sha256_hash_from_frontend";
        Role role = Role.SELLER;
        String avatar = "avatar.jpg";

        User expectedUser = new User();
        expectedUser.setId("user-123");
        expectedUser.setName(name);
        expectedUser.setEmail(email);
        expectedUser.setRole(role);
        expectedUser.setAvatar(avatar);

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(expectedUser);

        User result = userService.register(name, email, password, role, avatar);

        assertThat(result.getId()).isEqualTo("user-123");
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getRole()).isEqualTo(role);
        assertThat(result.getAvatar()).isEqualTo(avatar);

        verify(kafkaTemplate).send("user-registered", "user-123");
    }

    @Test
    void register_throwsExceptionIfEmailAlreadyExists() {
        String email = "existing@example.com";
        User existingUser = new User();
        existingUser.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() ->
            userService.register("John", email, "password", Role.SELLER, "avatar.jpg")
        )
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticate_returnsUserForValidCredentials() {
        String email = "user@example.com";
        String hashedPassword = "sha256_hash_from_frontend";
        
        User user = new User();
        user.setId("user-123");
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Optional<User> result = userService.authenticate(email, hashedPassword);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("user-123");
        assertThat(result.get().getEmail()).isEqualTo(email);
    }

    @Test
    void authenticate_returnsEmptyForInvalidPassword() {
        String email = "user@example.com";
        
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Optional<User> result = userService.authenticate(email, "wrong_password");

        assertThat(result).isEmpty();
    }

    @Test
    void authenticate_returnsEmptyForNonExistentUser() {
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        Optional<User> result = userService.authenticate(email, "password");

        assertThat(result).isEmpty();
    }

    @Test
    void findByEmail_returnsUserWhenExists() {
        String email = "user@example.com";
        User user = new User();
        user.setId("user-123");
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail(email);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("user-123");
    }

    @Test
    void findByEmail_returnsEmptyWhenNotExists() {
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail(email);

        assertThat(result).isEmpty();
    }

    @Test
    void updateProfile_updatesNameWhenProvided() {
        String email = "user@example.com";
        String newName = "Jane Doe";
        
        User user = new User();
        user.setId("user-123");
        user.setName("John Doe");
        user.setEmail(email);

        User updatedUser = new User();
        updatedUser.setId("user-123");
        updatedUser.setName(newName);
        updatedUser.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        Optional<User> result = userService.updateProfile(email, newName, null);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(newName);
    }

    @Test
    void updateProfile_updatesAvatarWhenProvided() {
        String email = "user@example.com";
        String newAvatar = "new-avatar.jpg";
        
        User user = new User();
        user.setId("user-123");
        user.setEmail(email);
        user.setAvatar("old-avatar.jpg");

        User updatedUser = new User();
        updatedUser.setId("user-123");
        updatedUser.setEmail(email);
        updatedUser.setAvatar(newAvatar);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        Optional<User> result = userService.updateProfile(email, null, newAvatar);

        assertThat(result).isPresent();
        assertThat(result.get().getAvatar()).isEqualTo(newAvatar);
    }

    @Test
    void updateProfile_returnsEmptyWhenUserNotFound() {
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        Optional<User> result = userService.updateProfile(email, "New Name", null);

        assertThat(result).isEmpty();
        verify(userRepository, never()).save(any());
    }
}
