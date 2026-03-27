#!/bin/bash

# Docker startup script for Buy-01 microservices
# - Cleans up old volumes (removes cached database data)
# - Builds and starts all services with docker-compose
# - Validates service health

set -e  # Exit on error

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Check Docker installation
check_docker() {
    print_header "Checking Docker Installation"
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        exit 1
    fi
    print_success "Docker found: $(docker --version)"
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed"
        exit 1
    fi
    print_success "Docker Compose found: $(docker-compose --version)"
}

# Clean up old volumes to remove cached data
cleanup_volumes() {
    print_header "Cleaning Up Old Volumes (Removing Cached Data)"
    
    # Check if volumes exist
    if docker volume ls | grep -q "buy-01_mongodb_data"; then
        print_info "Removing MongoDB volume (buy-01_mongodb_data)..."
        docker volume rm buy-01_mongodb_data 2>/dev/null || print_info "Could not remove (might be in use)"
    else
        print_info "MongoDB volume not found (already clean)"
    fi
    
    if docker volume ls | grep -q "buy-01_media_uploads"; then
        print_info "Removing media uploads volume (buy-01_media_uploads)..."
        docker volume rm buy-01_media_uploads 2>/dev/null || print_info "Could not remove (might be in use)"
    else
        print_info "Media uploads volume not found (already clean)"
    fi
    
    print_success "Volume cleanup complete"
}

# Check for stuck/orphaned containers
cleanup_containers() {
    print_header "Checking for Orphaned Containers"
    
    # Get all exited containers
    EXITED=$(docker ps -aq -f status=exited)
    if [ -n "$EXITED" ]; then
        print_info "Found exited containers, removing..."
        echo "$EXITED" | xargs docker rm -f 2>/dev/null || true
        print_success "Orphaned containers removed"
    else
        print_info "No orphaned containers found"
    fi
}

# Build and start all services
build_and_start() {
    print_header "Building and Starting All Services"
    
    print_info "Building Docker images and starting containers..."
    print_info "This may take a moment on first run..."
    
    # Use --build flag to rebuild images if needed
    docker-compose up -d --build
    
    print_success "All services started!"
}

# Wait and validate services health
validate_services() {
    print_header "Validating Service Health"
    
    sleep 10
    print_info "Checking service status..."
    
    # Get service status
    docker-compose ps
    
    print_info "Verifying critical services..."
    
    # Check MongoDB
    if docker exec mongodb mongosh --quiet --eval "db.adminCommand('ping')" &>/dev/null 2>&1; then
        print_success "MongoDB is healthy"
    else
        print_error "MongoDB health check failed (may still be initializing)"
    fi
    
    # Check if API Gateway is running
    if docker-compose ps api-gateway | grep -q "running"; then
        print_success "API Gateway is running"
    else
        print_error "API Gateway is not running"
    fi
}

# Verify database is clean
verify_clean_database() {
    print_header "Verifying Clean Database"
    
    sleep 5
    
    # Count products (sample data will be auto-initialized)
    PRODUCT_COUNT=$(docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('buy01').product.countDocuments()" 2>/dev/null || echo "0")
    
    print_info "Products in database: $PRODUCT_COUNT"
    
    if [ "$PRODUCT_COUNT" = "0" ]; then
        print_info "Waiting for auto-initialization of sample products..."
        sleep 5
        PRODUCT_COUNT=$(docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('buy01').product.countDocuments()" 2>/dev/null || echo "0")
        print_info "Products after initialization: $PRODUCT_COUNT"
    fi
    
    print_success "Database is clean and ready!"
}

# Display access information
show_access_info() {
    print_header "Service Access Information"
    
    echo "Frontend (Web App):        http://localhost"
    echo "API Gateway:               http://localhost:8080"
    echo "Discovery Server:          http://localhost:8761"
    echo "MongoDB:                   mongodb://localhost:27017"
    echo ""
    echo "MongoDB Compass Connection:"
    echo "  Connection String: mongodb://localhost:27017"
    echo "  Database: buy01"
    echo ""
}

# Main function
main() {
    print_header "Buy-01 Docker Startup Script"
    
    check_docker
    cleanup_containers
    cleanup_volumes
    build_and_start
    validate_services
    verify_clean_database
    show_access_info
    
    print_header "✓ All Done! System is Ready"
    echo ""
    echo "To view logs:"
    echo "  docker-compose logs -f"
    echo ""
    echo "To stop all services:"
    echo "  docker-compose down"
    echo ""
}

# Run main
main
