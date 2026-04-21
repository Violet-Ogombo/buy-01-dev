FROM jenkins/jenkins:lts

# Switch to root to install packages
USER root

# Install Docker client, Maven, and other dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    docker.io \
    maven \
    curl \
    git && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Download and install Docker Compose directly (with fallback)
RUN curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose 2>/dev/null && \
    chmod +x /usr/local/bin/docker-compose || echo "Docker Compose will be installed at runtime" && \
    true

# Switch back to jenkins user
USER jenkins

# Stay as root for Docker socket access
USER root
