package com.example.identity.service;

import com.example.identity.model.Role;
import com.example.identity.model.User;
import com.example.identity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Objects;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public UserService(UserRepository userRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.userRepository = userRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @SuppressWarnings("null")
    public User register(String name, String email, String password, Role role, String avatar) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        
        // Note: Password received is already SHA-256 hashed from the frontend.
        // We apply BCrypt here on top of the hash for additional security.
        // This ensures plaintext passwords never travel over network.
        
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        // Apply BCrypt to the SHA-256 hash: BCrypt(SHA256(plaintext))
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setAvatar(avatar);
        User savedUser = userRepository.save(user);
        kafkaTemplate.send("user-registered", savedUser.getId());
        return savedUser;
    }

    public Optional<User> authenticate(String email, String password) {
        // Password received is already SHA-256 hashed from the frontend
        // We compare the incoming hash with BCrypt(SHA256(...)) stored in database
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return userOpt;
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @SuppressWarnings("null")
    public Optional<User> updateProfile(String email, String name, String avatar) {
        return userRepository.findByEmail(email).map(user -> {
            if (name != null && !name.isEmpty()) {
                user.setName(name);
            }
            if (avatar != null) {
                user.setAvatar(avatar);
            }
            return Objects.requireNonNull(userRepository.save(user));
        });
    }

    @SuppressWarnings("null")
    public Optional<User> updateProfileWithPassword(String email, String name, String newEmail, 
                                                    String oldPassword, String newPassword) {
        return userRepository.findByEmail(email).flatMap(user -> {
            // Verify old password if provided
            // Note: oldPassword and newPassword are already SHA-256 hashed from the frontend
            if (oldPassword != null && !oldPassword.isEmpty()) {
                if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                    throw new RuntimeException("Old password is incorrect");
                }
                // Update password only if old password is correct
                if (newPassword != null && !newPassword.isEmpty()) {
                    // newPassword is already SHA-256 hashed, apply BCrypt on top
                    user.setPassword(passwordEncoder.encode(newPassword));
                }
            }

            // Update name if provided
            if (name != null && !name.isEmpty()) {
                user.setName(name);
            }

            // Update email if provided and not already taken
            if (newEmail != null && !newEmail.isEmpty() && !newEmail.equals(email)) {
                if (userRepository.findByEmail(newEmail).isPresent()) {
                    throw new RuntimeException("Email already in use");
                }
                user.setEmail(newEmail);
            }

            return Optional.of(Objects.requireNonNull(userRepository.save(user)));
        });
    }

    private boolean isPasswordStrong(String password) {
        // This method is deprecated - password strength validation should happen on frontend
        // before hashing. Backend receives hashed passwords and cannot validate strength.
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};:'\"<>,.?/].*");
        return hasUppercase && hasLowercase && hasDigit && hasSpecialChar;
    }
}
