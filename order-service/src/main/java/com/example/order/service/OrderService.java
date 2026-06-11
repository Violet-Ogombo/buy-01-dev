package com.example.order.service;

import com.example.order.dto.OrderDTO;
import com.example.order.dto.OrderItemDTO;
import com.example.order.dto.ProductDTO;
import com.example.order.exception.ResourceNotFoundException;
import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final String ORDER_NOT_FOUND_PREFIX = "Order not found with id: ";

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    @Value("${product.service.url:http://localhost:8082}")
    private String productServiceUrl;

    @Autowired
    public OrderService(OrderRepository orderRepository, RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
    }

    private String getProductName(String productId) {
        try {
            ProductDTO p = restTemplate.getForObject(productServiceUrl + "/" + productId, ProductDTO.class);
            return p != null ? p.getName() : "Unknown Product";
        } catch (Exception e) {
            log.warn("Could not retrieve product name for ID {}: {}", productId, e.getMessage());
            return "Unknown Product";
        }
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
                    String productName = getProductName(item.getProductId());
                    return new OrderItemDTO(item.getProductId(), productName,
                            item.getQuantity(), item.getUnitPrice(), item.getTotalPrice());
                })
                .toList();

        return new OrderDTO(order.getId(), order.getOrderNumber(), order.getUserId(), itemDTOs,
                order.getTotalAmount(), order.getStatus(), order.getPaymentMethod(),
                order.getShippingAddress(), order.getTrackingNumber(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
