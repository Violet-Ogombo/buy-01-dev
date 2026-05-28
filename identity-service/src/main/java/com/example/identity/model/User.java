package com.example.identity.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "users")
public class User {
    @Id
    private String id;
    
    private String name;
    
    private String email;
    
    private String password;
    
    private Role role;
    
    @Field("phone")
    private String phone;
    
    @Field("address")
    private String address;
    
    @Field("total_spent")
    private BigDecimal totalSpent;
    
    @Field("is_seller")
    private boolean isSeller;
    
    @Field("seller_revenue")
    private BigDecimal sellerRevenue;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public User() {
        this.totalSpent = BigDecimal.ZERO;
        this.sellerRevenue = BigDecimal.ZERO;
        this.isSeller = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }
    
    public boolean isSeller() { return isSeller; }
    public void setIsSeller(boolean seller) { isSeller = seller; }
    
    public BigDecimal getSellerRevenue() { return sellerRevenue; }
    public void setSellerRevenue(BigDecimal sellerRevenue) { this.sellerRevenue = sellerRevenue; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
