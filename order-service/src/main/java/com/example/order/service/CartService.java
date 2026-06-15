package com.example.order.service;

import com.example.order.dto.CartDTO;
import com.example.order.dto.CartItemDTO;
import com.example.order.dto.CheckoutRequest;
import com.example.order.dto.OrderDTO;
import com.example.order.dto.OrderItemDTO;
import com.example.order.dto.ProductDTO;
import com.example.order.dto.ReduceStockItem;
import com.example.order.exception.ResourceNotFoundException;
import com.example.order.exception.ServiceUnavailableException;
import com.example.order.model.*;
import com.example.order.repository.ShoppingCartRepository;
import com.example.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    private static final String CART_NOT_FOUND_PREFIX = "Cart not found for user: ";

    private final ShoppingCartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    @Value("${product.service.url:http://localhost:8082}")
    private String productServiceUrl;

    @Autowired
    public CartService(ShoppingCartRepository cartRepository,
                       OrderRepository orderRepository,
                       RestTemplate restTemplate) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
    }

    private ProductDTO fetchProduct(String productId) {
        try {
            ProductDTO product = restTemplate.getForObject(productServiceUrl + "/" + productId, ProductDTO.class);
            if (product == null) {
                throw new ServiceUnavailableException("Product service returned an empty response for id: " + productId);
            }
            return product;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        } catch (RestClientException e) {
            log.error("Failed to fetch product metadata for id {}: {}", productId, e.getMessage());
            throw new ServiceUnavailableException("Product service is currently unavailable.");
        }
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

    public CartDTO getCart(String userId) {
        ShoppingCart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createNewCart(userId));
        return convertToDTO(cart);
    }

    public CartDTO addToCart(String userId, String productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        ProductDTO product = fetchProduct(productId);

        if (product.getQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient stock. Available: " + product.getQuantity());
        }

        ShoppingCart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createNewCart(userId));

        // Check if product already in cart
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;
            if (product.getQuantity() < newQuantity) {
                throw new IllegalArgumentException("Insufficient stock for updated quantity");
            }
            item.setQuantity(newQuantity);
        } else {
            CartItem newItem = new CartItem(productId, quantity, BigDecimal.valueOf(product.getPrice()));
            cart.getItems().add(newItem);
        }

        recalculateCartTotal(cart);
        cartRepository.save(cart);
        return convertToDTO(cart);
    }

    public CartDTO updateCartItem(String userId, String itemId, int quantity) {
        if (quantity <= 0) {
            return removeCartItem(userId, itemId);
        }

        ShoppingCart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(CART_NOT_FOUND_PREFIX + userId));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart: " + itemId));

        ProductDTO product = fetchProduct(item.getProductId());

        if (product.getQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient stock. Available: " + product.getQuantity());
        }

        item.setQuantity(quantity);
        recalculateCartTotal(cart);
        cartRepository.save(cart);
        return convertToDTO(cart);
    }

    public CartDTO removeCartItem(String userId, String itemId) {
        ShoppingCart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(CART_NOT_FOUND_PREFIX + userId));

        cart.getItems().removeIf(item -> item.getProductId().equals(itemId));
        recalculateCartTotal(cart);
        cartRepository.save(cart);
        return convertToDTO(cart);
    }

    public CartDTO clearCart(String userId) {
        ShoppingCart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(CART_NOT_FOUND_PREFIX + userId));
        cart.getItems().clear();
        cart.setTotalAmount(BigDecimal.ZERO);
        cartRepository.save(cart);
        return convertToDTO(cart);
    }

    public OrderDTO checkoutCart(String userId, CheckoutRequest request) {
        ShoppingCart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(CART_NOT_FOUND_PREFIX + userId));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot checkout with empty cart");
        }

        // Validate all products have sufficient stock
        List<ReduceStockItem> reduceStockItems = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            ProductDTO product = fetchProduct(item.getProductId());
            if (product.getQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException(String.format(
                        "Insufficient stock for product: %s. Only %d units left.",
                        product.getName(), product.getQuantity()
                ));
            }
            reduceStockItems.add(new ReduceStockItem(item.getProductId(), item.getQuantity()));
        }

        // Call remote stock reduction
        try {
            restTemplate.postForEntity(
                    productServiceUrl + "/internal/products/reduce-stock",
                    reduceStockItems,
                    Void.class
            );
        } catch (RestClientException e) {
            log.error("Failed to perform remote stock reduction: {}", e.getMessage());
            throw new ServiceUnavailableException("Stock reduction failed. Checkout aborted.");
        }

        // Create order from cart
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "PAY_ON_DELIVERY");
        order.setShippingAddress(request.getShippingAddress());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Convert cart items to order items
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem(cartItem.getProductId(), cartItem.getQuantity(), cartItem.getPriceAtTime());
            orderItems.add(orderItem);

            BigDecimal subtotal = cartItem.getPriceAtTime().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalPrice = totalPrice.add(subtotal);
        }

        order.setItems(orderItems);
        BigDecimal tax = totalPrice.multiply(new BigDecimal("0.25"));
        BigDecimal grandTotal = totalPrice.add(tax);
        order.setTotalAmount(grandTotal);

        // Save order
        Order savedOrder = orderRepository.save(order);

        // Clear shopping cart
        cart.getItems().clear();
        cart.setTotalAmount(BigDecimal.ZERO);
        cartRepository.save(cart);

        return convertOrderToDTO(savedOrder);
    }

    private ShoppingCart createNewCart(String userId) {
        ShoppingCart cart = new ShoppingCart();
        cart.setUserId(userId);
        cart.setItems(new ArrayList<>());
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setCreatedAt(LocalDateTime.now());
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    private void recalculateCartTotal(ShoppingCart cart) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            BigDecimal itemTotal = item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(itemTotal);
        }
        cart.setTotalAmount(total);
        cart.setUpdatedAt(LocalDateTime.now());
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }

    private CartDTO convertToDTO(ShoppingCart cart) {
        List<CartItemDTO> itemDTOs = cart.getItems().stream()
                .map(item -> {
                    BigDecimal subtotal = item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity()));
                    String productName = getProductName(item.getProductId());
                    return new CartItemDTO(item.getProductId(), item.getProductId(), productName,
                            item.getQuantity(), item.getPriceAtTime(), subtotal);
                })
                .toList();

        CartDTO dto = new CartDTO();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUserId());
        dto.setItems(itemDTOs);
        dto.setTotalPrice(cart.getTotalAmount());
        dto.setItemCount(cart.getItems().size());
        return dto;
    }

    private OrderDTO convertOrderToDTO(Order order) {
        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(item -> {
                    String productName = getProductName(item.getProductId());
                    return new OrderItemDTO(item.getProductId(), productName,
                            item.getQuantity(), item.getUnitPrice(), item.getTotalPrice());
                })
                .toList();

        OrderDTO dto = OrderDTO.from(order.getId(), order.getOrderNumber(), order.getUserId(), itemDTOs,
                order.getTotalAmount(), order.getStatus(), order.getPaymentMethod());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setTrackingNumber(order.getTrackingNumber());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        return dto;
    }
}
