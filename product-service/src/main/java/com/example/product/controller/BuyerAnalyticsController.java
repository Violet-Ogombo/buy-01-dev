package com.example.product.controller;

import com.example.product.dto.BuyerAnalyticsDTO;
import com.example.product.service.BuyerAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/buyers")
public class BuyerAnalyticsController {

    private final BuyerAnalyticsService buyerAnalyticsService;

    public BuyerAnalyticsController(BuyerAnalyticsService buyerAnalyticsService) {
        this.buyerAnalyticsService = buyerAnalyticsService;
    }

    @GetMapping("/{userId}/analytics")
    public ResponseEntity<BuyerAnalyticsDTO> getBuyerAnalytics(@PathVariable String userId) {
        BuyerAnalyticsDTO analytics = buyerAnalyticsService.getBuyerAnalytics(userId);
        return ResponseEntity.ok(analytics);
    }
}
