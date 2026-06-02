package com.example.product.service;

import com.example.product.dto.ProductSearchDTO;
import com.example.product.model.Product;
import com.example.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

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
                .collect(Collectors.toList());
    }

    public List<ProductSearchDTO> filterProducts(BigDecimal minPrice, BigDecimal maxPrice) {
        List<Product> products = productRepository.findAll();

        return products.stream()
                .filter(product -> {
                    BigDecimal price = BigDecimal.valueOf(product.getPrice());
                    if (minPrice != null && price.compareTo(minPrice) < 0) {
                        return false;
                    }
                    if (maxPrice != null && price.compareTo(maxPrice) > 0) {
                        return false;
                    }
                    return true;
                })
                .sorted((p1, p2) -> BigDecimal.valueOf(p1.getPrice()).compareTo(BigDecimal.valueOf(p2.getPrice())))
                .map(this::convertToSearchDTO)
                .collect(Collectors.toList());
    }

    public List<ProductSearchDTO> filterAndSearch(String keyword, String category, BigDecimal minPrice, BigDecimal maxPrice) {
        String lowerKeyword = keyword != null ? keyword.toLowerCase() : "";
        List<Product> products = productRepository.findAll();

        return products.stream()
                .filter(product -> {
                    // Keyword filter
                    if (!lowerKeyword.isEmpty() && !matchesKeyword(product, lowerKeyword)) {
                        return false;
                    }
                    // Category filter
                    if (category != null && !category.isEmpty() && !"all".equalsIgnoreCase(category)) {
                        if (product.getCategory() == null || !category.equalsIgnoreCase(product.getCategory())) {
                            return false;
                        }
                    }
                    // Price range filter
                    BigDecimal price = BigDecimal.valueOf(product.getPrice());
                    if (minPrice != null && price.compareTo(minPrice) < 0) {
                        return false;
                    }
                    if (maxPrice != null && price.compareTo(maxPrice) > 0) {
                        return false;
                    }
                    return true;
                })
                .sorted((p1, p2) -> Long.compare(p2.getSalesCount(), p1.getSalesCount()))
                .map(this::convertToSearchDTO)
                .collect(Collectors.toList());
    }

    public List<ProductSearchDTO> getFilteredAndSorted(String category, BigDecimal minPrice, BigDecimal maxPrice, String sortBy) {
        List<Product> products = productRepository.findAll();

        List<ProductSearchDTO> filteredList = products.stream()
                .filter(product -> {
                    // Category filter
                    if (category != null && !category.isEmpty() && !"all".equalsIgnoreCase(category)) {
                        if (product.getCategory() == null || !category.equalsIgnoreCase(product.getCategory())) {
                            return false;
                        }
                    }
                    // Price range filter
                    BigDecimal price = BigDecimal.valueOf(product.getPrice());
                    if (minPrice != null && price.compareTo(minPrice) < 0) {
                        return false;
                    }
                    if (maxPrice != null && price.compareTo(maxPrice) > 0) {
                        return false;
                    }
                    return true;
                })
                .map(this::convertToSearchDTO)
                .collect(Collectors.toList());

        switch (sortBy != null ? sortBy.toLowerCase() : "popularity") {
            case "price_asc":
                return filteredList.stream()
                        .sorted((p1, p2) -> p1.getPrice().compareTo(p2.getPrice()))
                        .collect(Collectors.toList());
            case "price_desc":
                return filteredList.stream()
                        .sorted((p1, p2) -> p2.getPrice().compareTo(p1.getPrice()))
                        .collect(Collectors.toList());
            case "name":
                return filteredList.stream()
                        .sorted((p1, p2) -> p1.getName().compareTo(p2.getName()))
                        .collect(Collectors.toList());
            case "newest":
                return filteredList;
            case "popularity":
            default:
                return filteredList.stream()
                        .sorted((p1, p2) -> Long.compare(p2.getSalesCount(), p1.getSalesCount()))
                        .collect(Collectors.toList());
        }
    }

    private List<ProductSearchDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::convertToSearchDTO)
                .collect(Collectors.toList());
    }

    private boolean matchesKeyword(Product product, String keyword) {
        return product.getName().toLowerCase().contains(keyword) ||
               (product.getDescription() != null && product.getDescription().toLowerCase().contains(keyword));
    }

    private ProductSearchDTO convertToSearchDTO(Product product) {
        return new ProductSearchDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                BigDecimal.valueOf(product.getPrice()),
                product.getQuantity(),
                product.getImageUrls(),
                product.getSalesCount(),
                BigDecimal.ZERO
        );
    }
}
