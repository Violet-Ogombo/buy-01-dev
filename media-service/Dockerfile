# Multi-stage build for Media Service
# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-17 as builder

WORKDIR /app

# Copy pom.xml and download dependencies (cache layer)
COPY pom.xml .
RUN mvn dependency:resolve

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# Stage 2: Runtime with minimal image
FROM eclipse-temurin:17-jre

WORKDIR /app

# Create uploads directory for media storage
RUN mkdir -p /app/uploads/images && chmod 755 /app/uploads/images

# Copy JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
    CMD wget --quiet --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# Expose port
EXPOSE 8083

# Set environment variables for JVM optimization and media storage
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0"
ENV MEDIA_UPLOAD_DIR="/app/uploads/images"

# Run application
CMD ["java", "-jar", "app.jar"]
