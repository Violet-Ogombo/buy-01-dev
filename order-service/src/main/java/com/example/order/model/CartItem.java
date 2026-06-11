package com.example.order.model;

import org.springframework.data.mongodb.core.mapping.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CartItem {

    @Field("product_id")
    private String productId;

    @Field("quantity")
    private int quantity;

    @Field("price_at_time")
    private BigDecimal priceAtTime;

    @Field("added_at")
    private LocalDateTime addedAt;

    public CartItem() {
        this.addedAt = LocalDateTime.now();
    }

    public CartItem(String productId, int quantity, BigDecimal priceAtTime) {
        this();
        this.productId = productId;
        this.quantity = quantity;
        this.priceAtTime = priceAtTime;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPriceAtTime() {
        return priceAtTime;
    }

    public void setPriceAtTime(BigDecimal priceAtTime) {
        this.priceAtTime = priceAtTime;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    public BigDecimal getTotal() {
        return priceAtTime.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String toString() {
        return "CartItem{" +
                "productId='" + productId + '\'' +
                ", quantity=" + quantity +
                ", priceAtTime=" + priceAtTime +
                ", addedAt=" + addedAt +
                '}';
    }
}
