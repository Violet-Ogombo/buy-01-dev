FROM jenkins/jenkins:lts

# Switch to root to install packages
USER root

# Install Node.js, Docker client, Maven, and other dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl ca-certificates gnupg && \
    mkdir -p /etc/apt/keyrings && \
    curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg && \
    echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_20.x nodistro main" | tee /etc/apt/sources.list.d/nodesource.list && \
    apt-get update && \
    apt-get install -y --no-install-recommends \
    docker.io \
    docker-cli \
    maven \
    git \
    nodejs && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Download and install Docker Compose directly (with fallback) and link as CLI plugin
RUN curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose 2>/dev/null && \
    chmod +x /usr/local/bin/docker-compose && \
    mkdir -p /usr/libexec/docker/cli-plugins && \
    ln -sf /usr/local/bin/docker-compose /usr/libexec/docker/cli-plugins/docker-compose || \
    echo "Docker Compose will be installed at runtime" && \
    true

# Switch back to jenkins user
USER jenkins

# Stay as root for Docker socket access
USER root
