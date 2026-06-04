package com.example.product.dto;

import java.math.BigDecimal;
import java.util.List;

public class SellerProfileDTO {
    private String sellerId;
    private int totalProducts;
    private long totalSales;
    private BigDecimal totalRevenue;
    private List<BestSellingProductDTO> bestSellingProducts;

    public SellerProfileDTO() {}

    public SellerProfileDTO(String sellerId, int totalProducts, long totalSales, 
                            BigDecimal totalRevenue, List<BestSellingProductDTO> bestSellingProducts) {
        this.sellerId = sellerId;
        this.totalProducts = totalProducts;
        this.totalSales = totalSales;
        this.totalRevenue = totalRevenue;
        this.bestSellingProducts = bestSellingProducts;
    }

    // Getters and Setters
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public int getTotalProducts() { return totalProducts; }
    public void setTotalProducts(int totalProducts) { this.totalProducts = totalProducts; }

    public long getTotalSales() { return totalSales; }
    public void setTotalSales(long totalSales) { this.totalSales = totalSales; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public List<BestSellingProductDTO> getBestSellingProducts() { return bestSellingProducts; }
    public void setBestSellingProducts(List<BestSellingProductDTO> bestSellingProducts) { 
        this.bestSellingProducts = bestSellingProducts; 
    }

    public static class BestSellingProductDTO {
        private String productId;
        private String name;
        private double price;
        private int salesCount;
        private BigDecimal revenue;

        public BestSellingProductDTO() {}

        public BestSellingProductDTO(String productId, String name, double price, 
                                     int salesCount, BigDecimal revenue) {
            this.productId = productId;
            this.name = name;
            this.price = price;
            this.salesCount = salesCount;
            this.revenue = revenue;
        }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }

        public int getSalesCount() { return salesCount; }
        public void setSalesCount(int salesCount) { this.salesCount = salesCount; }

        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    }
}
