package com.example.product.dto;

import java.math.BigDecimal;
import java.util.List;

public class BuyerAnalyticsDTO {
    private String userId;
    private int totalOrders;
    private BigDecimal totalSpent;
    private List<MostBoughtProductDTO> mostBoughtProducts;

    public BuyerAnalyticsDTO() {}

    public BuyerAnalyticsDTO(String userId, int totalOrders, BigDecimal totalSpent, 
                            List<MostBoughtProductDTO> mostBoughtProducts) {
        this.userId = userId;
        this.totalOrders = totalOrders;
        this.totalSpent = totalSpent;
        this.mostBoughtProducts = mostBoughtProducts;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }

    public List<MostBoughtProductDTO> getMostBoughtProducts() { return mostBoughtProducts; }
    public void setMostBoughtProducts(List<MostBoughtProductDTO> mostBoughtProducts) { 
        this.mostBoughtProducts = mostBoughtProducts; 
    }

    public static class MostBoughtProductDTO {
        private String productId;
        private String productName;
        private int totalQuantity;
        private BigDecimal totalSpent;
        private int purchaseCount;

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
