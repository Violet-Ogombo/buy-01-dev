package com.example.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WishlistItemDTO {
    private String id;
    private String productId;
    private String productName;
    private BigDecimal price;
    private String imageUrl;
    private LocalDateTime addedAt;

    public WishlistItemDTO() {}

    public WishlistItemDTO(String id, String productId, String productName, BigDecimal price, 
                          String imageUrl, LocalDateTime addedAt) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.imageUrl = imageUrl;
        this.addedAt = addedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
