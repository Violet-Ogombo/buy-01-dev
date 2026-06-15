package com.example.product.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductSearchDTO {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private int quantity;
    private List<String> imageUrls;
    private long salesCount;
    private BigDecimal rating;
    private String category;

    public ProductSearchDTO() {}

    public static ProductSearchDTO from(String id, String name, String description,
                                        String category, BigDecimal price, int quantity) {
        ProductSearchDTO dto = new ProductSearchDTO();
        dto.id = id;
        dto.name = name;
        dto.description = description;
        dto.category = category;
        dto.price = price;
        dto.quantity = quantity;
        return dto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public long getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(long salesCount) {
        this.salesCount = salesCount;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
