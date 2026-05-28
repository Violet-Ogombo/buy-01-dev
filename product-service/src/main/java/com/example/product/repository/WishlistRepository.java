package com.example.product.repository;

import com.example.product.model.Wishlist;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends MongoRepository<Wishlist, String> {
    List<Wishlist> findByUserId(String userId);
    
    Optional<Wishlist> findByUserIdAndProductId(String userId, String productId);
    
    void deleteByUserIdAndProductId(String userId, String productId);
    
    long countByUserId(String userId);
}
