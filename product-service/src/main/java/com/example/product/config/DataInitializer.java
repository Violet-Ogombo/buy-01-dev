package com.example.product.config;

import com.example.product.model.Product;
import com.example.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) throws Exception {
        // Only add sample data if no products exist
        if (productRepository.count() == 0) {
            logger.info("Initializing sample products and sample documents...");

            // Sample user IDs (string _id for easy lookups)
            String SELLER_1 = "sample-seller-1";
            String SELLER_2 = "sample-seller-2";
            String BUYER_1 = "sample-buyer-1";

            // Insert sample users if users collection is empty
            if (mongoTemplate.getCollection("users").countDocuments() == 0) {
                Map<String, Object> seller1 = new HashMap<>();
                seller1.put("_id", SELLER_1);
                seller1.put("username", "seller_one");
                seller1.put("email", "seller1@example.com");
                seller1.put("password", "password");
                seller1.put("phone", "000-000-0001");
                seller1.put("address", "1 Seller Lane");
                seller1.put("total_spent", 0);
                seller1.put("is_seller", true);
                seller1.put("seller_revenue", 0);
                seller1.put("created_at", LocalDateTime.now());
                seller1.put("updated_at", LocalDateTime.now());
                seller1.put("roles", Arrays.asList("SELLER"));

                Map<String, Object> seller2 = new HashMap<>();
                seller2.put("_id", SELLER_2);
                seller2.put("username", "seller_two");
                seller2.put("email", "seller2@example.com");
                seller2.put("password", "password");
                seller2.put("phone", "000-000-0002");
                seller2.put("address", "2 Seller Road");
                seller2.put("total_spent", 0);
                seller2.put("is_seller", true);
                seller2.put("seller_revenue", 0);
                seller2.put("created_at", LocalDateTime.now());
                seller2.put("updated_at", LocalDateTime.now());
                seller2.put("roles", Arrays.asList("SELLER"));

                Map<String, Object> buyer1 = new HashMap<>();
                buyer1.put("_id", BUYER_1);
                buyer1.put("username", "buyer_one");
                buyer1.put("email", "buyer1@example.com");
                buyer1.put("password", "password");
                buyer1.put("phone", "000-000-1000");
                buyer1.put("address", "100 Buyer Ave");
                buyer1.put("total_spent", 0);
                buyer1.put("is_seller", false);
                buyer1.put("seller_revenue", 0);
                buyer1.put("created_at", LocalDateTime.now());
                buyer1.put("updated_at", LocalDateTime.now());
                buyer1.put("roles", Arrays.asList("USER"));

                mongoTemplate.insert(seller1, "users");
                mongoTemplate.insert(seller2, "users");
                mongoTemplate.insert(buyer1, "users");
                logger.info("Sample users inserted.");
            } else {
                logger.info("Users already exist, skipping user initialization.");
            }

            // Sample products for testing
            Product product1 = new Product();
            product1.setName("iPhone 15 Pro");
            product1.setDescription("Latest Apple iPhone with advanced camera system");
            product1.setCategory("electronics");
            product1.setPrice(999.99);
            product1.setQuantity(50);
            product1.setUserId(SELLER_1);
       
            product1.setSalesCount(8);
            product1.setRevenue(BigDecimal.valueOf(7999.92));
            product1.setImageUrls(Arrays.asList("https://example.com/images/iphone15pro.jpg"));

            Product product2 = new Product();
            product2.setName("Samsung Galaxy S24");
            product2.setDescription("Flagship Android smartphone with AI features");
            product2.setCategory("electronics");
            product2.setPrice(899.99);
            product2.setQuantity(30);
            product2.setUserId(SELLER_1);
           
            product2.setSalesCount(5);
            product2.setRevenue(BigDecimal.valueOf(4499.95));
            product2.setImageUrls(Arrays.asList("https://example.com/images/galaxys24.jpg"));

            Product product3 = new Product();
            product3.setName("MacBook Air M3");
            product3.setDescription("Lightweight laptop with Apple M3 chip");
            product3.setCategory("electronics");
            product3.setPrice(1299.99);
            product3.setQuantity(25);
            product3.setUserId(SELLER_2);
            
            product3.setSalesCount(3);
            product3.setRevenue(BigDecimal.valueOf(3899.97));
            product3.setImageUrls(Arrays.asList("https://example.com/images/macbookairm3.jpg"));

            Product product4 = new Product();
            product4.setName("Sony WH-1000XM5");
            product4.setDescription("Premium noise-canceling headphones");
            product4.setCategory("electronics");
            product4.setPrice(349.99);
            product4.setQuantity(100);
            product4.setUserId(SELLER_2);
         
            product4.setSalesCount(10);
            product4.setRevenue(BigDecimal.valueOf(3499.90));
            product4.setImageUrls(Arrays.asList("https://example.com/images/sonywh1000xm5.jpg"));

            Product product5 = new Product();
            product5.setName("Nike Air Max 270");
            product5.setDescription("Comfortable running shoes with air cushioning");
            product5.setCategory("fashion");
            product5.setPrice(129.99);
            product5.setQuantity(75);
            product5.setUserId(SELLER_1);
          
            product5.setSalesCount(12);
            product5.setRevenue(BigDecimal.valueOf(1559.88));
            product5.setImageUrls(Arrays.asList("https://example.com/images/nikeairmax270.jpg"));

            // Save products and get generated IDs
            product1 = productRepository.save(product1);
            product2 = productRepository.save(product2);
            product3 = productRepository.save(product3);
            product4 = productRepository.save(product4);
            product5 = productRepository.save(product5);

            logger.info("Sample products added successfully!");

                // Create a sample shopping cart for BUYER_1
                Map<String, Object> cart = new HashMap<>();
                cart.put("user_id", BUYER_1);
                Map<String, Object> cartItem1 = new HashMap<>();
                cartItem1.put("product_id", product1.getId());
                cartItem1.put("quantity", 1);
                cartItem1.put("priceAtTime", product1.getPrice());
                cartItem1.put("addedAt", LocalDateTime.now());

                Map<String, Object> cartItem2 = new HashMap<>();
                cartItem2.put("product_id", product5.getId());
                cartItem2.put("quantity", 2);
                cartItem2.put("priceAtTime", product5.getPrice());
                cartItem2.put("addedAt", LocalDateTime.now());

                cart.put("items", Arrays.asList(cartItem1, cartItem2));
                cart.put("total_amount", product1.getPrice() + product5.getPrice() * 2);
                cart.put("created_at", LocalDateTime.now());
                cart.put("updated_at", LocalDateTime.now());
                mongoTemplate.insert(cart, "shopping_cart");

            // Create a sample order for BUYER_1
            Map<String, Object> order = new HashMap<>();
            order.put("user_id", BUYER_1);
            order.put("order_number", "ORD-" + UUID.randomUUID().toString().substring(0,8));
                Map<String, Object> orderItem1 = new HashMap<>();
                orderItem1.put("product_id", product1.getId());
                orderItem1.put("quantity", 1);
                orderItem1.put("unit_price", product1.getPrice());
                orderItem1.put("total_price", product1.getPrice());

                Map<String, Object> orderItem2 = new HashMap<>();
                orderItem2.put("product_id", product5.getId());
                orderItem2.put("quantity", 2);
                orderItem2.put("unit_price", product5.getPrice());
                orderItem2.put("total_price", product5.getPrice() * 2);

                order.put("items", Arrays.asList(orderItem1, orderItem2));
            order.put("status", "PENDING");
            order.put("total_amount", product1.getPrice() + product5.getPrice() * 2);
            order.put("payment_method", "pay_on_delivery");
            order.put("shipping_address", "100 Buyer Ave");
            order.put("tracking_number", "");
            order.put("created_at", LocalDateTime.now());
            order.put("updated_at", LocalDateTime.now());
            mongoTemplate.insert(order, "orders");

            // Create a sample wishlist for BUYER_1
            Map<String, Object> wish = new HashMap<>();
            wish.put("user_id", BUYER_1);
            wish.put("product_id", product3.getId());
            wish.put("added_at", LocalDateTime.now());
            mongoTemplate.insert(wish, "wishlist");

        } else {
            logger.info("Products already exist, skipping initialization.");
        }
    }
}