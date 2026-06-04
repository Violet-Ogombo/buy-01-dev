package com.example.product.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "wishlist")
@CompoundIndexes({
    @CompoundIndex(name = "user_product_unique", def = "{'user_id': 1, 'product_id': 1}", unique = true)
})
public class Wishlist {

    @Id
    private String id;

    @Field("user_id")
    @Indexed
    private String userId;

    @Field("product_id")
    @Indexed
    private String productId;

    @Field("added_at")
    private LocalDateTime addedAt;

    // Constructors
    public Wishlist() {
        this.addedAt = LocalDateTime.now();
    }

    public Wishlist(String userId, String productId) {
        this();
        this.userId = userId;
        this.productId = productId;
    }

    // Getters and Setters
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

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    @Override
    public String toString() {
        return "Wishlist{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", productId='" + productId + '\'' +
                ", addedAt=" + addedAt +
                '}';
    }
}
