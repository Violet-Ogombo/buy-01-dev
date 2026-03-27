package com.example.product.service;

import com.example.product.model.Product;
import com.example.product.model.RemoteMedia;
import com.example.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final RestTemplate restTemplate;
    
    private final AuditService auditService;

    @Value("${media.service.url:http://localhost:8083}")
    private String mediaServiceUrl;

    @Autowired
    public ProductService(ProductRepository productRepository,
                          KafkaTemplate<String, String> kafkaTemplate,
                          RestTemplate restTemplate,
                          AuditService auditService) {
        this.productRepository = productRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.restTemplate = restTemplate;
        this.auditService = auditService;
    }

    public List<Product> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productRepository.findAll(pageable).getContent();
    }

    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product, String userId) {
        product.setUserId(userId);
        if (product.getImageUrls() == null) {
            product.setImageUrls(new ArrayList<>());
        }
        LocalDateTime now = LocalDateTime.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        Product saved = productRepository.save(product);
        auditService.logWriteOperation(userId, "CREATE", "Product", saved.getId(), 
            "name=" + saved.getName() + ", price=" + saved.getPrice());
        kafkaTemplate.send("product-created", saved.getId());
        return saved;
    }

    public Product updateProduct(String id, Product product, String userId) {
        return productRepository.findById(id).map(existingProduct -> {
            if (!existingProduct.getUserId().equals(userId)) {
                throw new com.example.product.exception.AccessDeniedException("You are not authorized to update this product.");
            }
            existingProduct.setName(product.getName());
            existingProduct.setDescription(product.getDescription());
            existingProduct.setPrice(product.getPrice());
            existingProduct.setQuantity(product.getQuantity());
            existingProduct.setImageUrls(product.getImageUrls());
            existingProduct.setUpdatedAt(LocalDateTime.now());
            Product updated = productRepository.save(existingProduct);
            auditService.logWriteOperation(userId, "UPDATE", "Product", id,
                "name=" + updated.getName() + ", price=" + updated.getPrice());
            kafkaTemplate.send("product-updated", updated.getId());
            return updated;
        }).orElseThrow(() -> new com.example.product.exception.ResourceNotFoundException("Product not found with id: " + id));
    }

    public void deleteProduct(String id, String userId) {
        productRepository.findById(id).ifPresentOrElse(product -> {
            if (!product.getUserId().equals(userId)) {
                throw new com.example.product.exception.AccessDeniedException("You are not authorized to delete this product.");
            }
            productRepository.deleteById(id);
            auditService.logWriteOperation(userId, "DELETE", "Product", id,
                "name=" + product.getName());
            kafkaTemplate.send("product-deleted", id);
        }, () -> {
            throw new com.example.product.exception.ResourceNotFoundException("Product not found with id: " + id);
        });
    }

    public Product addImages(String productId, List<String> mediaIds, String userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new com.example.product.exception.ResourceNotFoundException("Product not found with id: " + productId));

        if (!product.getUserId().equals(userId)) {
            throw new com.example.product.exception.AccessDeniedException("You are not authorized to modify this product.");
        }

        if (product.getImageUrls() == null) {
            product.setImageUrls(new ArrayList<>());
        }

        for (String mediaId : mediaIds) {
            if (mediaId == null || mediaId.isBlank()) {
                continue;
            }

            RemoteMedia media;
            try {
                media = restTemplate.getForObject(
                        mediaServiceUrl + "/images/" + mediaId + "/meta",
                        RemoteMedia.class
                );
            } catch (HttpClientErrorException.NotFound e) {
                throw new com.example.product.exception.ResourceNotFoundException("Media not found with id: " + mediaId);
            } catch (RestClientException e) {
                throw new RuntimeException("Failed to validate media with id: " + mediaId, e);
            }

            if (media == null || media.getUserId() == null || !media.getUserId().equals(userId)) {
                throw new com.example.product.exception.AccessDeniedException("You can only use your own media.");
            }

            String imageUrl = "/api/media/images/" + mediaId;
            if (!product.getImageUrls().contains(imageUrl)) {
                product.getImageUrls().add(imageUrl);
            }
        }

        product.setUpdatedAt(LocalDateTime.now());
        Product updated = productRepository.save(product);
        auditService.logWriteOperation(userId, "UPDATE", "Product", productId,
            "added " + mediaIds.size() + " images");
        return updated;
    }
}
