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
		input.setName("Test Product");
		input.setPrice(100.0);
		input.setQuantity(5);

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
		assertThat(result.getImageUrls()).isNotNull();
		assertThat(result.getId()).isEqualTo("prod-1");

		verify(productRepository).save(any(Product.class));
		verify(kafkaTemplate).send(eq("product-created"), eq("prod-1"));
	}

	@Test
	void createProduct_initializesEmptyImageUrlsWhenNull() {
		// Arrange
		Product input = new Product();
		input.setName("Product Without Images");
		input.setPrice(50.0);
		input.setQuantity(10);
		// imageUrls is null

		String userId = "seller-456";

		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
			Product p = invocation.getArgument(0);
			p.setId("prod-2");
			return p;
		});

		// Act
		Product result = productService.createProduct(input, userId);

		// Assert
		assertThat(result.getImageUrls()).isNotEmpty();
		assertThat(result.getImageUrls()).isInstanceOf(ArrayList.class);
	}

	@Test
	void getProductById_returnsProductWhenExists() {
		// Arrange
		String productId = "prod-1";
		Product product = new Product();
		product.setId(productId);
		product.setName("Test Product");
		product.setPrice(100.0);

		when(productRepository.findById(productId)).thenReturn(Optional.of(product));

		// Act
		Optional<Product> result = productService.getProductById(productId);

		// Assert
		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(productId);
		assertThat(result.get().getName()).isEqualTo("Test Product");
	}

	@Test
	void getProductById_returnsEmptyWhenNotExists() {
		// Arrange
		String productId = "nonexistent-id";
		when(productRepository.findById(productId)).thenReturn(Optional.empty());

		// Act
		Optional<Product> result = productService.getProductById(productId);

		// Assert
		assertThat(result).isEmpty();
	}

	@Test
	void getAllProducts_returnsPaginatedProducts() {
		// Arrange
		Product prod1 = new Product();
		prod1.setId("prod-1");
		prod1.setName("Product 1");

		Product prod2 = new Product();
		prod2.setId("prod-2");
		prod2.setName("Product 2");

		Pageable pageable = PageRequest.of(0, 10);
		when(productRepository.findAll(pageable))
			.thenReturn(new PageImpl<>(List.of(prod1, prod2)));

		// Act
		List<Product> result = productService.getAllProducts(0, 10);

		// Assert
		assertThat(result).hasSize(2);
		assertThat(result).extracting("id").contains("prod-1", "prod-2");
	}

	@Test
	void getAllProducts_returnsEmptyWhenNoProducts() {
		// Arrange
		Pageable pageable = PageRequest.of(0, 10);
		when(productRepository.findAll(pageable))
			.thenReturn(new PageImpl<>(List.of()));

		// Act
		List<Product> result = productService.getAllProducts(0, 10);

		// Assert
		assertThat(result).isEmpty();
	}

	@Test
	void updateProduct_successfullyUpdatesProductForOwner() {
		// Arrange
		String productId = "prod-1";
		String userId = "seller-123";

		Product existingProduct = new Product();
		existingProduct.setId(productId);
		existingProduct.setName("Old Name");
		existingProduct.setPrice(100.0);
		existingProduct.setUserId(userId);

		Product updateData = new Product();
		updateData.setName("New Name");
		updateData.setPrice(150.0);
		updateData.setDescription("New Description");
		updateData.setQuantity(5);

		Product updatedProduct = new Product();
		updatedProduct.setId(productId);
		updatedProduct.setName("New Name");
		updatedProduct.setPrice(150.0);
		updatedProduct.setUserId(userId);

		when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
		when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

		// Act
		Product result = productService.updateProduct(productId, updateData, userId);

		// Assert
		assertThat(result.getName()).isEqualTo("New Name");
		assertThat(result.getPrice()).isEqualTo(150.0);
		verify(kafkaTemplate).send(eq("product-updated"), eq(productId));
	}

	@Test
	void updateProduct_deniesAccessForNonOwner() {
		// Arrange
		String productId = "prod-1";
		String ownerId = "owner-123";
		String otherUserId = "other-user";

		Product existingProduct = new Product();
		existingProduct.setId(productId);
		existingProduct.setName("Product");
		existingProduct.setUserId(ownerId);

		Product updateData = new Product();
		updateData.setName("New Name");

		when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));

		// Act & Assert
		assertThatThrownBy(() ->
			productService.updateProduct(productId, updateData, otherUserId)
		)
		.isInstanceOf(AccessDeniedException.class)
		.hasMessageContaining("not authorized");

		verify(productRepository, never()).save(any());
		verify(kafkaTemplate, never()).send(anyString(), anyString());
	}

	@Test
	void updateProduct_throwsExceptionForNonExistentProduct() {
		// Arrange
		String productId = "nonexistent-id";
		String userId = "seller-123";

		Product updateData = new Product();
		updateData.setName("New Name");

		when(productRepository.findById(productId)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() ->
			productService.updateProduct(productId, updateData, userId)
		)
		.isInstanceOf(ResourceNotFoundException.class)
		.hasMessageContaining("not found");
	}

	@Test
	void deleteProduct_successfullyDeletesProductForOwner() {
		// Arrange
		String productId = "prod-1";
		String userId = "seller-123";

		Product product = new Product();
		product.setId(productId);
		product.setName("Product to Delete");
		product.setUserId(userId);

		when(productRepository.findById(productId)).thenReturn(Optional.of(product));

		// Act
		productService.deleteProduct(productId, userId);

		// Assert
		verify(productRepository).deleteById(productId);
		verify(kafkaTemplate).send(eq("product-deleted"), eq(productId));
	}

	@Test
	void deleteProduct_deniesAccessForNonOwner() {
		// Arrange
		String productId = "prod-1";
		String ownerId = "owner-123";
		String otherUserId = "other-user";

		Product product = new Product();
		product.setId(productId);
		product.setName("Product");
		product.setUserId(ownerId);

		when(productRepository.findById(productId)).thenReturn(Optional.of(product));

		// Act & Assert
		assertThatThrownBy(() ->
			productService.deleteProduct(productId, otherUserId)
		)
		.isInstanceOf(AccessDeniedException.class)
		.hasMessageContaining("not authorized");

		verify(productRepository, never()).deleteById(any());
		verify(kafkaTemplate, never()).send(anyString(), anyString());
	}

	@Test
	void deleteProduct_throwsExceptionForNonExistentProduct() {
		// Arrange
		String productId = "nonexistent-id";
		String userId = "seller-123";

		when(productRepository.findById(productId)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() ->
			productService.deleteProduct(productId, userId)
		)
		.isInstanceOf(ResourceNotFoundException.class)
		.hasMessageContaining("not found");

		verify(productRepository, never()).deleteById(any());
	}

	@Test
	void addImages_successfullyAddsMediaToProduct() {
		// Arrange
		String productId = "prod-1";
		String userId = "seller-123";
		String mediaId = "media-1";

		Product product = new Product();
		product.setId(productId);
		product.setUserId(userId);
		product.setImageUrls(new ArrayList<>());

		RemoteMedia media = new RemoteMedia();
		media.setId(mediaId);
		media.setUserId(userId);

		Product updatedProduct = new Product();
		updatedProduct.setId(productId);
		updatedProduct.setImageUrls(List.of("/api/media/images/" + mediaId));

		when(productRepository.findById(productId)).thenReturn(Optional.of(product));
		when(restTemplate.getForObject(
			contains("/api/media/images/" + mediaId),
			eq(RemoteMedia.class)
		)).thenReturn(media);
		when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

		// Act
		Product result = productService.addImages(productId, List.of(mediaId), userId);

		// Assert
		assertThat(result.getImageUrls()).contains("/api/media/images/" + mediaId);
		verify(productRepository).save(any(Product.class));
		verify(kafkaTemplate, never()).send(anyString(), anyString()); // No event published for image add
	}

	@Test
	void addImages_deniesAccessIfNotOwner() {
		// Arrange
		String productId = "prod-1";
		String ownerId = "owner-123";
		String otherUserId = "other-user";
		String mediaId = "media-1";

		Product product = new Product();
		product.setId(productId);
		product.setUserId(ownerId);

		when(productRepository.findById(productId)).thenReturn(Optional.of(product));

		// Act & Assert
		assertThatThrownBy(() ->
			productService.addImages(productId, List.of(mediaId), otherUserId)
		)
		.isInstanceOf(AccessDeniedException.class)
		.hasMessageContaining("not authorized");

		verify(productRepository, never()).save(any());
	}

	@Test
	void addImages_throwsExceptionForMediaNotFoundInMediaService() {
		// Arrange
		String productId = "prod-1";
		String userId = "seller-123";
		String mediaId = "nonexistent-media";

		Product product = new Product();
		product.setId(productId);
		product.setUserId(userId);
		product.setImageUrls(new ArrayList<>());

		when(productRepository.findById(productId)).thenReturn(Optional.of(product));
		when(restTemplate.getForObject(anyString(), eq(RemoteMedia.class)))
			.thenThrow(new HttpClientErrorException(org.springframework.http.HttpStatus.NOT_FOUND));

		// Act & Assert
		assertThatThrownBy(() ->
			productService.addImages(productId, List.of(mediaId), userId)
		)
		.isInstanceOf(ResourceNotFoundException.class)
		.hasMessageContaining("Media not found");
	}

	@Test
	void addImages_deniesAccessIfMediaDoesNotBelongToUser() {
		// Arrange
		String productId = "prod-1";
		String userId = "seller-123";
		String mediaId = "media-1";
		String mediaOwner = "other-seller";

		Product product = new Product();
		product.setId(productId);
		product.setUserId(userId);
		product.setImageUrls(new ArrayList<>());

		RemoteMedia media = new RemoteMedia();
		media.setId(mediaId);
		media.setUserId(mediaOwner); // Different owner

		when(productRepository.findById(productId)).thenReturn(Optional.of(product));
		when(restTemplate.getForObject(anyString(), eq(RemoteMedia.class)))
			.thenReturn(media);

		// Act & Assert
		assertThatThrownBy(() ->
			productService.addImages(productId, List.of(mediaId), userId)
		)
		.isInstanceOf(AccessDeniedException.class)
		.hasMessageContaining("own media");

		verify(productRepository, never()).save(any());
	}

	@Test
	void addImages_ignoresNullOrBlankMediaIds() {
		// Arrange
		String productId = "prod-1";
		String userId = "seller-123";

		Product product = new Product();
		product.setId(productId);
		product.setUserId(userId);
		product.setImageUrls(new ArrayList<>());

		Product updatedProduct = new Product();
		updatedProduct.setId(productId);
		updatedProduct.setImageUrls(new ArrayList<>());

		when(productRepository.findById(productId)).thenReturn(Optional.of(product));
		when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

		// Act
		Product result = productService.addImages(productId, List.of(null, "", "  "), userId);

		// Assert
		assertThat(result.getImageUrls()).isEmpty();
		verify(restTemplate, never()).getForObject(anyString(), any());
	}
}
