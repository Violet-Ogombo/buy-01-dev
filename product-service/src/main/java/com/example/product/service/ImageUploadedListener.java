package com.example.product.service;

import com.example.product.event.ImageUploadedEvent;
import com.example.product.model.Product;
import com.example.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ImageUploadedListener {

	private static final Log logger = LogFactory.getLog(ImageUploadedListener.class);

	private final ProductRepository productRepository;
	private final ObjectMapper objectMapper;

	@Autowired
	public ImageUploadedListener(ProductRepository productRepository, ObjectMapper objectMapper) {
		this.productRepository = productRepository;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "image-uploaded", groupId = "product-service")
	public void handleImageUploaded(String message) {
		try {
			ImageUploadedEvent event = objectMapper.readValue(message, ImageUploadedEvent.class);
			if (event.getProductId() == null || event.getProductId().isBlank()) {
				return;
			}

			Product product = productRepository.findById(event.getProductId()).orElse(null);
			if (product == null) {
				return;
			}

			if (product.getUserId() == null || !product.getUserId().equals(event.getUserId())) {
				return;
			}

			String imageUrl = "/api/media/images/" + event.getMediaId();
			if (product.getImageUrls() == null) {
				product.setImageUrls(new java.util.ArrayList<>());
			}
			if (!product.getImageUrls().contains(imageUrl)) {
				product.getImageUrls().add(imageUrl);
				product.setUpdatedAt(LocalDateTime.now());
				productRepository.save(product);
			}
		} catch (Exception e) {
			logger.warn("Failed to process image-uploaded event", e);
		}
	}
}
