package com.example.product.service;

import com.example.product.model.Product;
import com.example.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ProductService - NO Spring context loaded
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	@Mock
	private RestTemplate restTemplate;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private ProductService productService;

	@Test
	void createProduct_setsOwnerAndPublishesEvent() {
		// Arrange
		Product input = new Product();
		input.setName("Test Laptop");
		input.setPrice(999.99);
		input.setQuantity(10);
		String userId = "seller-123";

		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
			Product p = invocation.getArgument(0);
			p.setId("prod-1");
			return p;
		});

		// Act
		Product result = productService.createProduct(input, userId);

		// Assert
		assertThat(result.getUserId()).isEqualTo(userId);
		assertThat(result.getCreatedAt()).isNotNull();
		assertThat(result.getUpdatedAt()).isNotNull();
		assertThat(result.getId()).isEqualTo("prod-1");

		ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
		verify(productRepository).save(productCaptor.capture());
		Product saved = productCaptor.getValue();
		assertThat(saved.getUserId()).isEqualTo(userId);

		verify(kafkaTemplate).send(eq("product-created"), eq("prod-1"));
	}
}
