package com.example.product.controller;

import com.example.product.dto.WishlistItemDTO;
import com.example.product.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    @Autowired
    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WishlistItemDTO>> getUserWishlist(@RequestHeader("X-User-Id") String userId) {
        List<WishlistItemDTO> wishlist = wishlistService.getUserWishlist(userId);
        return ResponseEntity.ok(wishlist);
    }

    @PostMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WishlistItemDTO> addToWishlist(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String productId) {
        WishlistItemDTO item = wishlistService.addToWishlist(userId, productId);
        return ResponseEntity.status(201).body(item);
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeFromWishlist(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String productId) {
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> getWishlistCount(@RequestHeader("X-User-Id") String userId) {
        long count = wishlistService.getWishlistCount(userId);
        return ResponseEntity.ok(count);
    }
}
