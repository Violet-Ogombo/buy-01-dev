package com.example.order.dto;

import java.math.BigDecimal;

public class RevertStockItem {
    private String productId;
    private int quantity;
    private BigDecimal subtotal;

    public RevertStockItem() {}

    public RevertStockItem(String productId, int quantity, BigDecimal subtotal) {
        this.productId = productId;
        this.quantity = quantity;
        this.subtotal = subtotal;
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

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
