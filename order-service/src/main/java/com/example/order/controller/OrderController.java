package com.example.order.controller;

import com.example.order.dto.OrderDTO;
import com.example.order.model.OrderStatus;
import com.example.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable String orderId) {
        OrderDTO order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OrderDTO>> getUserOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDTO> orders = orderService.getUserOrders(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OrderDTO>> getUserOrdersByStatus(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDTO> orders = orderService.getUserOrdersByStatus(userId, orderStatus, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OrderDTO>> searchUserOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String orderNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDTO> orders = orderService.searchUserOrders(userId, orderNumber, pageable);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
        OrderDTO order = orderService.updateOrderStatus(orderId, newStatus);
        return ResponseEntity.ok(order);
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable String orderId,
            @RequestHeader("X-User-Id") String userId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/redo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDTO> redoOrder(@PathVariable String orderId) {
        OrderDTO order = orderService.redoOrder(orderId);
        return ResponseEntity.status(201).body(order);
    }
}
