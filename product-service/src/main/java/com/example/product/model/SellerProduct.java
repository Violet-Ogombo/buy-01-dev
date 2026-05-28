package com.example.product.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "seller_products")
public class SellerProduct {

    @Id
    private String id;

    @Field("seller_id")
    @Indexed
    private String sellerId;

    @Field("product_id")
    @Indexed
    private String productId;

    @Field("sales_count")
    private int salesCount;

    @Field("revenue")
    private BigDecimal revenue;

    @Field("created_at")
    private LocalDateTime createdAt;

    // Constructors
    public SellerProduct() {
        this.salesCount = 0;
        this.revenue = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
    }

    public SellerProduct(String sellerId, String productId) {
        this();
        this.sellerId = sellerId;
        this.productId = productId;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(int salesCount) {
        this.salesCount = salesCount;
    }

    public void incrementSalesCount(int quantity) {
        this.salesCount += quantity;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public void addRevenue(BigDecimal amount) {
        this.revenue = this.revenue.add(amount);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "SellerProduct{" +
                "id='" + id + '\'' +
                ", sellerId='" + sellerId + '\'' +
                ", productId='" + productId + '\'' +
                ", salesCount=" + salesCount +
                ", revenue=" + revenue +
                '}';
    }
}
