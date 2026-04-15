FROM jenkins/jenkins:lts

# Switch to root to install packages
USER root

# Install Docker client and Docker Compose
RUN apt-get update && \
    apt-get install -y docker.io docker-compose-plugin && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Stay as root for socket permissions
USER root
