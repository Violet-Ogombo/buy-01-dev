package com.example.product.service;

import com.example.product.dto.WishlistItemDTO;
import com.example.product.exception.ResourceNotFoundException;
import com.example.product.model.Product;
import com.example.product.model.Wishlist;
import com.example.product.repository.WishlistRepository;
import com.example.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    @Autowired
    public WishlistService(WishlistRepository wishlistRepository, ProductRepository productRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
    }

    public List<WishlistItemDTO> getUserWishlist(String userId) {
        List<Wishlist> wishlistItems = wishlistRepository.findByUserId(userId);
        return wishlistItems.stream()
                .map(item -> {
                    Product product = productRepository.findById(item.getProductId())
                            .orElse(null);
                    if (product != null) {
                        String imageUrl = product.getImageUrls() != null && !product.getImageUrls().isEmpty()
                                ? product.getImageUrls().get(0)
                                : null;
                        return new WishlistItemDTO(item.getId(), item.getProductId(), product.getName(),
                                BigDecimal.valueOf(product.getPrice()), imageUrl, item.getAddedAt());
                    }
                    return null;
                })
                .filter(item -> item != null)
                .toList();
    }

    public WishlistItemDTO addToWishlist(String userId, String productId) {
        // Check if product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // Check if already in wishlist
        if (wishlistRepository.findByUserIdAndProductId(userId, productId).isPresent()) {
            throw new IllegalArgumentException("Product already in wishlist");
        }

        Wishlist wishlistItem = new Wishlist();
        wishlistItem.setUserId(userId);
        wishlistItem.setProductId(productId);
        wishlistItem.setAddedAt(LocalDateTime.now());

        Wishlist saved = wishlistRepository.save(wishlistItem);

        String imageUrl = product.getImageUrls() != null && !product.getImageUrls().isEmpty()
                ? product.getImageUrls().get(0)
                : null;

        return new WishlistItemDTO(saved.getId(), saved.getProductId(), product.getName(),
                BigDecimal.valueOf(product.getPrice()), imageUrl, saved.getAddedAt());
    }

    public void removeFromWishlist(String userId, String productId) {
        Wishlist item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in wishlist"));
        wishlistRepository.delete(item);
    }

    public long getWishlistCount(String userId) {
        return wishlistRepository.countByUserId(userId);
    }
}
