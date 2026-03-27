package com.example.product.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "products")
public class Product {
	@Id
	private String id;

	@NotBlank(message = "Product name is required")
	@Size(min = 2, max = 100, message = "Name must be 2-100 characters")
	@Field("name")
	private String name;

	@Size(max = 500, message = "Description can't exceed 500 characters")
	@Field("description")
	private String description;

	@Positive(message = "Price must be greater than 0")
	@Field("price")
	private double price;

	@Positive(message = "Quantity must be greater than 0")
	@Field("quantity")
	private int quantity;

	@Field("userId")
	private String userId;

	@Field("imageUrls")
	private List<String> imageUrls = new ArrayList<>();

	@Field("createdAt")
	private LocalDateTime createdAt;

	@Field("updatedAt")
	private LocalDateTime updatedAt;

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

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public List<String> getImageUrls() {
		return imageUrls;
	}

	public void setImageUrls(List<String> imageUrls) {
		this.imageUrls = imageUrls;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
