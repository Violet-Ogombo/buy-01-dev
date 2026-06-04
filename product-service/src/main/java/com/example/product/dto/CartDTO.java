package com.example.product.dto;

import java.math.BigDecimal;
import java.util.List;

public class CartDTO {
    private String id;
    private String userId;
    private List<CartItemDTO> items;
    private BigDecimal totalPrice;
    private int itemCount;

    public CartDTO() {}

    public CartDTO(String id, String userId, List<CartItemDTO> items, BigDecimal totalPrice, int itemCount) {
        this.id = id;
        this.userId = userId;
        this.items = items;
        this.totalPrice = totalPrice;
        this.itemCount = itemCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<CartItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CartItemDTO> items) {
        this.items = items;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }
}
