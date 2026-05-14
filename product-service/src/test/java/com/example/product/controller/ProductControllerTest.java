package com.example.product.controller;

import com.example.product.model.AddImagesRequest;
import com.example.product.model.Product;
import com.example.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @MockBean
    private ProductService productService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Initialization handled by Spring
    }

    @Test
    void getAllProducts_returnsProductsWithDefaultPagination() throws Exception {
        // Arrange
        Product product1 = new Product();
        product1.setId("prod-1");
        product1.setName("Product 1");

        Product product2 = new Product();
        product2.setId("prod-2");
        product2.setName("Product 2");

        when(productService.getAllProducts(0, 20))
            .thenReturn(List.of(product1, product2));

        // Act & Assert
        mockMvc.perform(get("/")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(productService).getAllProducts(0, 20);
    }

    @Test
    void getAllProducts_returnsEmptyListWhenNoProducts() throws Exception {
        // Arrange
        when(productService.getAllProducts(0, 20)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void getProductById_returnsProductWhenExists() throws Exception {
        // Arrange
        String productId = "prod-1";
        Product product = new Product();
        product.setId(productId);
        product.setName("Test Product");
        product.setPrice(100.0);

        when(productService.getProductById(productId)).thenReturn(Optional.of(product));

        // Act & Assert
        mockMvc.perform(get("/" + productId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(productService).getProductById(productId);
    }

    @Test
    void getProductById_throwsExceptionWhenNotFound() throws Exception {
        // Arrange
        String productId = "nonexistent-id";
        when(productService.getProductById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/" + productId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
}
