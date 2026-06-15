# DevOps and Infrastructure Deep Dive

This document explains the files that control how the application is built, packaged, deployed, and managed automatically.

---

## 1. The Jenkinsfile (Automated CI/CD Pipeline)

The `Jenkinsfile` is a script that tells our Jenkins automation server exactly what to do when a developer pushes new code. It creates a "Pipeline" of steps.

### High-Level Flow:
1.  **Checkout**: Downloads the latest code from GitHub.
2.  **Build & Analyze (Backend)**: Goes into each Spring Boot microservice folder (`product-service`, `order-service`, etc.) one by one. It runs `mvn clean verify` to compile the Java code and run unit tests. It also runs `sonar:sonar` to send the code to SonarQube for security and quality scanning.
3.  **Build & Test (Frontend)**: Goes into `buy-01-frontend`, runs `npm install` (to download libraries), `npm run build` (to compile the Angular code), and `npm run test` (to run frontend unit tests).
4.  **Quality Gate Check**: Jenkins pauses and waits for SonarQube's report. If SonarQube says "This code is too messy or has too many bugs" (failing the Quality Gate), the pipeline aborts.
5.  **Docker Build**: Uses `docker compose build` to package every microservice and the frontend into fresh Docker container images. *Crucially*, it first tags the currently running images as `:backup` just in case the new deployment fails.
6.  **Deploy**: Runs `docker compose up -d` to restart the application using the brand new Docker images.

### Key Snippets Explained:

```groovy
post {
    success {
        // ...
        slackSend( ... message: "*Build SUCCESS*" )
    }
    failure {
        sh '''
            docker tag buy-01-dev-$svc:backup buy-01-dev-$svc:latest
            docker compose up -d $APP_SERVICES discovery-server
        '''
        slackSend( ... message: "*Build FAILED*" )
    }
}
```
**What this does**: This is the "Post-build action". If the deployment succeeds, it sends a green success message to the `#build-notifications` Slack channel. 
If *anything* fails (tests fail, SonarQube fails, or deployment crashes), it automatically **rolls back**. It takes the `:backup` images we saved earlier, tags them as `:latest`, and restarts the server with the old, working code. It then sends a red failure alert to Slack.

---

## 2. Dockerfiles (Packaging the Application)

A `Dockerfile` is a recipe to create a Docker Image. We use a concept called **Multi-stage builds** to make our final images as small and secure as possible.

### Backend Dockerfile (e.g., `product-service/Dockerfile`)

```dockerfile
# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-17 as builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime with minimal image
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8082
CMD ["java", "-jar", "app.jar"]
```
**What this does**:
1.  **Stage 1 (`builder`)**: It starts with a heavy Linux machine that has Maven and the Java JDK installed. It copies our source code and compiles it into a single executable `.jar` file.
2.  **Stage 2 (`runtime`)**: It throws away the heavy build machine and starts a brand new, lightweight Linux machine that *only* has the Java Runtime Environment (JRE). It copies *only* the finished `.jar` file from Stage 1. 
*Why?* We don't need Maven or source code to *run* the app in production. This keeps our final image size very small and reduces security vulnerabilities.

### Frontend Dockerfile (`buy-01-frontend/Dockerfile`)

```dockerfile
# Stage 1: Build with Node
FROM node:20 as builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm install
COPY . .
RUN npm run build

# Stage 2: Runtime with Nginx
FROM nginx:1.25
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist/buy-01-frontend/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```
**What this does**:
Similar to the backend, Stage 1 uses a heavy Node.js environment to compile the Angular TypeScript code into pure, minified HTML/CSS/JavaScript.
Stage 2 uses Nginx (a super-fast web server). It takes those finished HTML/JS files from Stage 1, puts them in Nginx's hosting folder (`/usr/share/nginx/html`), and starts serving them to the internet.

---

## 3. docker-compose.yml (The Orchestrator)

While a Dockerfile builds *one* container, `docker-compose.yml` runs *all* of them together and connects them.

### Key Sections:
*   **Infrastructure Services**: Starts `zookeeper`, `kafka` (with a script to auto-create our messaging topics), and `mongodb`.
*   **Backend Microservices**: Starts `api-gateway`, `product-service`, etc. Notice the `depends_on` blocks. For example, `product-service` won't fully start until `mongodb` and `kafka` are healthy.
*   **Environment Variables**: This file passes critical configuration to the Spring Boot apps. For example, it tells the `order-service` what the `JWT_SECRET` is and what the MongoDB connection string is (`mongodb://mongodb:27017/buy01`). Notice it uses the internal Docker network name (`mongodb`), not `localhost`.
*   **Nginx Reverse Proxy**: Starts an Nginx container that maps port 80 and 443 to the outside world. It acts as the shield for the whole cluster.
*   **Volumes**: 
    ```yaml
    volumes:
      mongodb_data: /data/db
    ```
    If a Docker container crashes and restarts, it usually loses all its data. "Volumes" solve this. This tells Docker to save MongoDB's data to a persistent folder on the host machine's hard drive. Even if we delete the MongoDB container entirely and create a new one, our database records survive.
