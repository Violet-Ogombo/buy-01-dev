package com.example.order.service;

import com.example.order.dto.OrderDTO;
import com.example.order.dto.ProductDTO;
import com.example.order.exception.ResourceNotFoundException;
import com.example.order.exception.ServiceUnavailableException;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class OrderServiceTest {

    // ── FakeRestTemplate (subclass to avoid inline-mock issues) ───────────
    private static class FakeRestTemplate extends RestTemplate {
        boolean failPost = false;

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getForObject(String url, Class<T> type, Object... vars) {
            ProductDTO p = new ProductDTO("prod-1", "Widget", 10.0, 100, "seller");
            return (T) p;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> ResponseEntity<T> postForEntity(String url, Object req, Class<T> type, Object... vars) {
            if (failPost) throw new RestClientException("timeout");
            return (ResponseEntity<T>) ResponseEntity.ok().build();
        }
    }

    // ── In-memory proxy for OrderRepository ───────────────────────────────
    private static class RepoHolder {
        final Map<String, Order> saved = new LinkedHashMap<>();
    }

    private static OrderRepository makeOrderRepo(RepoHolder h) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "save" -> {
                    Order o = (Order) args[0];
                    if (o.getId() == null) o.setId("gen-" + System.nanoTime());
                    h.saved.put(o.getId(), o);
                    return o;
                }
                case "findById" -> { return Optional.ofNullable(h.saved.get((String) args[0])); }
                case "findByUserId" -> {
                    String uid = (String) args[0];
                    Pageable p = (Pageable) args[1];
                    List<Order> list = h.saved.values().stream()
                            .filter(o -> uid.equals(o.getUserId())).toList();
                    return new PageImpl<>(list, p, list.size());
                }
                case "findByUserIdAndStatus" -> {
                    String uid = (String) args[0];
                    OrderStatus st = (OrderStatus) args[1];
                    Pageable p = (Pageable) args[2];
                    List<Order> list = h.saved.values().stream()
                            .filter(o -> uid.equals(o.getUserId()) && st == o.getStatus()).toList();
                    return new PageImpl<>(list, p, list.size());
                }
                case "findByUserIdAndOrderNumberContaining" -> {
                    String uid = (String) args[0];
                    String kw = (String) args[1];
                    Pageable p = (Pageable) args[2];
                    List<Order> list = h.saved.values().stream()
                            .filter(o -> uid.equals(o.getUserId()) && o.getOrderNumber() != null
                                    && o.getOrderNumber().contains(kw)).toList();
                    return new PageImpl<>(list, p, list.size());
                }
                default -> { return null; }
            }
        };
        return (OrderRepository) Proxy.newProxyInstance(
                OrderRepository.class.getClassLoader(),
                new Class<?>[]{OrderRepository.class},
                handler);
    }

    // ─────────────────────────────────────────────────────────────────────

    private RepoHolder repoHolder;
    private FakeRestTemplate restTemplate;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        repoHolder = new RepoHolder();
        restTemplate = new FakeRestTemplate();
        orderService = new OrderService(makeOrderRepo(repoHolder), restTemplate);
        ReflectionTestUtils.setField(orderService, "productServiceUrl", "http://product-service:8082");
    }

    private Order persist(String id, String userId, OrderStatus status) {
        Order o = new Order();
        o.setId(id);
        o.setUserId(userId);
        o.setOrderNumber("ORD-" + id);
        o.setStatus(status);
        o.setTotalAmount(BigDecimal.TEN);
        o.setItems(new ArrayList<>());
        repoHolder.saved.put(id, o);
        return o;
    }

    private Order persistWithItems(String id, String userId, OrderStatus status) {
        Order o = persist(id, userId, status);
        o.setItems(new ArrayList<>(List.of(new OrderItem("prod-1", 2, new BigDecimal("15.00")))));
        return o;
    }

    // ──────────────────────────────────────────────────────────────────────
    // getOrderById
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void getOrderById_returnsDTO() {
        persist("o1", "u1", OrderStatus.PENDING);
        OrderDTO dto = orderService.getOrderById("o1");
        assertThat(dto.getId()).isEqualTo("o1");
        assertThat(dto.getUserId()).isEqualTo("u1");
    }

    @Test
    void getOrderById_notFound_throws() {
        assertThatThrownBy(() -> orderService.getOrderById("bad"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ──────────────────────────────────────────────────────────────────────
    // getUserOrders / getUserOrdersByStatus / searchUserOrders
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void getUserOrders_returnsMappedPage() {
        persist("o1", "u1", OrderStatus.PENDING);
        Page<OrderDTO> page = orderService.getUserOrders("u1", PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUserId()).isEqualTo("u1");
    }

    @Test
    void getUserOrdersByStatus_filtersByStatus() {
        persist("o1", "u1", OrderStatus.SHIPPED);
        Page<OrderDTO> page = orderService.getUserOrdersByStatus("u1", OrderStatus.SHIPPED, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void searchUserOrders_returnsMappedPage() {
        Order o = persist("o3", "u1", OrderStatus.PENDING);
        o.setOrderNumber("ORD-FIND-ME");
        Page<OrderDTO> result = orderService.searchUserOrders("u1", "ORD-FIND", PageRequest.of(0, 5));
        assertThat(result.getContent()).hasSize(1);
    }

    // ──────────────────────────────────────────────────────────────────────
    // updateOrderStatus
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void updateOrderStatus_pendingToProcessing() {
        persist("o1", "u1", OrderStatus.PENDING);
        assertThat(orderService.updateOrderStatus("o1", OrderStatus.PROCESSING).getStatus())
                .isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void updateOrderStatus_pendingToCancelled() {
        persist("o1", "u1", OrderStatus.PENDING);
        assertThat(orderService.updateOrderStatus("o1", OrderStatus.CANCELLED).getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void updateOrderStatus_processingToShipped() {
        persist("o1", "u1", OrderStatus.PROCESSING);
        assertThat(orderService.updateOrderStatus("o1", OrderStatus.SHIPPED).getStatus())
                .isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void updateOrderStatus_processingToCancelled() {
        persist("o1", "u1", OrderStatus.PROCESSING);
        assertThat(orderService.updateOrderStatus("o1", OrderStatus.CANCELLED).getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void updateOrderStatus_shippedToDelivered() {
        persist("o1", "u1", OrderStatus.SHIPPED);
        assertThat(orderService.updateOrderStatus("o1", OrderStatus.DELIVERED).getStatus())
                .isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void updateOrderStatus_invalidTransition_throws() {
        persist("o1", "u1", OrderStatus.SHIPPED);
        assertThatThrownBy(() -> orderService.updateOrderStatus("o1", OrderStatus.PENDING))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void updateOrderStatus_notFound_throws() {
        assertThatThrownBy(() -> orderService.updateOrderStatus("bad", OrderStatus.PROCESSING))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ──────────────────────────────────────────────────────────────────────
    // cancelOrder
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void cancelOrder_pendingOrder_succeeds() {
        persistWithItems("o1", "u1", OrderStatus.PENDING);
        orderService.cancelOrder("o1");
        assertThat(repoHolder.saved.get("o1").getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_deliveredOrder_throws() {
        persist("o1", "u1", OrderStatus.DELIVERED);
        assertThatThrownBy(() -> orderService.cancelOrder("o1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot cancel order");
    }

    @Test
    void cancelOrder_alreadyCancelled_throws() {
        persist("o1", "u1", OrderStatus.CANCELLED);
        assertThatThrownBy(() -> orderService.cancelOrder("o1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelOrder_productServiceFailure_throws() {
        persistWithItems("o1", "u1", OrderStatus.PENDING);
        restTemplate.failPost = true;
        assertThatThrownBy(() -> orderService.cancelOrder("o1"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("communication failure");
    }

    @Test
    void cancelOrder_notFound_throws() {
        assertThatThrownBy(() -> orderService.cancelOrder("bad"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ──────────────────────────────────────────────────────────────────────
    // redoOrder
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void redoOrder_createsNewPendingOrder() {
        Order original = persistWithItems("o1", "u1", OrderStatus.DELIVERED);
        original.setPaymentMethod("PAY_ON_DELIVERY");
        original.setShippingAddress("123 Street");

        OrderDTO result = orderService.redoOrder("o1");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getUserId()).isEqualTo("u1");
    }

    @Test
    void redoOrder_notFound_throws() {
        assertThatThrownBy(() -> orderService.redoOrder("bad"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
