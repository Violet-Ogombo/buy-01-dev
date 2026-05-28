package com.example.product.repository;

import com.example.product.model.Order;
import com.example.product.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    Page<Order> findByUserId(String userId, Pageable pageable);
    
    Page<Order> findByUserIdAndStatus(String userId, OrderStatus status, Pageable pageable);
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    Page<Order> findByUserIdAndOrderNumberContaining(String userId, String orderNumber, Pageable pageable);
}
