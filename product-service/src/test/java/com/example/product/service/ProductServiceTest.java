package com.example.product.service;

import com.example.product.model.Product;
import com.example.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
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

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private ProductService productService;

	@BeforeEach
	void setUp() {
		// No-op; initialization handled by @InjectMocks
	}

	@Test
	void createProduct_setsOwnerAndPublishesEvent() {
		Product input = new Product();
		input.setName("Test Product");
		input.setPrice(100.0);
		input.setQuantity(5);

		String userId = "seller-123";

		ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
			Product p = invocation.getArgument(0);
			p.setId("prod-1");
			return p;
		});

		Product result = productService.createProduct(input, userId);

		assertThat(result.getUserId()).isEqualTo(userId);
		assertThat(result.getCreatedAt()).isNotNull();
		assertThat(result.getUpdatedAt()).isNotNull();
		assertThat(result.getImageUrls()).isNotNull();

		verify(productRepository).save(productCaptor.capture());
		Product saved = productCaptor.getValue();
		assertThat(saved.getUserId()).isEqualTo(userId);

		verify(kafkaTemplate).send(eq("product-created"), eq("prod-1"));
	}
}
