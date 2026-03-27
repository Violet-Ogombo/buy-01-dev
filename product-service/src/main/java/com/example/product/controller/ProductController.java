package com.example.product.controller;

import com.example.product.exception.ResourceNotFoundException;
import com.example.product.model.AddImagesRequest;
import com.example.product.model.Product;
import com.example.product.service.ProductService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/")
public class ProductController {
	private static final Logger log = LoggerFactory.getLogger(ProductController.class);
	private final ProductService productService;

	@Autowired
	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	@PermitAll
	public ResponseEntity<List<Product>> getAllProducts(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		List<Product> products = productService.getAllProducts(page, size);
		return ResponseEntity.ok(products);
	}

	@GetMapping("/{id}")
	@PermitAll
	public ResponseEntity<Product> getProductById(@PathVariable String id) {
		Product product = productService.getProductById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
		return ResponseEntity.ok(product);
	}

	@PostMapping
	@PreAuthorize("hasRole('SELLER')")
	public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product, 
	                                             @RequestHeader("X-User-Id") String userId) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		log.info("✅ createProduct endpoint reached - Authentication: {}, Principal: {}, Authorities: {}", 
				auth != null ? "PRESENT" : "NULL", 
				auth != null ? auth.getPrincipal() : "N/A",
				auth != null ? auth.getAuthorities() : "N/A");
		
		Product savedProduct = productService.createProduct(product, userId);
		return ResponseEntity.status(201).body(savedProduct);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('SELLER')")
	public ResponseEntity<Product> updateProduct(
			@PathVariable String id,
			@Valid @RequestBody Product product,
			@RequestHeader("X-User-Id") String userId) {
		Product updated = productService.updateProduct(id, product, userId);
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('SELLER')")
	public ResponseEntity<Void> deleteProduct(
			@PathVariable String id,
			@RequestHeader("X-User-Id") String userId) {
		productService.deleteProduct(id, userId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/images")
	@PreAuthorize("hasRole('SELLER')")
	public ResponseEntity<Product> addImages(
			@PathVariable String id,
			@RequestBody AddImagesRequest request,
			@RequestHeader("X-User-Id") String userId) {
		Product updated = productService.addImages(id, request.getMediaIds(), userId);
		return ResponseEntity.ok(updated);
	}
}

