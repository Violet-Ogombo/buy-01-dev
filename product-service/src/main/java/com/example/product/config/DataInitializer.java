package com.example.product.config;

import com.example.product.model.Product;
import com.example.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private static final String SAMPLE_SELLER_1 = "sample-seller-1";
    private static final String SAMPLE_SELLER_2 = "sample-seller-2";

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        // Only add sample data if no products exist
        if (productRepository.count() == 0) {
            logger.info("Initializing sample products...");
            
            // Sample products for testing
            Product product1 = new Product();
            product1.setName("iPhone 15 Pro");
            product1.setDescription("Latest Apple iPhone with advanced camera system");
            product1.setPrice(999.99);
            product1.setQuantity(50);
            product1.setUserId(SAMPLE_SELLER_1); // Will be replaced by actual seller ID
            
            Product product2 = new Product();
            product2.setName("Samsung Galaxy S24");
            product2.setDescription("Flagship Android smartphone with AI features");
            product2.setPrice(899.99);
            product2.setQuantity(30);
            product2.setUserId(SAMPLE_SELLER_1);
            
            Product product3 = new Product();
            product3.setName("MacBook Air M3");
            product3.setDescription("Lightweight laptop with Apple M3 chip");
            product3.setPrice(1299.99);
            product3.setQuantity(25);
            product3.setUserId(SAMPLE_SELLER_2);
            
            Product product4 = new Product();
            product4.setName("Sony WH-1000XM5");
            product4.setDescription("Premium noise-canceling headphones");
            product4.setPrice(349.99);
            product4.setQuantity(100);
            product4.setUserId(SAMPLE_SELLER_2);
            
            Product product5 = new Product();
            product5.setName("Nike Air Max 270");
            product5.setDescription("Comfortable running shoes with air cushioning");
            product5.setPrice(129.99);
            product5.setQuantity(75);
            product5.setUserId(SAMPLE_SELLER_1);
            
            productRepository.save(product1);
            productRepository.save(product2);  
            productRepository.save(product3);
            productRepository.save(product4);
            productRepository.save(product5);
            
            logger.info("Sample products added successfully!");
        } else {
            logger.info("Products already exist, skipping initialization.");
        }
    }
}