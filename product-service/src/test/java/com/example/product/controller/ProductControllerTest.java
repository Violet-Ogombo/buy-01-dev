package com.example.product.controller;

import com.example.product.dto.ProductCreateRequest;
import com.example.product.exception.ResourceNotFoundException;
import com.example.product.model.Product;
import com.example.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private Product testProduct;
    private ProductCreateRequest testRequest;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId("prod-123");
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(99.99);
        testProduct.setQuantity(10);
        testProduct.setUserId("user-123");
        testProduct.setCreatedAt(LocalDateTime.now());
        testProduct.setUpdatedAt(LocalDateTime.now());

        testRequest = new ProductCreateRequest();
        testRequest.setName("Test Product");
        testRequest.setDescription("Test Description");
        testRequest.setPrice(99.99);
        testRequest.setQuantity(10);
    }

    @Test
    void getAllProducts_returnsListOfProducts() {
        List<Product> products = List.of(testProduct);
        when(productService.getAllProducts(0, 20)).thenReturn(products);

        ResponseEntity<List<Product>> response = productController.getAllProducts(0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getId()).isEqualTo("prod-123");
        verify(productService).getAllProducts(0, 20);
    }

    @Test
    void getAllProducts_returnsEmptyList() {
        when(productService.getAllProducts(0, 20)).thenReturn(List.of());

        ResponseEntity<List<Product>> response = productController.getAllProducts(0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getProductById_returnsProductWhenFound() {
        when(productService.getProductById("prod-123")).thenReturn(Optional.of(testProduct));

        ResponseEntity<Product> response = productController.getProductById("prod-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo("prod-123");
        verify(productService).getProductById("prod-123");
    }

    @Test
    void getProductById_throwsExceptionWhenNotFound() {
        when(productService.getProductById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productController.getProductById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void createProduct_createsProductSuccessfully() {
        when(productService.createProduct(any(ProductCreateRequest.class), eq("user-123")))
                .thenReturn(testProduct);

        ResponseEntity<Product> response = productController.createProduct(testRequest, "user-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Test Product");
        verify(productService).createProduct(testRequest, "user-123");
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void updateProduct_updatesProductSuccessfully() {
        Product updated = new Product();
        updated.setId("prod-123");
        updated.setName("Updated Product");
        updated.setDescription("Updated Description");
        updated.setPrice(199.99);
        updated.setQuantity(5);
        updated.setUserId("user-123");

        when(productService.updateProduct("prod-123", testRequest, "user-123")).thenReturn(updated);

        ResponseEntity<Product> response = productController.updateProduct("prod-123", testRequest, "user-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Updated Product");
        assertThat(response.getBody().getPrice()).isEqualTo(199.99);
        verify(productService).updateProduct("prod-123", testRequest, "user-123");
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void deleteProduct_deletesProductSuccessfully() {
        ResponseEntity<Void> response = productController.deleteProduct("prod-123", "user-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(productService).deleteProduct("prod-123", "user-123");
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void deleteProduct_throws_whenProductNotFound() {
        doThrow(new ResourceNotFoundException("Product not found"))
                .when(productService).deleteProduct("nonexistent", "user-123");

        assertThatThrownBy(() -> productController.deleteProduct("nonexistent", "user-123"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllProducts_usesDefaultPagination() {
        when(productService.getAllProducts(0, 20)).thenReturn(List.of(testProduct));

        productController.getAllProducts(0, 20);

        verify(productService).getAllProducts(0, 20);
    }

    @Test
    void getAllProducts_usesCustomPagination() {
        when(productService.getAllProducts(2, 50)).thenReturn(List.of());

        productController.getAllProducts(2, 50);

        verify(productService).getAllProducts(2, 50);
    }
}
