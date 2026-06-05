package com.example.product.service;

import com.example.product.dto.SellerProfileDTO;
import com.example.product.model.Product;
import com.example.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SellerProfileService {
    
    private final ProductRepository productRepository;

    public SellerProfileService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public SellerProfileDTO getSellerProfile(String sellerId) {
        // Get all products by seller
        List<Product> sellerProducts = productRepository.findByUserId(sellerId);

        if (sellerProducts.isEmpty()) {
            return new SellerProfileDTO(sellerId, 0, 0, BigDecimal.ZERO, List.of());
        }

        // Calculate totals
        int totalProducts = sellerProducts.size();
        long totalSales = sellerProducts.stream().mapToLong(Product::getSalesCount).sum();
        BigDecimal totalRevenue = sellerProducts.stream()
                .map(Product::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get top 5 best-selling products sorted by sales count
        List<SellerProfileDTO.BestSellingProductDTO> bestSellingProducts = sellerProducts.stream()
                .sorted((a, b) -> Integer.compare(b.getSalesCount(), a.getSalesCount()))
                .limit(5)
                .map(product -> new SellerProfileDTO.BestSellingProductDTO(
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        product.getSalesCount(),
                        product.getRevenue()
                ))
                .toList();

        return new SellerProfileDTO(sellerId, totalProducts, totalSales, totalRevenue, bestSellingProducts);
    }
}
