package com.example.product;

import com.example.product.controller.ProductController;
import com.example.product.exception.GlobalExceptionHandler;
import com.example.product.model.Product;
import com.example.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductIntegrationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(new FakeProductService()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllProducts_returnsEmptyListInitially() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getProductById_returnsNotFoundForNonExistent() throws Exception {
        mockMvc.perform(get("/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Product not found with id: nonexistent"));
    }

    @Test
    void productsEndpointExists() throws Exception {
        mockMvc.perform(get("/").param("page", "0").param("size", "20"))
                .andExpect(status().isOk());
    }

    private static final class FakeProductService extends ProductService {
        private FakeProductService() {
            super(null, null, null, null);
        }

        @Override
        public List<Product> getAllProducts(int page, int size) {
            return List.of();
        }

        @Override
        public Optional<Product> getProductById(String id) {
            return Optional.empty();
        }
    }
}
