package com.example.identity.dto;

import java.math.BigDecimal;
import java.util.List;

public class UserProfileDTO {
    private String userId;
    private String username;
    private String email;
    private String phone;
    private String address;
    private BigDecimal totalSpent;
    private boolean isSeller;
    private BigDecimal sellerRevenue;
    private List<MostBoughtProductDTO> mostBoughtProducts;
    private int totalOrders;

    public UserProfileDTO() {}

    public UserProfileDTO(String userId, String username, String email, String phone, 
                          String address, BigDecimal totalSpent, boolean isSeller, 
                          BigDecimal sellerRevenue, List<MostBoughtProductDTO> mostBoughtProducts,
                          int totalOrders) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.totalSpent = totalSpent;
        this.isSeller = isSeller;
        this.sellerRevenue = sellerRevenue;
        this.mostBoughtProducts = mostBoughtProducts;
        this.totalOrders = totalOrders;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

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

    public List<MostBoughtProductDTO> getMostBoughtProducts() { return mostBoughtProducts; }
    public void setMostBoughtProducts(List<MostBoughtProductDTO> mostBoughtProducts) { 
        this.mostBoughtProducts = mostBoughtProducts; 
    }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public static class MostBoughtProductDTO {
        private String productId;
        private String productName;
        private int totalQuantity;
        private BigDecimal totalSpent;
        private int purchaseCount;  // how many orders this product appears in

        public MostBoughtProductDTO() {}

        public MostBoughtProductDTO(String productId, String productName, int totalQuantity, 
                                    BigDecimal totalSpent, int purchaseCount) {
            this.productId = productId;
            this.productName = productName;
            this.totalQuantity = totalQuantity;
            this.totalSpent = totalSpent;
            this.purchaseCount = purchaseCount;
        }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public int getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }

        public BigDecimal getTotalSpent() { return totalSpent; }
        public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }

        public int getPurchaseCount() { return purchaseCount; }
        public void setPurchaseCount(int purchaseCount) { this.purchaseCount = purchaseCount; }
    }
}
