package com.example.product.controller;

import com.example.product.dto.ProductSearchDTO;
import com.example.product.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class SearchController {

    private final SearchService searchService;

    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<ProductSearchDTO>> searchProducts(
            @RequestParam(required = false) String keyword) {
        List<ProductSearchDTO> results = searchService.searchByKeyword(keyword);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/filter")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<ProductSearchDTO>> filterProducts(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        List<ProductSearchDTO> results = searchService.filterProducts(minPrice, maxPrice);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search-and-filter")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<ProductSearchDTO>> searchAndFilter(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        List<ProductSearchDTO> results = searchService.filterAndSearch(keyword, category, minPrice, maxPrice);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/filter-and-sort")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<ProductSearchDTO>> filterAndSort(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "popularity") String sortBy) {
        List<ProductSearchDTO> results = searchService.getFilteredAndSorted(category, minPrice, maxPrice, sortBy);
        return ResponseEntity.ok(results);
    }
}
