package com.example.product.service;

import com.example.product.dto.CartDTO;
import com.example.product.dto.CartItemDTO;
import com.example.product.dto.CheckoutRequest;
import com.example.product.dto.OrderDTO;
import com.example.product.dto.OrderItemDTO;
import com.example.product.exception.ResourceNotFoundException;
import com.example.product.model.*;
import com.example.product.repository.ShoppingCartRepository;
import com.example.product.repository.ProductRepository;
import com.example.product.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CartService {

    private static final String CART_NOT_FOUND_PREFIX = "Cart not found for user: ";

    private final ShoppingCartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Autowired
    public CartService(ShoppingCartRepository cartRepository, ProductRepository productRepository,
                       OrderRepository orderRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
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

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

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

        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

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
        for (CartItem item : cart.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + item.getProductId()));
            if (product.getQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException(String.format(
                        "Insufficient stock for product: %s. Only %d units left.",
                        product.getName(), product.getQuantity()
                ));
            }
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
            Product product = productRepository.findById(cartItem.getProductId()).get();

            OrderItem orderItem = new OrderItem(cartItem.getProductId(), cartItem.getQuantity(), cartItem.getPriceAtTime());
            orderItems.add(orderItem);

            BigDecimal subtotal = cartItem.getPriceAtTime().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            // Update product: reduce quantity, increment sales, add revenue
            product.setQuantity(product.getQuantity() - cartItem.getQuantity());
            product.setSalesCount(product.getSalesCount() + cartItem.getQuantity());
            product.setRevenue(product.getRevenue().add(subtotal));
            product.setUpdatedAt(LocalDateTime.now());
            productRepository.save(product);

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
                    String productName = productRepository.findById(item.getProductId())
                            .map(Product::getName)
                            .orElse("Unknown Product");
                    return new CartItemDTO(item.getProductId(), item.getProductId(), productName,
                            item.getQuantity(), item.getPriceAtTime(), subtotal);
                })
                .collect(Collectors.toList());

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
