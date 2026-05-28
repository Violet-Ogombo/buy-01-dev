package com.example.product.service;

import com.example.product.dto.BuyerAnalyticsDTO;
import com.example.product.model.Order;
import com.example.product.model.OrderItem;
import com.example.product.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BuyerAnalyticsService {

    private final OrderRepository orderRepository;

    public BuyerAnalyticsService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public BuyerAnalyticsDTO getBuyerAnalytics(String userId) {
        // Get all user's orders without pagination to aggregate
        Page<Order> ordersPage = orderRepository.findByUserId(userId, Pageable.unpaged());
        List<Order> orders = ordersPage.getContent();

        if (orders.isEmpty()) {
            return new BuyerAnalyticsDTO(userId, 0, BigDecimal.ZERO, List.of());
        }

        // Calculate most bought products
        Map<String, ProductPurchaseStats> productStats = new HashMap<>();
        BigDecimal totalSpent = BigDecimal.ZERO;
        
        for (Order order : orders) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    String productId = item.getProductId();
                    productStats.putIfAbsent(productId, new ProductPurchaseStats());
                    
                    ProductPurchaseStats stats = productStats.get(productId);
                    stats.totalQuantity += item.getQuantity();
                    stats.totalSpent = stats.totalSpent.add(item.getTotalPrice());
                    stats.purchaseCount += 1;
                    
                    totalSpent = totalSpent.add(item.getTotalPrice());
                }
            }
        }

        // Get top 5 most bought products
        List<BuyerAnalyticsDTO.MostBoughtProductDTO> mostBoughtProducts = productStats.entrySet()
                .stream()
                .sorted((a, b) -> Integer.compare(b.getValue().totalQuantity, a.getValue().totalQuantity))
                .limit(5)
                .map(entry -> new BuyerAnalyticsDTO.MostBoughtProductDTO(
                        entry.getKey(),
                        "Product " + entry.getKey().substring(0, Math.min(5, entry.getKey().length())),
                        entry.getValue().totalQuantity,
                        entry.getValue().totalSpent,
                        entry.getValue().purchaseCount
                ))
                .collect(Collectors.toList());

        return new BuyerAnalyticsDTO(userId, orders.size(), totalSpent, mostBoughtProducts);
    }

    private static class ProductPurchaseStats {
        int totalQuantity = 0;
        BigDecimal totalSpent = BigDecimal.ZERO;
        int purchaseCount = 0;
    }
}
