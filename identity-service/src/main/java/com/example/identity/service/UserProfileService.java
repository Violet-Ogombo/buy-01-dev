package com.example.identity.service;

import com.example.identity.dto.UserProfileDTO;
import com.example.identity.model.User;
import com.example.identity.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileDTO getUserProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Return basic user profile
        // Most bought products are fetched from product-service via separate endpoint
        return new UserProfileDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getTotalSpent(),
                user.getSellerRevenue(),
                Collections.emptyList(),  // Buyer analytics come from product-service
                0  // Total orders come from product-service
        );
    }
}
