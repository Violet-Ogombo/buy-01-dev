# buy-01-dev

A microservices e-commerce platform with a Jenkins CI/CD pipeline for testing, deployment, rollback, and Slack notifications.

## Project overview

This project is built with a microservices architecture. The main services are:

- `api-gateway` — entry point for client requests.
- `discovery-server` — service discovery.
- `identity-service` — authentication and authorization.
- `product-service` — product management.
- `media-service` — media and file handling.
- `frontend` — user interface.
- Infrastructure services used in development include Jenkins, MongoDB, Kafka, and ZooKeeper.

## How the Jenkins pipeline works

The Jenkins pipeline is defined in the `Jenkinsfile` and runs automatically when code is pushed or when SCM polling detects a change.

### 1. Checkout
Jenkins pulls the latest code from the `main` branch of the repository.

### 2. Run tests
The pipeline runs Maven tests for the main backend services:

- API Gateway
- Product Service
- Media Service
- Identity Service

If any test fails, the pipeline stops and marks the build as failed.

### 3. Deploy application services
If the build is on the `main` branch and tests pass, Jenkins deploys only the application microservices:

- `api-gateway`
- `product-service`
- `media-service`
- `identity-service`
- `frontend`

The pipeline does **not** tear down the full environment. Shared infrastructure such as Jenkins, MongoDB, Kafka, and ZooKeeper stays running.

Before deployment, Jenkins creates backup image tags named `previous`. It then rebuilds and recreates only the application services using Docker Compose.

### 4. Smoke tests
After deployment, Jenkins performs health checks against the running services. If a health check fails, the deployment is marked as failed.

### 5. Rollback strategy
If deployment becomes unstable or fails after a new image is applied, Jenkins rolls application services back to the previously tagged images.

This rollback affects only the application services and does not remove shared infrastructure containers.

### 6. Notifications
Slack notifications are sent automatically for:

- successful builds,
- failed builds,
- rollback events.

The Slack webhook URL is stored securely in Jenkins credentials as `slack-webhook-url`.

## Triggering builds

The pipeline can start in two ways:

- GitHub webhook trigger
- SCM polling configured in the Jenkinsfile

This allows Jenkins to react automatically to repository updates.

## Test reports

JUnit test reports are collected from Maven Surefire output:

- `**/target/surefire-reports/*.xml`

These reports are stored in Jenkins and can be reviewed after each build.

## Why this pipeline setup is safer

Earlier versions of the pipeline shut down the whole Docker environment during deployment. The updated pipeline is safer because it only rebuilds the microservices that belong to the application.

This means:

- Jenkins stays online,
- MongoDB data is preserved,
- Kafka and ZooKeeper are not restarted unnecessarily,
- deployments are faster and less disruptive.

## Local development

Typical local workflow:

1. Start the full development environment with Docker.
2. Let Jenkins handle testing and redeployment of application services.
3. Monitor build logs, test reports, and Slack notifications.

## Repository structure

- `Jenkinsfile` — CI/CD pipeline definition.
- `docker-compose.yml` — local container orchestration.
- `api-gateway/` — API Gateway service.
- `discovery-server/` — discovery service.
- `identity-service/` — identity service.
- `product-service/` — product catalog service.
- `media-service/` — media handling service.
- `frontend/` — frontend application.

## Notes

This README focuses on the Jenkins pipeline and deployment flow so that reviewers can quickly understand how CI/CD is handled in the project.