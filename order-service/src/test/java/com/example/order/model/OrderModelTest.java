package com.example.order.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class OrderModelTest {

    // ──────────────────────────────────────────────────────────────────────
    // Order
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void order_defaultConstructor_setsDefaults() {
        Order o = new Order();
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(o.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(o.getCreatedAt()).isNotNull();
        assertThat(o.getUpdatedAt()).isNotNull();
    }

    @Test
    void order_parameterizedConstructor_setsFields() {
        Order o = new Order("u1", "ORD-001", "CARD", "123 St");
        assertThat(o.getUserId()).isEqualTo("u1");
        assertThat(o.getOrderNumber()).isEqualTo("ORD-001");
        assertThat(o.getPaymentMethod()).isEqualTo("CARD");
        assertThat(o.getShippingAddress()).isEqualTo("123 St");
    }

    @Test
    void order_setStatus_updatesUpdatedAt() throws InterruptedException {
        Order o = new Order();
        LocalDateTime before = o.getUpdatedAt();
        
        o.setStatus(OrderStatus.PROCESSING);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(o.getUpdatedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void order_allGettersAndSetters() {
        Order o = new Order();
        o.setId("id1");
        o.setUserId("u1");
        o.setOrderNumber("ORD-1");
        o.setTotalAmount(new BigDecimal("100.00"));
        o.setPaymentMethod("CASH");
        o.setShippingAddress("Street");
        o.setTrackingNumber("TRK-001");
        LocalDateTime now = LocalDateTime.now();
        o.setCreatedAt(now);
        o.setUpdatedAt(now);
        o.setDeliveredAt(now);
        o.setItems(new ArrayList<>(List.of(new OrderItem("p1", 1, BigDecimal.TEN))));

        assertThat(o.getId()).isEqualTo("id1");
        assertThat(o.getUserId()).isEqualTo("u1");
        assertThat(o.getOrderNumber()).isEqualTo("ORD-1");
        assertThat(o.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(o.getPaymentMethod()).isEqualTo("CASH");
        assertThat(o.getShippingAddress()).isEqualTo("Street");
        assertThat(o.getTrackingNumber()).isEqualTo("TRK-001");
        assertThat(o.getCreatedAt()).isEqualTo(now);
        assertThat(o.getUpdatedAt()).isEqualTo(now);
        assertThat(o.getDeliveredAt()).isEqualTo(now);
        assertThat(o.getItems()).hasSize(1);
        assertThat(o.toString()).contains("ORD-1");
    }

    // ──────────────────────────────────────────────────────────────────────
    // OrderItem
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void orderItem_parameterizedConstructor_calculatesTotalPrice() {
        OrderItem item = new OrderItem("p1", 3, new BigDecimal("10.00"));
        assertThat(item.getTotalPrice()).isEqualByComparingTo("30.00");
    }

    @Test
    void orderItem_setQuantity_recalculatesTotalPrice() {
        OrderItem item = new OrderItem("p1", 2, new BigDecimal("5.00"));
        item.setQuantity(4);
        assertThat(item.getTotalPrice()).isEqualByComparingTo("20.00");
    }

    @Test
    void orderItem_setUnitPrice_recalculatesTotalPrice() {
        OrderItem item = new OrderItem("p1", 3, new BigDecimal("5.00"));
        item.setUnitPrice(new BigDecimal("10.00"));
        assertThat(item.getTotalPrice()).isEqualByComparingTo("30.00");
    }

    @Test
    void orderItem_setters_work() {
        OrderItem item = new OrderItem();
        item.setProductId("p1");
        item.setTotalPrice(new BigDecimal("50.00"));
        assertThat(item.getProductId()).isEqualTo("p1");
        assertThat(item.getTotalPrice()).isEqualByComparingTo("50.00");
        assertThat(item.toString()).contains("p1");
    }

    // ──────────────────────────────────────────────────────────────────────
    // CartItem
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void cartItem_parameterizedConstructor_setsFields() {
        CartItem item = new CartItem("p1", 2, new BigDecimal("7.50"));
        assertThat(item.getProductId()).isEqualTo("p1");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getPriceAtTime()).isEqualByComparingTo("7.50");
        assertThat(item.getTotal()).isEqualByComparingTo("15.00");
    }

    @Test
    void cartItem_defaultConstructor_setsAddedAt() {
        CartItem item = new CartItem();
        assertThat(item.getAddedAt()).isNotNull();
    }

    @Test
    void cartItem_setters_work() {
        CartItem item = new CartItem();
        item.setProductId("p2");
        item.setQuantity(5);
        item.setPriceAtTime(new BigDecimal("3.00"));
        LocalDateTime now = LocalDateTime.now();
        item.setAddedAt(now);

        assertThat(item.getProductId()).isEqualTo("p2");
        assertThat(item.getQuantity()).isEqualTo(5);
        assertThat(item.getPriceAtTime()).isEqualByComparingTo("3.00");
        assertThat(item.getAddedAt()).isEqualTo(now);
        assertThat(item.toString()).contains("p2");
    }

    // ──────────────────────────────────────────────────────────────────────
    // ShoppingCart
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void shoppingCart_defaultConstructor_setsDefaults() {
        ShoppingCart cart = new ShoppingCart();
        assertThat(cart.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cart.getCreatedAt()).isNotNull();
        assertThat(cart.getUpdatedAt()).isNotNull();
    }

    @Test
    void shoppingCart_parameterizedConstructor_setsUserId() {
        ShoppingCart cart = new ShoppingCart("u1");
        assertThat(cart.getUserId()).isEqualTo("u1");
    }

    @Test
    void shoppingCart_setters_work() {
        ShoppingCart cart = new ShoppingCart();
        cart.setId("cart-1");
        cart.setUserId("u2");
        cart.setItems(new ArrayList<>());
        cart.setTotalAmount(new BigDecimal("99.99"));
        LocalDateTime now = LocalDateTime.now();
        cart.setCreatedAt(now);
        cart.setUpdatedAt(now);

        assertThat(cart.getId()).isEqualTo("cart-1");
        assertThat(cart.getUserId()).isEqualTo("u2");
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getTotalAmount()).isEqualByComparingTo("99.99");
        assertThat(cart.toString()).contains("u2");
    }

    // ──────────────────────────────────────────────────────────────────────
    // OrderStatus
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void orderStatus_canTransitionTo_validTransitions() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.PROCESSING)).isTrue();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.SHIPPED)).isTrue();
        assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED)).isTrue();
    }

    @Test
    void orderStatus_canTransitionTo_invalidTransitions() {
        assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PENDING)).isFalse();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.DELIVERED)).isFalse();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.PENDING)).isFalse();
    }

    @Test
    void orderStatus_getDisplayName() {
        assertThat(OrderStatus.PENDING.getDisplayName()).isEqualTo("Pending");
        assertThat(OrderStatus.PROCESSING.getDisplayName()).isEqualTo("Processing");
        assertThat(OrderStatus.SHIPPED.getDisplayName()).isEqualTo("Shipped");
        assertThat(OrderStatus.DELIVERED.getDisplayName()).isEqualTo("Delivered");
        assertThat(OrderStatus.CANCELLED.getDisplayName()).isEqualTo("Cancelled");
    }
}
