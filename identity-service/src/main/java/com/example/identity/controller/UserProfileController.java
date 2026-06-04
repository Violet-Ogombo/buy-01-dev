package com.example.identity.controller;

import com.example.identity.dto.UserProfileDTO;
import com.example.identity.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable String userId) {
        UserProfileDTO profile = userProfileService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }
}
