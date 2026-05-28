package com.example.product.repository;

import com.example.product.model.SellerProduct;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerProductRepository extends MongoRepository<SellerProduct, String> {
    List<SellerProduct> findBySellerId(String sellerId);
    
    Optional<SellerProduct> findBySellerIdAndProductId(String sellerId, String productId);
    
    List<SellerProduct> findByProductId(String productId);
}
