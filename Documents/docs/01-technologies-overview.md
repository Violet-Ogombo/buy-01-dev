# Technologies Overview: What are they and why do we use them?

This document is designed to explain the core technologies used in this project in plain English. Think of it as a glossary and a high-level map of the tools that make this e-commerce platform run.

## 1. The Core Languages & Frameworks

### Java & Spring Boot
**What is it?** Java is the programming language used for the "backend" (the invisible brain of the application). Spring Boot is a massive toolkit (a framework) built on top of Java that makes creating web applications much faster. 
**Why use it?** Building everything from scratch in Java takes forever. Spring Boot provides pre-built pieces for common tasks: connecting to databases, creating web addresses (like `/api/products`), and handling security. It's the industry standard for building robust, enterprise-level backends.
**Library Usages:**
- **Spring Web:** To create the REST APIs (the URLs the frontend talks to).
- **Spring Data MongoDB:** To easily talk to our MongoDB database without writing complex database queries.
- **Spring Security (with JWT):** To lock down the application. It ensures only logged-in users can buy things or access their profiles. JWT (JSON Web Tokens) are like digital VIP passes that users get when they log in.

### Angular
**What is it?** Angular is a framework maintained by Google for building the "frontend" (the visual part of the website you interact with in your browser). It's built using HTML, CSS, and TypeScript (a stricter version of JavaScript).
**Why use it?** Instead of building one giant, hard-to-maintain webpage, Angular lets us build "Components" (like Lego blocks). We have a `ProductList` block, a `ShoppingCart` block, and a `NavigationBar` block. We assemble these blocks to create complex, dynamic pages that update without needing to refresh the whole browser tab.
**Library Usages:**
- **RxJS:** Used extensively for handling asynchronous data (like waiting for a product list to load from the backend).
- **Angular Router:** Handles navigating between pages (e.g., going from `/home` to `/cart`).

## 2. The Microservices Infrastructure

Instead of having one massive application doing everything (a monolith), this project uses a **Microservices Architecture**. This means the application is split into smaller, independent mini-applications (services) that talk to each other.

### Spring Cloud Gateway (API Gateway)
**What is it?** Think of it as the receptionist or front door for the entire backend.
**Why use it?** When the Angular frontend wants data, it doesn't need to know the exact address of the Product Service or the Order Service. It just asks the API Gateway, and the Gateway routes the request to the correct mini-application.

### Netflix Eureka (Discovery Server)
**What is it?** A phonebook for our microservices.
**Why use it?** Microservices might change their internal addresses. The Discovery Server keeps a live registry of every service that is currently running. When the API Gateway needs to talk to the Product Service, it checks the Discovery Server to find out where the Product Service is right now.

## 3. Communication & Data Storage

### MongoDB
**What is it?** A NoSQL database. Unlike traditional databases with rigid tables and rows (like Excel), MongoDB stores data in flexible, document-like formats (similar to JSON).
**Why use it?** It's incredibly fast and flexible. If we want to add a new feature to a product (like a "discount" field), we don't have to redesign the whole database structure. It fits perfectly with modern web applications.

### Apache Kafka & Zookeeper
**What is it?** Kafka is an event streaming platform. Think of it as a highly reliable, super-fast digital post office or message broker. Zookeeper is Kafka's manager—it keeps Kafka organized and running smoothly.
**Why use it?** In microservices, services need to talk to each other without being tightly connected. For example, when an order is placed (Order Service), the product inventory needs to decrease (Product Service). Instead of the Order Service calling the Product Service directly (which could fail if the Product Service is temporarily down), the Order Service shouts a message into Kafka: "Order placed!". The Product Service listens to Kafka, hears the message, and updates the inventory when it's ready.

## 4. DevOps & Deployment (The Engine Room)

### Docker & Docker Compose
**What is it?** Docker packages an application and everything it needs to run (libraries, tools, code) into a standardized unit called a "Container". Docker Compose is a tool that lets us start multiple containers at once with a single command.
**Why use it?** "It works on my machine" is a classic developer problem. Docker solves this. If it runs in a Docker container on a developer's laptop, it will run exactly the same way on a production server. Docker Compose allows us to spin up the database, Kafka, and all 6 microservices effortlessly.

### Jenkins
**What is it?** An automation server used for CI/CD (Continuous Integration / Continuous Deployment). 
**Why use it?** When a developer writes new code, Jenkins automatically downloads it, tests it to make sure nothing broke, packages it into a new Docker container, and gets it ready to deploy. It acts as our automated quality control and delivery robot.

### SonarQube (with PostgreSQL)
**What is it?** A code quality and security scanner. It uses a PostgreSQL database to store its findings.
**Why use it?** Jenkins tells SonarQube to scan the code. SonarQube acts like an automated senior developer, looking for bugs, security vulnerabilities, or messy code (code smells). If the code is too messy, it can fail the build, ensuring only high-quality code reaches production.

### Nginx
**What is it?** A highly efficient web server and reverse proxy.
**Why use it?** In this project, Nginx sits at the very front of the architecture. It handles secure HTTPS connections (SSL termination) and serves the Angular frontend files to the user's browser incredibly fast.
