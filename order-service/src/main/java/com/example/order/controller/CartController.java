package com.example.order.controller;

import com.example.order.dto.CartDTO;
import com.example.order.dto.CheckoutRequest;
import com.example.order.dto.OrderDTO;
import com.example.order.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartDTO> getCart(@RequestHeader("X-User-Id") String userId) {
        CartDTO cart = cartService.getCart(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartDTO> addToCart(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String productId,
            @RequestParam(defaultValue = "1") int quantity) {
        CartDTO cart = cartService.addToCart(userId, productId, quantity);
        return ResponseEntity.ok(cart);
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartDTO> updateCartItem(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String itemId,
            @RequestParam int quantity) {
        CartDTO cart = cartService.updateCartItem(userId, itemId, quantity);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartDTO> removeCartItem(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String itemId) {
        CartDTO cart = cartService.removeCartItem(userId, itemId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartDTO> clearCart(@RequestHeader("X-User-Id") String userId) {
        CartDTO cart = cartService.clearCart(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDTO> checkout(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CheckoutRequest request) {
        OrderDTO order = cartService.checkoutCart(userId, request);
        return ResponseEntity.status(201).body(order);
    }
}
