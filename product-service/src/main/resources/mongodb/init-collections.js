// MongoDB Index Initialization Script
// Run this script using mongosh or mongo shell:
// mongosh < init-collections.js

// Select the buy01 database (script-safe form)
db = db.getSiblingDB("buy01");

// ==================== Shopping Cart Indexes ====================
db.shopping_cart.createIndex({ "user_id": 1 });

// ==================== Orders Indexes ====================
db.orders.createIndex({ "user_id": 1 });
db.orders.createIndex({ "status": 1 });
db.orders.createIndex({ "order_number": 1 }, { unique: true });

// ==================== Seller Products Indexes ====================
db.seller_products.createIndex({ "seller_id": 1 });
db.seller_products.createIndex({ "product_id": 1 });

// ==================== Wishlist Indexes ====================
db.wishlist.createIndex({ "user_id": 1, "product_id": 1 }, { unique: true });

// ==================== Products Indexes ====================
db.products.createIndex({ "userId": 1 });

// ==================== Users Indexes ====================
db.users.createIndex({ "email": 1 }, { unique: true });

// Verify all indexes have been created
print("All indexes created successfully!");
print("Shopping Cart indexes:", db.shopping_cart.getIndexes().length);
print("Orders indexes:", db.orders.getIndexes().length);
print("Seller Products indexes:", db.seller_products.getIndexes().length);
print("Wishlist indexes:", db.wishlist.getIndexes().length);
print("Products indexes:", db.products.getIndexes().length);
print("Users indexes:", db.users.getIndexes().length);
