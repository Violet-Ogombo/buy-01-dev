# Buy-01: Microservices E-Commerce Platform

A modern microservices architecture for an e-commerce platform with multiple specialized services for product management, media handling, user identity, and API orchestration.

## Architecture Overview

This project follows a **microservices architecture** with the following components:

```
┌─────────────────┐
│   Frontend      │
└────────┬────────┘
         │
    ┌────▼────────────────┐
    │   API Gateway       │
    └────┬────────────────┘
         │
    ┌────┴─────────────────┬─────────────────┬────────────────┐
    │                      │                 │                │
┌───▼──────────┐  ┌────────▼────────┐  ┌─────▼────┐  ┌────────▼──────┐
│   Identity   │  │   Discovery     │  │ Product  │  │    Media      │
│   Service    │  │    Server       │  │ Service  │  │   Service     │
└──────────────┘  └─────────────────┘  └──────────┘  └───────────────┘
```

## Services

- **api-gateway** - Central entry point for all client requests with routing and load balancing
- **discovery-server** - Service discovery and registration (Eureka)
- **identity-service** - Authentication and authorization
- **product-service** - Product catalog and management
- **media-service** - Image and file handling
- **notes** - Notes and documentation service
- **buy-01-frontend** - React/Angular frontend application

## Prerequisites

- Docker & Docker Compose
- Java 11+ (for local development without Docker)
- Maven 3.6+
- Node.js 14+ (for frontend)

## Getting Started

### 1. Generate SSL Certificates

```bash
./generate-ssl-cert.sh
```

This creates self-signed certificates for secure local communication between services.

### 2. Start Services with Docker

```bash
./docker.sh
```

This builds and starts all microservices in Docker containers.

### 3. Access the Application

- **Frontend**: http://localhost:3000
- **API Gateway**: https://localhost:8080
- **Discovery Server**: http://localhost:8761

## Local Development

To run services individually without Docker:

```bash
cd api-gateway && mvn spring-boot:run
cd discovery-server && mvn spring-boot:run
cd identity-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
cd media-service && mvn spring-boot:run
```

## SSL Certificate for Development

The `generate-ssl-cert.sh` script creates self-signed certificates. Your browser may show security warnings. To trust locally:

**macOS:**
```bash
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain keystore/ssl/server.crt
```

**Windows (PowerShell as Admin):**
```powershell
Import-Certificate -FilePath 'keystore/ssl/server.crt' -CertStoreLocation 'Cert:\LocalMachine\Root'
```

## Project Structure

```
├── api-gateway/          # API Gateway service
├── discovery-server/     # Service discovery
├── identity-service/     # Auth service
├── product-service/      # Product service
├── media-service/        # Media service
├── notes/                # Notes service
├── buy-01-frontend/      # Frontend application
├── keystore/             # SSL certificates
├── uploads/              # File storage
├── docker-compose.yml    # Docker Compose configuration
├── docker.sh             # Docker startup script
└── generate-ssl-cert.sh  # SSL certificate generator
```

## Documentation

For detailed service documentation, see individual service README files.

## License

Proprietary - All rights reserved
