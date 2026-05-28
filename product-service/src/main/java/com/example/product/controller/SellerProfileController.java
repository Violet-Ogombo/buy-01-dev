package com.example.product.controller;

import com.example.product.dto.SellerProfileDTO;
import com.example.product.service.SellerProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sellers")
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;

    public SellerProfileController(SellerProfileService sellerProfileService) {
        this.sellerProfileService = sellerProfileService;
    }

    @GetMapping("/{sellerId}/profile")
    public ResponseEntity<SellerProfileDTO> getSellerProfile(@PathVariable String sellerId) {
        SellerProfileDTO profile = sellerProfileService.getSellerProfile(sellerId);
        return ResponseEntity.ok(profile);
    }
}
