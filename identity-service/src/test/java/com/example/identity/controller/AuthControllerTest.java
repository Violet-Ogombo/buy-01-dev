package com.example.identity.controller;

import com.example.identity.model.Role;
import com.example.identity.model.User;
import com.example.identity.service.JwtService;
import com.example.identity.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private MockMvc mockMvc;
    private FakeUserService fakeUserService;
    private FakeJwtService fakeJwtService;

    @BeforeEach
    void setUp() {
        fakeUserService = new FakeUserService();
        fakeJwtService = new FakeJwtService();
        AuthController controller = new AuthController(fakeUserService, fakeJwtService);
        
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    // === REGISTER TESTS ===

    @Test
    void register_successReturnsUserDataAndMessage() throws Exception {
        // Given
        String requestBody = """
                {
                    "name": "John Doe",
                    "email": "john@example.com",
                    "password": "password",
                    "role": "SELLER"
                }
                """;

        // When / Then
        mockMvc.perform(post("/register")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("SELLER"))
                .andExpect(jsonPath("$.message").value("Registration successful. Please login."));
    }

    @Test
    void register_returnsBadRequest_whenEmailAlreadyExists() throws Exception {
        // Given - Register first user
        fakeUserService.register("First User", "duplicate@example.com", "pass1", Role.SELLER);

        String requestBody = """
                {
                    "name": "Jane Doe",
                    "email": "duplicate@example.com",
                    "password": "password",
                    "role": "CLIENT"
                }
                """;

        // When / Then
        mockMvc.perform(post("/register")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already exists"));
    }

    @Test
    void register_returnsBadRequest_whenMissingRequiredFields() throws Exception {
        // Given
        String requestBody = """
                {
                    "name": "John Doe"
                }
                """;

        // When / Then
        mockMvc.perform(post("/register")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void register_returnsBadRequest_whenInvalidRole() throws Exception {
        // Given
        String requestBody = """
                {
                    "name": "John Doe",
                    "email": "john@example.com",
                    "password": "password",
                    "role": "INVALID_ROLE"
                }
                """;

        // When / Then
        mockMvc.perform(post("/register")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid role or missing fields"));
    }

    // === LOGIN TESTS ===

    @Test
    void login_successReturnsTokenAndUserData() throws Exception {
        // Given - Register user first
        fakeUserService.register("John Doe", "john@example.com", "password", Role.SELLER);

        String requestBody = """
                {
                    "email": "john@example.com",
                    "password": "password"
                }
                """;

        // When / Then
        mockMvc.perform(post("/login")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("SELLER"))
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void login_returns401_whenCredentialsInvalid() throws Exception {
        // Given
        String requestBody = """
                {
                    "email": "wrong@example.com",
                    "password": "wrongpassword"
                }
                """;

        // When / Then
        mockMvc.perform(post("/login")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void login_returns401_whenUserNotFound() throws Exception {
        // Given
        String requestBody = """
                {
                    "email": "notfound@example.com",
                    "password": "password"
                }
                """;

        // When / Then
        mockMvc.perform(post("/login")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void login_returns401_whenPasswordWrong() throws Exception {
        // Given
        fakeUserService.register("John Doe", "john@example.com", "correctpass", Role.SELLER);

        String requestBody = """
                {
                    "email": "john@example.com",
                    "password": "wrongpass"
                }
                """;

        // When / Then
        mockMvc.perform(post("/login")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    // === GET PROFILE TESTS ===

    @Test
    void getProfile_returnsUserDataWhenFound() throws Exception {
        // Given
        User user = fakeUserService.register("John Doe", "john@example.com", "password", Role.SELLER);
        Authentication auth = createMockAuthentication("john@example.com");

        // When / Then
        mockMvc.perform(get("/me")
                .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("SELLER"));
    }

    @Test
    void getProfile_returns404_whenUserNotFound() throws Exception {
        // Given
        Authentication auth = createMockAuthentication("notfound@example.com");

        // When / Then
        mockMvc.perform(get("/me")
                .principal(auth))
                .andExpect(status().isNotFound());
    }

    // === UPDATE PROFILE TESTS ===

    @Test
    void updateProfile_successfullyUpdatesProfile() throws Exception {
        // Given
        fakeUserService.register("John Doe", "john@example.com", "oldpass", Role.SELLER);
        Authentication auth = createMockAuthentication("john@example.com");

        String requestBody = """
                {
                    "name": "Updated Name",
                    "email": "newemail@example.com",
                    "oldPassword": "oldpass",
                    "newPassword": "newpass"
                }
                """;

        // When / Then
        mockMvc.perform(put("/me")
                .contentType(APPLICATION_JSON)
                .content(requestBody)
                .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("newemail@example.com"));
    }

    @Test
    void updateProfile_returns400_onInvalidPassword() throws Exception {
        // Given
        fakeUserService.register("John Doe", "john@example.com", "correctpass", Role.SELLER);
        Authentication auth = createMockAuthentication("john@example.com");

        String requestBody = """
                {
                    "name": "Updated Name",
                    "email": "newemail@example.com",
                    "oldPassword": "wrongpass",
                    "newPassword": "newpass"
                }
                """;

        // When / Then
        mockMvc.perform(put("/me")
                .contentType(APPLICATION_JSON)
                .content(requestBody)
                .principal(auth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid old password"));
    }

    @Test
    void updateProfile_returns404_whenUserNotFound() throws Exception {
        // Given
        Authentication auth = createMockAuthentication("notfound@example.com");

        String requestBody = """
                {
                    "name": "Updated Name",
                    "email": "newemail@example.com",
                    "oldPassword": "oldpass",
                    "newPassword": "newpass"
                }
                """;

        // When / Then
        mockMvc.perform(put("/me")
                .contentType(APPLICATION_JSON)
                .content(requestBody)
                .principal(auth))
                .andExpect(status().isNotFound());
    }

    // === HELPER METHODS ===

    private Authentication createMockAuthentication(String email) {
        Authentication auth = new org.springframework.security.core.Authentication() {
            @Override
            public String getName() {
                return email;
            }

            @Override
            public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
                return java.util.Collections.emptyList();
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return email;
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            }
        };
        return auth;
    }

    // === FAKE SERVICE IMPLEMENTATIONS ===

    private static class FakeUserService extends UserService {
        private java.util.Map<String, User> users = new java.util.HashMap<>();
        private java.security.MessageDigest digest;

        public FakeUserService() {
            super(null, null);
            try {
                digest = java.security.MessageDigest.getInstance("SHA-256");
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public User register(String name, String email, String password, Role role) {
            if (users.containsKey(email)) {
                throw new com.example.identity.exception.DuplicateEmailException("Email already exists");
            }
            User user = new User();
            user.setId("user-" + System.nanoTime());
            user.setName(name);
            user.setEmail(email);
            user.setPassword(hashPassword(password));
            user.setRole(role);
            users.put(email, user);
            return user;
        }

        @Override
        public Optional<User> authenticate(String email, String password) {
            User user = users.get(email);
            if (user != null && user.getPassword().equals(hashPassword(password))) {
                return Optional.of(user);
            }
            return Optional.empty();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return Optional.ofNullable(users.get(email));
        }

        @Override
        public Optional<User> updateProfile(String email, String name) {
            User user = users.get(email);
            if (user != null && (name != null && !name.isBlank())) {
                user.setName(name);
            }
            return Optional.ofNullable(user);
        }

        @Override
        public Optional<User> updateProfileWithPassword(String email, String name, String newEmail, String oldPassword, String newPassword) {
            User user = users.get(email);
            if (user == null) {
                return Optional.empty();
            }
            
            if (!user.getPassword().equals(hashPassword(oldPassword))) {
                throw new RuntimeException("Invalid old password");
            }
            
            if (newEmail != null && !newEmail.equals(email) && users.containsKey(newEmail)) {
                throw new com.example.identity.exception.DuplicateEmailException("Email already exists");
            }
            
            if (name != null && !name.isBlank()) {
                user.setName(name);
            }
            if (newEmail != null && !newEmail.isBlank()) {
                users.remove(email);
                user.setEmail(newEmail);
                users.put(newEmail, user);
            }
            if (newPassword != null && !newPassword.isBlank()) {
                user.setPassword(hashPassword(newPassword));
            }
            return Optional.of(user);
        }

        private String hashPassword(String password) {
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        }
    }

    private static class FakeJwtService extends JwtService {
        public FakeJwtService() {
            super("test-secret-key-32-chars-minimum!");
        }

        @Override
        public String generateToken(User user) {
            return "jwt-token-" + user.getId();
        }
    }
}

