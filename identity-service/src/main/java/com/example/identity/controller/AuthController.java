package com.example.identity.controller;

import com.example.identity.model.Role;
import com.example.identity.model.User;
import com.example.identity.service.JwtService;
import com.example.identity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/")
public class AuthController {
    private final UserService userService;
    private final JwtService jwtService;
    
    private static final String EMAIL_KEY = "email";
    private static final String AVATAR_KEY = "avatar";
    private static final String ERROR_KEY = "error";

    @Autowired
    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody Map<String, String> req) {
        try {
            String name = req.get("name");
            String email = req.get(EMAIL_KEY);
            String password = req.get("password");
            Role role = Role.valueOf(req.get("role"));
            String avatar = req.get("avatar");
            User user = userService.register(name, email, password, role, avatar);
            return ResponseEntity.ok(Map.of(
                "id", user.getId(), 
                "name", user.getName(),
                EMAIL_KEY, user.getEmail(), 
                "role", user.getRole().name(),
                "message", "Registration successful. Please login."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body((Object) Map.of(ERROR_KEY, "Invalid role or missing fields"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body((Object) Map.of(ERROR_KEY, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body((Object) Map.of(ERROR_KEY, "Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Map<String, String> req) {
        try {
            String email = req.get(EMAIL_KEY);
            String password = req.get("password");
            return userService.authenticate(email, password)
                    .map(user -> {
                        String token = jwtService.generateToken(user);
                        return ResponseEntity.ok((Object) Map.of(
                            "id", user.getId(),
                            "name", user.getName(),
                            EMAIL_KEY, user.getEmail(),
                            "role", user.getRole().name(),
                            "token", token
                        ));
                    })
                    .orElse(ResponseEntity.status(401).body((Object) Map.of(ERROR_KEY, "Invalid credentials")));
        } catch (Exception e) {
            return ResponseEntity.status(500).body((Object) Map.of(ERROR_KEY, "Login failed: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> getProfile(Authentication auth) {
        String email = auth.getName();
        return userService.findByEmail(email)
            .map(user -> ResponseEntity.ok((Object) Map.of(
                "id", user.getId(),
                "name", user.getName(),
                EMAIL_KEY, user.getEmail(),
                "role", user.getRole().name(),
                AVATAR_KEY, user.getAvatar() != null ? user.getAvatar() : ""
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> updateProfile(@RequestBody Map<String, String> req, Authentication auth) {
        try {
            String email = auth.getName();
            String name = req.get("name");
            String newEmail = req.get(EMAIL_KEY);
            String oldPassword = req.get("oldPassword");
            String newPassword = req.get("newPassword");

            return userService.updateProfileWithPassword(email, name, newEmail, oldPassword, newPassword)
                .map(user -> ResponseEntity.ok((Object) Map.of(
                    "id", user.getId(),
                    "name", user.getName(),
                    EMAIL_KEY, user.getEmail(),
                    "role", user.getRole().name(),
                    AVATAR_KEY, user.getAvatar() != null ? user.getAvatar() : ""
                )))
                .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body((Object) Map.of(ERROR_KEY, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body((Object) Map.of(ERROR_KEY, "Update failed: " + e.getMessage()));
        }
    }
}
