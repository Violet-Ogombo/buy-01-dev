# Jenkins Credentials Setup Guide - Buy-01 Microservices

## Overview
This guide walks you through setting up credentials in Jenkins for your CI/CD pipeline to authenticate with Git, Docker, and other services without hardcoding secrets.

---

## Prerequisites
- Jenkins running at `http://localhost:8085`
- Admin access to Jenkins
- Git repository credentials (SSH key or personal access token)
- Docker registry credentials (if pushing to registry)

---

## Step-by-Step Credential Setup

### 1. Access Jenkins Credentials Manager

1. Open Jenkins: `http://localhost:8085`
2. Click **Manage Jenkins** (left sidebar)
3. Click **Manage Credentials**
4. Click **System** (under "Stores scoped to Jenkins")
5. Click **Global credentials (unrestricted)**

---

### 2. Add Git Repository Credentials

#### Option A: SSH Key (Recommended for GitHub)

**Step 1: Generate SSH Key (if you don't have one)**
```bash
ssh-keygen -t rsa -b 4096 -f ~/.ssh/jenkins_github
# Press Enter when prompted for passphrase (leave empty)
```

**Step 2: Add Public Key to GitHub**
```bash
cat ~/.ssh/jenkins_github.pub
```
- Go to GitHub.com → Settings → SSH and GPG keys → **New SSH key**
- Paste the content from `jenkins_github.pub`
- Click **Add SSH key**

**Step 3: Add Private Key to Jenkins**
1. In Jenkins Credentials Manager, click **+ Add Credentials**
2. **Kind:** `SSH Username with private key`
3. **Scope:** `Global`
4. **Username:** `git` (for GitHub) or your GitLab username
5. **Private Key:** Select "Enter directly"
6. Paste content from `~/.ssh/jenkins_github` (private key)
7. **ID:** `github-ssh-key` (you'll reference this in Jenkinsfile)
8. **Description:** `GitHub SSH Key for Jenkins`
9. Click **Create**

#### Option B: Personal Access Token (GitHub)

1. Go to GitHub.com → Settings → **Developer settings** → **Personal access tokens**
2. Click **Generate new token**
3. Select scopes: `repo`, `admin:repo_hook`
4. Copy the token
5. In Jenkins Credentials Manager, click **+ Add Credentials**
6. **Kind:** `Username with password`
7. **Username:** `your-github-username`
8. **Password:** Paste your GitHub token
9. **ID:** `github-credentials`
10. **Description:** `GitHub Personal Access Token`
11. Click **Create**

---

### 3. Add Docker Registry Credentials

If you're pushing Docker images to a registry (Docker Hub, ECR, etc.):

1. In Jenkins Credentials Manager, click **+ Add Credentials**
2. **Kind:** `Username with password`
3. **Scope:** `Global`
4. **Username:** Your Docker registry username
5. **Password:** Your Docker registry password (or token)
6. **ID:** `docker-registry-credentials`
7. **Description:** `Docker Hub/Registry Credentials`
8. Click **Create**

#### For Docker Hub:
- **Username:** Your Docker Hub username
- **Password:** Your Docker Hub access token (Settings → Security → New Access Token)

---

### 4. Add MongoDB Credentials (if needed)

1. Click **+ Add Credentials**
2. **Kind:** `Username with password`
3. **Username:** `admin` (MongoDB admin user)
4. **Password:** Your MongoDB password
5. **ID:** `mongodb-credentials`
6. **Description:** `MongoDB Admin Credentials`
7. Click **Create**

---

### 5. Add API Keys & Secrets

For any other sensitive data (JWT secrets, API keys, etc.):

1. Click **+ Add Credentials**
2. **Kind:** `Secret text`
3. **Scope:** `Global`
4. **Secret:** Paste your secret value
5. **ID:** `jwt-secret` (or appropriate name)
6. **Description:** `JWT Secret Key`
7. Click **Create**

---

## Summary of Credentials to Create

| ID | Type | Value | Used For |
|---|---|---|---|
| `github-ssh-key` | SSH Key | Private key from ~/.ssh/jenkins_github | Git repository checkout |
| `docker-registry-credentials` | Username/Password | Docker Hub credentials | Push Docker images |
| `mongodb-credentials` | Username/Password | MongoDB admin credentials | Database access |
| `jwt-secret` | Secret Text | JWT secret from environment | Microservice authentication |

---

## Update Jenkinsfile to Use Credentials

Once credentials are created, update your Jenkinsfile to use them:

```groovy
pipeline {
    agent any
    
    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    
    environment {
        // Reference credentials by ID
        DOCKER_CREDENTIALS = credentials('docker-registry-credentials')
        JWT_SECRET = credentials('jwt-secret')
    }
    
    stages {
        stage('Checkout') {
            steps {
                // Use SSH key for Git clone
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[
                        url: 'git@github.com:your-username/buy-01-dev.git',
                        credentialsId: 'github-ssh-key'
                    ]]
                ])
            }
        }
        
        stage('Build Services') {
            steps {
                sh './docker.sh'
            }
        }
        
        stage('Login to Docker Registry') {
            steps {
                sh '''
                    echo $DOCKER_CREDENTIALS_PSW | docker login -u $DOCKER_CREDENTIALS_USR --password-stdin
                '''
            }
        }
        
        stage('Run Tests') {
            steps {
                sh 'docker exec api-gateway mvn test'
            }
        }
    }
    
    post {
        always {
            junit '**/target/surefire-reports/*.xml'
            // Logout from Docker
            sh 'docker logout || true'
        }
        success {
            echo 'Build successful!'
        }
        failure {
            echo 'Build failed!'
        }
    }
}
```

---

## Verify Credentials are Working

### Test Git Access:
```bash
# In Jenkins, run a test build
# If checkout succeeds, Git credentials are working
```

### Test Docker Credentials:
1. Add a stage in Jenkinsfile:
```groovy
stage('Test Docker Login') {
    steps {
        withCredentials([usernamePassword(
            credentialsId: 'docker-registry-credentials',
            usernameVariable: 'DOCKER_USER',
            passwordVariable: 'DOCKER_PASS'
        )]) {
            sh '''
                echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                docker logout
            '''
        }
    }
}
```

2. Run build and verify Docker login succeeds

---

## Security Best Practices

✅ **DO:**
- [ ] Store all secrets as Jenkins Credentials
- [ ] Use SSH keys for Git (more secure than passwords)
- [ ] Use personal access tokens instead of passwords for APIs
- [ ] Rotate credentials every 90 days
- [ ] Restrict credential access in Jenkins security config
- [ ] Use masked variables in logs (Jenkins does this automatically for `credentials()`)

❌ **DON'T:**
- [ ] Hardcode secrets in Jenkinsfile
- [ ] Hardcode secrets in docker-compose.yml
- [ ] Hardcode secrets in application configuration files
- [ ] Store credentials in version control
- [ ] Share credentials via Slack/email
- [ ] Use personal passwords (use tokens instead)

---

## Next Steps

1. ✅ Create credentials in Jenkins (this guide)
2. ⏭️ Update Jenkinsfile to use credentials
3. ⏭️ Test the pipeline with a manual build trigger
4. ⏭️ Set up Git webhook for automatic triggering
5. ⏭️ Configure notifications (email/Slack)

---

## Troubleshooting

### Git Clone Fails
**Error:** `Permission denied (publickey)`
- [ ] Verify SSH key is added to GitHub
- [ ] Verify SSH key path is correct in Jenkins
- [ ] Test SSH key locally: `ssh -i ~/.ssh/jenkins_github -T git@github.com`

### Docker Login Fails
**Error:** `invalid username/password`
- [ ] Verify Docker credentials are correct
- [ ] Check if access token (not password) is being used
- [ ] Verify credentials are masked in Jenkins logs

### Build log shows secrets
**If you see actual values instead of ****:**
- [ ] Ensure you're using `credentials()` in Jenkinsfile
- [ ] Don't echo secrets directly with `echo $SECRET`
- [ ] Use Jenkins credential binding: `withCredentials([])`

---

## Reference: Jenkinsfile Credential Syntax

```groovy
// Method 1: Environment variables (automatically masked)
environment {
    MY_CREDS = credentials('credential-id')
}

// Method 2: With credentials wrapper
withCredentials([
    usernamePassword(
        credentialsId: 'docker-registry-credentials',
        usernameVariable: 'DOCKER_USER',
        passwordVariable: 'DOCKER_PASS'
    )
]) {
    sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
}

// Method 3: For Git checkout
checkout([
    $class: 'GitSCM',
    userRemoteConfigs: [[
        url: 'git@github.com:user/repo.git',
        credentialsId: 'ssh-key-id'
    ]]
])

// Method 4: SSH key access
sshagent(['jenkins-ssh-key']) {
    sh 'ssh -T git@github.com'
}
```

---

## Checklist for Completion

- [ ] SSH key generated and added to GitHub
- [ ] Git credentials created in Jenkins
- [ ] Docker registry credentials created (if using)
- [ ] Jenkinsfile updated to use credentials
- [ ] Build triggered manually to test
- [ ] Git checkout succeeds (no auth errors)
- [ ] Docker login succeeds (if applicable)
- [ ] Secrets are masked in build logs
