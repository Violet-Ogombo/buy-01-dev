# Discovery Server Low-Level Deep Dive

The Discovery Server is built using **Spring Cloud Netflix Eureka Server**. It acts as a service registry where all other microservices (identity, product, order, media, and api-gateway) register themselves upon startup. This enables dynamic service discovery, preventing the need to hardcode service IP addresses.

---

## 1. Application Configuration (`application.yml`)
Path: [application.yml](file:///Users/aung.min/Desktop/github/buy-01-dev/discovery-server/src/main/resources/application.yml)

```yaml
server:
  port: 8761

spring:
  application:
    name: discovery-server

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

### Explanation of Properties:
*   **`server.port: 8761`**: Configures the HTTP server port to `8761`, which is the standard default port for Netflix Eureka.
*   **`spring.application.name: discovery-server`**: Identifies the application name in the Spring Cloud ecosystem.
*   **`eureka.client.register-with-eureka: false`**: Tells this instance not to register itself as a client in the Eureka registry. Since this application is the registry server itself, it has no need to register.
*   **`eureka.client.fetch-registry: false`**: Tells this instance not to fetch registry information from other Eureka instances. In a clustered Eureka setup, this would be set to `true` to sync registries, but here in a single-instance development setup, it is set to `false`.

---

## 2. Code Analysis

### `DiscoveryServerApplication.java`
Path: [DiscoveryServerApplication.java](file:///Users/aung.min/Desktop/github/buy-01-dev/discovery-server/src/main/java/com/example/discovery/DiscoveryServerApplication.java)

#### Class Annotations:
*   `@SpringBootApplication`: Marks this class as the primary configuration class for the Spring Boot application. It enables component scanning, autoconfiguration, and property support.
*   `@EnableEurekaServer`: Activates the Eureka Server configuration, turning this Spring Boot application into a registry server.

#### Methods:
*   `public static void main(String[] args)`:
    *   **Parameters**: `String[] args` - Command-line arguments.
    *   **Return Type**: `void`
    *   **Implementation**: Calls `SpringApplication.run(DiscoveryServerApplication.class, args)` to bootstrap the Spring context, load configurations, and launch the embedded Tomcat container.

---

## 3. Test Coverage
There are no test files in `discovery-server`, as it only contains boilerplate bootstrapping code.
