package com.example.product.service;

import com.example.product.dto.OrderDTO;
import com.example.product.dto.OrderItemDTO;
import com.example.product.exception.ResourceNotFoundException;
import com.example.product.model.Product;
import com.example.product.model.Order;
import com.example.product.model.OrderStatus;
import com.example.product.repository.OrderRepository;
import com.example.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final String ORDER_NOT_FOUND_PREFIX = "Order not found with id: ";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public OrderDTO getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_PREFIX + orderId));
        return convertToDTO(order);
    }

    public Page<OrderDTO> getUserOrders(String userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        return orders.map(this::convertToDTO);
    }

    public Page<OrderDTO> getUserOrdersByStatus(String userId, OrderStatus status, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserIdAndStatus(userId, status, pageable);
        return orders.map(this::convertToDTO);
    }

    public Page<OrderDTO> searchUserOrders(String userId, String orderNumber, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserIdAndOrderNumberContaining(userId, orderNumber, pageable);
        return orders.map(this::convertToDTO);
    }

    public OrderDTO updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_PREFIX + orderId));

        validateStatusTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        Order updated = orderRepository.save(order);
        return convertToDTO(updated);
    }

    public void cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_PREFIX + orderId));

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot cancel order with status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    public OrderDTO redoOrder(String orderId) {
        Order originalOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_PREFIX + orderId));

        // Create new order with same items
        Order newOrder = new Order();
        newOrder.setOrderNumber("ORD-" + System.currentTimeMillis());
        newOrder.setUserId(originalOrder.getUserId());
        newOrder.setItems(originalOrder.getItems());
        newOrder.setTotalAmount(originalOrder.getTotalAmount());
        newOrder.setStatus(OrderStatus.PENDING);
        newOrder.setPaymentMethod(originalOrder.getPaymentMethod());
        newOrder.setShippingAddress(originalOrder.getShippingAddress());
        newOrder.setCreatedAt(LocalDateTime.now());
        newOrder.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(newOrder);
        return convertToDTO(savedOrder);
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Allowed transitions
        if (currentStatus == OrderStatus.PENDING && 
            (newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.CANCELLED)) {
            return;
        }
        if (currentStatus == OrderStatus.PROCESSING && 
            (newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED)) {
            return;
        }
        if (currentStatus == OrderStatus.SHIPPED && newStatus == OrderStatus.DELIVERED) {
            return;
        }
        throw new IllegalArgumentException("Invalid status transition from " + currentStatus + " to " + newStatus);
    }

    private OrderDTO convertToDTO(Order order) {
        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(item -> {
                    String productName = productRepository.findById(item.getProductId())
                            .map(Product::getName)
                            .orElse("Unknown Product");
                    return new OrderItemDTO(item.getProductId(), productName,
                            item.getQuantity(), item.getUnitPrice(), item.getTotalPrice());
                })
                .collect(Collectors.toList());

        return new OrderDTO(order.getId(), order.getOrderNumber(), order.getUserId(), itemDTOs,
                order.getTotalAmount(), order.getStatus(), order.getPaymentMethod(),
                order.getShippingAddress(), order.getTrackingNumber(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
