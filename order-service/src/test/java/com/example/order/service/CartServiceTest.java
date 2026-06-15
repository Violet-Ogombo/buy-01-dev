package com.example.order.service;

import com.example.order.dto.CartDTO;
import com.example.order.dto.CheckoutRequest;
import com.example.order.dto.OrderDTO;
import com.example.order.dto.ProductDTO;
import com.example.order.exception.ResourceNotFoundException;
import com.example.order.exception.ServiceUnavailableException;
import com.example.order.model.*;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ShoppingCartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class CartServiceTest {

    // ── FakeRestTemplate ──────────────────────────────────────────────────
    private static class FakeRestTemplate extends RestTemplate {
        ProductDTO product = new ProductDTO("p1", "Widget", 10.0, 100, "seller");
        boolean throwNotFound = false;
        boolean throwServiceDown = false;
        boolean failPost = false;

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getForObject(String url, Class<T> type, Object... vars) {
            if (throwNotFound) throw HttpClientErrorException.NotFound.create(
                    HttpStatus.NOT_FOUND, "Not Found",
                    org.springframework.http.HttpHeaders.EMPTY,
                    new byte[0], java.nio.charset.StandardCharsets.UTF_8);
            if (throwServiceDown) throw new RestClientException("service down");
            return (T) product;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> ResponseEntity<T> postForEntity(String url, Object req, Class<T> type, Object... vars) {
            if (failPost) throw new RestClientException("timeout");
            return (ResponseEntity<T>) ResponseEntity.ok().build();
        }
    }

    // ── Proxy-based repo holders ──────────────────────────────────────────
    private static class CartStore {
        final Map<String, ShoppingCart> m = new LinkedHashMap<>();
    }

    private static class OrderStore {
        final Map<String, Order> m = new LinkedHashMap<>();
        int seq = 0;
    }

    private static ShoppingCartRepository cartProxy(CartStore s) {
        InvocationHandler h = (proxy, method, args) -> {
            switch (method.getName()) {
                case "findByUserId" -> {
                    String uid = (String) args[0];
                    return s.m.values().stream().filter(c -> uid.equals(c.getUserId())).findFirst();
                }
                case "save" -> {
                    ShoppingCart c = (ShoppingCart) args[0];
                    if (c.getId() == null) c.setId("cart-" + System.nanoTime());
                    s.m.put(c.getId(), c);
                    return c;
                }
                case "findById" -> { return Optional.ofNullable(s.m.get((String) args[0])); }
                default -> { return null; }
            }
        };
        return (ShoppingCartRepository) Proxy.newProxyInstance(
                ShoppingCartRepository.class.getClassLoader(),
                new Class<?>[]{ShoppingCartRepository.class}, h);
    }

    private static OrderRepository orderProxy(OrderStore s) {
        InvocationHandler h = (proxy, method, args) -> {
            switch (method.getName()) {
                case "save" -> {
                    Order o = (Order) args[0];
                    if (o.getId() == null) o.setId("ord-" + (++s.seq));
                    s.m.put(o.getId(), o);
                    return o;
                }
                case "findById" -> { return Optional.ofNullable(s.m.get((String) args[0])); }
                case "findByUserId" -> { return new PageImpl<>(List.of(), (Pageable) args[1], 0); }
                case "findByUserIdAndStatus" -> { return new PageImpl<>(List.of(), (Pageable) args[2], 0); }
                case "findByUserIdAndOrderNumberContaining" -> { return new PageImpl<>(List.of(), (Pageable) args[2], 0); }
                default -> { return null; }
            }
        };
        return (OrderRepository) Proxy.newProxyInstance(
                OrderRepository.class.getClassLoader(),
                new Class<?>[]{OrderRepository.class}, h);
    }

    // ─────────────────────────────────────────────────────────────────────

    private CartStore cartStore;
    private OrderStore orderStore;
    private FakeRestTemplate restTemplate;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartStore = new CartStore();
        orderStore = new OrderStore();
        restTemplate = new FakeRestTemplate();
        cartService = new CartService(cartProxy(cartStore), orderProxy(orderStore), restTemplate);
        ReflectionTestUtils.setField(cartService, "productServiceUrl", "http://product-service:8082");
    }

    private ShoppingCart savedCart(String userId) {
        ShoppingCart c = new ShoppingCart(userId);
        c.setId("cart-" + userId);
        cartStore.m.put(c.getId(), c);
        return c;
    }

    // ──────────────────────────────────────────────────────────────────────
    // getCart
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void getCart_returnsExistingCart() {
        savedCart("u1");
        assertThat(cartService.getCart("u1").getUserId()).isEqualTo("u1");
    }

    @Test
    void getCart_createsWhenMissing() {
        CartDTO dto = cartService.getCart("u-new");
        assertThat(dto.getUserId()).isEqualTo("u-new");
    }

    // ──────────────────────────────────────────────────────────────────────
    // addToCart
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void addToCart_zeroQty_throws() {
        assertThatThrownBy(() -> cartService.addToCart("u1", "p1", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be greater than 0");
    }

    @Test
    void addToCart_negativeQty_throws() {
        assertThatThrownBy(() -> cartService.addToCart("u1", "p1", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addToCart_newItem_addsToCart() {
        restTemplate.product = new ProductDTO("p1", "Widget", 10.0, 10, "s");
        CartDTO result = cartService.addToCart("u1", "p1", 2);
        assertThat(result.getItemCount()).isEqualTo(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void addToCart_existingItem_accumulatesQty() {
        ShoppingCart c = savedCart("u1");
        c.getItems().add(new CartItem("p1", 3, BigDecimal.valueOf(10.0)));
        restTemplate.product = new ProductDTO("p1", "Widget", 10.0, 20, "s");
        CartDTO result = cartService.addToCart("u1", "p1", 2);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void addToCart_insufficientStock_throws() {
        restTemplate.product = new ProductDTO("p1", "Widget", 10.0, 1, "s");
        assertThatThrownBy(() -> cartService.addToCart("u1", "p1", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void addToCart_updatedQtyExceedsStock_throws() {
        ShoppingCart c = savedCart("u1");
        c.getItems().add(new CartItem("p1", 3, BigDecimal.valueOf(10.0)));
        restTemplate.product = new ProductDTO("p1", "Widget", 10.0, 4, "s");
        assertThatThrownBy(() -> cartService.addToCart("u1", "p1", 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock for updated quantity");
    }

    @Test
    void addToCart_productNotFound_throws() {
        restTemplate.throwNotFound = true;
        assertThatThrownBy(() -> cartService.addToCart("u1", "p1", 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addToCart_serviceDown_throws() {
        restTemplate.throwServiceDown = true;
        assertThatThrownBy(() -> cartService.addToCart("u1", "p1", 1))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Product service is currently unavailable");
    }

    @Test
    void addToCart_nullProductResponse_throws() {
        restTemplate.product = null;
        assertThatThrownBy(() -> cartService.addToCart("u1", "p1", 1))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("empty response");
    }

    // ──────────────────────────────────────────────────────────────────────
    // updateCartItem
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void updateCartItem_zeroQty_removesItem() {
        ShoppingCart c = savedCart("u1");
        c.getItems().add(new CartItem("p1", 2, BigDecimal.TEN));
        assertThat(cartService.updateCartItem("u1", "p1", 0).getItemCount()).isEqualTo(0);
    }

    @Test
    void updateCartItem_cartNotFound_throws() {
        assertThatThrownBy(() -> cartService.updateCartItem("u-missing", "p1", 2))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCartItem_itemNotFound_throws() {
        savedCart("u1");
        assertThatThrownBy(() -> cartService.updateCartItem("u1", "not-there", 2))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCartItem_insufficientStock_throws() {
        ShoppingCart c = savedCart("u1");
        c.getItems().add(new CartItem("p1", 1, BigDecimal.TEN));
        restTemplate.product = new ProductDTO("p1", "Widget", 10.0, 1, "s");
        assertThatThrownBy(() -> cartService.updateCartItem("u1", "p1", 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateCartItem_valid_succeeds() {
        ShoppingCart c = savedCart("u1");
        c.getItems().add(new CartItem("p1", 1, BigDecimal.TEN));
        restTemplate.product = new ProductDTO("p1", "Widget", 10.0, 10, "s");
        assertThat(cartService.updateCartItem("u1", "p1", 4).getItems().get(0).getQuantity()).isEqualTo(4);
    }

    // ──────────────────────────────────────────────────────────────────────
    // removeCartItem
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void removeCartItem_cartNotFound_throws() {
        assertThatThrownBy(() -> cartService.removeCartItem("u-missing", "p1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeCartItem_removesItem() {
        ShoppingCart c = savedCart("u1");
        c.getItems().add(new CartItem("p1", 1, BigDecimal.TEN));
        assertThat(cartService.removeCartItem("u1", "p1").getItemCount()).isEqualTo(0);
    }

    // ──────────────────────────────────────────────────────────────────────
    // clearCart
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void clearCart_notFound_throws() {
        assertThatThrownBy(() -> cartService.clearCart("u-missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void clearCart_clearsItems() {
        ShoppingCart c = savedCart("u1");
        c.getItems().add(new CartItem("p1", 2, BigDecimal.TEN));
        CartDTO result = cartService.clearCart("u1");
        assertThat(result.getItemCount()).isEqualTo(0);
        assertThat(result.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ──────────────────────────────────────────────────────────────────────
    // checkoutCart
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void checkoutCart_notFound_throws() {
        CheckoutRequest req = new CheckoutRequest();
        assertThatThrownBy(() -> cartService.checkoutCart("u-missing", req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void checkoutCart_emptyCart_throws() {
        savedCart("u1");
        CheckoutRequest req = new CheckoutRequest();
        assertThatThrownBy(() -> cartService.checkoutCart("u1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty cart");
    }

    @Test
    void checkoutCart_insufficientStock_throws() {
        ShoppingCart c = savedCart("u1");
        c.getItems().add(new CartItem("p1", 10, BigDecimal.TEN));
        restTemplate.product = new ProductDTO("p1", "Widget", 10.0, 2, "s");
        CheckoutRequest req = new CheckoutRequest();
        assertThatThrownBy(() -> cartService.checkoutCart("u1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void checkoutCart_stockReductionFails_throws() {
        ShoppingCart c = savedCart("u1");
        c.getItems().add(new CartItem("p1", 1, BigDecimal.TEN));
        restTemplate.product = new ProductDTO("p1", "Widget", 10.0, 10, "s");
        restTemplate.failPost = true;
        CheckoutRequest req = new CheckoutRequest();
        req.setPaymentMethod("PAY_ON_DELIVERY");
        assertThatThrownBy(() -> cartService.checkoutCart("u1", req))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Stock reduction failed");
    }

    @Test
    void checkoutCart_success_createsOrder() {
        ShoppingCart c = savedCart("u1");
        c.getItems().add(new CartItem("p1", 2, BigDecimal.valueOf(20.0)));
        restTemplate.product = new ProductDTO("p1", "Widget", 20.0, 10, "s");
        CheckoutRequest req = new CheckoutRequest();
        req.setPaymentMethod("PAY_ON_DELIVERY");
        req.setShippingAddress("123 Main St");

        OrderDTO result = cartService.checkoutCart("u1", req);

        assertThat(result.getUserId()).isEqualTo("u1");
        assertThat(cartStore.m.values().stream().filter(cart -> "u1".equals(cart.getUserId()))
                .findFirst().map(cart -> cart.getItems().size()).orElse(-1)).isEqualTo(0);
    }

    @Test
    void checkoutCart_nullPaymentMethod_defaultsToPAY_ON_DELIVERY() {
        ShoppingCart c = savedCart("u1");
        c.getItems().add(new CartItem("p1", 1, BigDecimal.TEN));
        restTemplate.product = new ProductDTO("p1", "Widget", 10.0, 10, "s");

        OrderDTO result = cartService.checkoutCart("u1", new CheckoutRequest());

        assertThat(result.getPaymentMethod()).isEqualTo("PAY_ON_DELIVERY");
    }
}
