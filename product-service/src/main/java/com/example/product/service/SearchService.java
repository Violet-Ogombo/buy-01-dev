package com.example.product.service;

import com.example.product.dto.ProductSearchDTO;
import com.example.product.model.Product;
import com.example.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class SearchService {

    private final ProductRepository productRepository;

    @Autowired
    public SearchService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductSearchDTO> searchByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllProducts();
        }

        String lowerKeyword = keyword.toLowerCase();
        List<Product> products = productRepository.findAll();

        return products.stream()
                .filter(product -> matchesKeyword(product, lowerKeyword))
                .sorted((p1, p2) -> Long.compare(p2.getSalesCount(), p1.getSalesCount()))
                .map(this::convertToSearchDTO)
                .toList();
    }

    public List<ProductSearchDTO> filterProducts(BigDecimal minPrice, BigDecimal maxPrice) {
        List<Product> products = productRepository.findAll();

        return products.stream()
                .filter(product -> {
                    BigDecimal price = BigDecimal.valueOf(product.getPrice());
                    if (minPrice != null && price.compareTo(minPrice) < 0) {
                        return false;
                    }
                    
                    return (maxPrice != null && price.compareTo(maxPrice) > 0);
                })
                .sorted((p1, p2) -> BigDecimal.valueOf(p1.getPrice()).compareTo(BigDecimal.valueOf(p2.getPrice())))
                .map(this::convertToSearchDTO)
                .toList();
    }

    public List<ProductSearchDTO> filterAndSearch(String keyword, String category, BigDecimal minPrice, BigDecimal maxPrice) {
        String lowerKeyword = keyword != null ? keyword.toLowerCase() : "";
        List<Product> products = productRepository.findAll();

        return products.stream()
                .filter(product -> {
                    if (!lowerKeyword.isEmpty() && !matchesKeyword(product, lowerKeyword)) {
                        return false;
                    }
                    return matchesCategory(product, category) && matchesPriceRange(product, minPrice, maxPrice);
                })
                .sorted((p1, p2) -> Long.compare(p2.getSalesCount(), p1.getSalesCount()))
                .map(this::convertToSearchDTO)
                .toList();
    }

    public List<ProductSearchDTO> getFilteredAndSorted(String category, BigDecimal minPrice, BigDecimal maxPrice, String sortBy) {
        List<Product> products = productRepository.findAll();

        List<ProductSearchDTO> filteredList = products.stream()
                .filter(product -> matchesCategory(product, category) && matchesPriceRange(product, minPrice, maxPrice))
                .map(this::convertToSearchDTO)
                .toList();

        return sortProducts(filteredList, sortBy);
    }

    private boolean matchesCategory(Product product, String category) {
        if (category == null || category.isEmpty() || "all".equalsIgnoreCase(category)) {
            return true;
        }
        return product.getCategory() != null && category.equalsIgnoreCase(product.getCategory());
    }

    private boolean matchesPriceRange(Product product, BigDecimal minPrice, BigDecimal maxPrice) {
        BigDecimal price = BigDecimal.valueOf(product.getPrice());
        if (minPrice != null && price.compareTo(minPrice) < 0) {
            return false;
        }
        if (maxPrice != null && price.compareTo(maxPrice) > 0) {
            return false;
        }
        return true;
    }

    private List<ProductSearchDTO> sortProducts(List<ProductSearchDTO> products, String sortBy) {
        switch (sortBy != null ? sortBy.toLowerCase() : "popularity") {
            case "price_asc":
                return products.stream()
                        .sorted((p1, p2) -> p1.getPrice().compareTo(p2.getPrice()))
                        .toList();
            case "price_desc":
                return products.stream()
                        .sorted((p1, p2) -> p2.getPrice().compareTo(p1.getPrice()))
                        .toList();
            case "name":
                return products.stream()
                        .sorted((p1, p2) -> p1.getName().compareTo(p2.getName()))
                        .toList();
            case "newest":
                return products;
            case "popularity":
            default:
                return products.stream()
                        .sorted((p1, p2) -> Long.compare(p2.getSalesCount(), p1.getSalesCount()))
                        .toList();
        }
    }

    private List<ProductSearchDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::convertToSearchDTO)
                .toList();
    }

    private boolean matchesKeyword(Product product, String keyword) {
        return product.getName().toLowerCase().contains(keyword) ||
               (product.getDescription() != null && product.getDescription().toLowerCase().contains(keyword));
    }

    private ProductSearchDTO convertToSearchDTO(Product product) {
        ProductSearchDTO dto = ProductSearchDTO.from(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                BigDecimal.valueOf(product.getPrice()),
                product.getQuantity()
        );
        dto.setImageUrls(product.getImageUrls());
        dto.setSalesCount(product.getSalesCount());
        dto.setRating(BigDecimal.ZERO);
        return dto;
    }
}
