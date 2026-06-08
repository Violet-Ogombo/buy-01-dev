# Jenkins & SonarQube Integration Setup Guide

This guide explains how to configure Jenkins and SonarQube to work together for continuous code quality analysis.

---

## Overview

**Goal:** When Jenkins builds your code, it automatically sends the analysis to SonarQube for quality checks.

**Flow:**
```
Jenkins Build Triggered
    ↓
Jenkins runs tests & builds code
    ↓
Jenkins runs SonarQube scanner
    ↓
Analysis results sent to SonarQube (http://localhost:9000)
    ↓
Quality Gate checks if code meets standards
    ↓
Jenkins passes/fails based on quality gate result
```

---

## Jenkins Setup

### Step 1: Install SonarQube Plugin

1. Go to Jenkins: `http://localhost:8085`
2. Click **Manage Jenkins** (left sidebar)
3. Click **Manage Plugins**
4. Search for: `SonarQube Scanner`
5. Check the checkbox next to it
6. Click **Install without restart** or **Install and restart Jenkins when job is idle**
7. Wait for installation to complete

### Step 2: Add SonarQube Server in Jenkins Config

1. Go to **Manage Jenkins** → **Configure System**
2. Scroll down to find **SonarQube Servers** section
3. Click **Add SonarQube**
   - **Name:** `SonarQube` (or your preferred name)
   - **Server URL:** `http://sonarqube:9000` (using Docker network name since they're on same network)
   - **Server authentication token:** (leave blank for now - will set after creating SonarQube token)
4. Click **Save**

### Step 3: Add SonarQube Credentials to Jenkins

1. Go to **Manage Jenkins** → **Manage Credentials**
2. Click **System** (left sidebar)
3. Click **Global credentials (unrestricted)**
4. Click **Add Credentials** (left sidebar)
5. Fill in:
   - **Kind:** Select `Secret text`
   - **Secret:** (paste the SonarQube token - will generate in next section)
   - **ID:** `sonar-token`
   - **Description:** `SonarQube Token`
6. Click **Create**

### Step 4: Update SonarQube Server with Token

1. Go to **Manage Jenkins** → **Configure System**
2. Find **SonarQube Servers** section
3. Click on the SonarQube server you created
4. In **Server authentication token** field, paste the token (or select from credentials dropdown)
5. Click **Save**

---

## SonarQube Setup

### Step 1: Access SonarQube

1. Go to: `http://localhost:9000`
2. Login with default credentials:
   - **Username:** `admin`
   - **Password:** `admin`
3. ⚠️ **Important:** You should change the admin password after first login

### Step 2: Create SonarQube Token for Jenkins

1. Click your **profile icon** in top-right corner
2. Click **My Account**
3. Click **Security** tab
4. Under **Tokens** section, enter:
   - **Token name:** `jenkins-token`
   - **Expires in:** `365` (days) or select `Never` for no expiration
5. Click **Generate**
6. **Copy the token** - you'll need it for Jenkins
7. Save it somewhere safe (you won't be able to see it again)

### Step 3: Add Token to Jenkins Credentials

1. Go back to Jenkins
2. Go to **Manage Jenkins** → **Manage Credentials** → **System** → **Global credentials**
3. Click **Add Credentials**
4. Fill in:
   - **Kind:** `Secret text`
   - **Secret:** Paste the token you copied from SonarQube
   - **ID:** `sonar-token`
   - **Description:** `SonarQube Token`
5. Click **Create**

### Step 4: Configure SonarQube Server URL in Jenkins

1. Go to **Manage Jenkins** → **Configure System**
2. Find **SonarQube Servers** section
3. Click the SonarQube server you added
4. In **Server authentication token** dropdown, select `sonar-token` (the credential you just created)
5. Click **Save**

### Step 5: Create Quality Gates (Optional but Recommended)

Quality Gates define the rules your code must pass.

1. In SonarQube, go to **Administration** (left sidebar)
2. Click **Quality Gates**
3. Click **Create**
4. Enter name: `Buy-02 Standards`
5. Click **Create**
6. Add conditions:
   - Click **Add Condition**
   - **Metric:** Select `Coverage`
   - **Operator:** `is greater than`
   - **Value:** `80`
   - Click **Add**
   - Repeat for:
     - `Duplications <= 5%`
     - `Security Rating = A`
     - `Reliability Rating = A`
     - `Maintainability Rating = A`
7. Click **Set as default** button
8. Save

---

## Jenkinsfile Configuration

Add these stages to your `Jenkinsfile`:

```groovy
pipeline {
    agent any
    
    environment {
        SONAR_HOST_URL = 'http://sonarqube:9000'
        SONAR_TOKEN = credentials('sonar-token')
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/buy-02']],
                    userRemoteConfigs: [[
                        url: 'git@github.com:Violet-Ogombo/buy-01-dev.git',
                        credentialsId: 'github-ssh-key'
                    ]]
                ])
            }
        }
        
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        
        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                echo '🔍 Running SonarQube Analysis...'
                sh '''
                    mvn clean org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                        -Dsonar.projectKey=buy-02-backend \
                        -Dsonar.projectName="Buy-02 Backend" \
                        -Dsonar.host.url=${SONAR_HOST_URL} \
                        -Dsonar.login=${SONAR_TOKEN}
                '''
            }
        }
        
        stage('Quality Gate') {
            steps {
                echo '⏳ Waiting for Quality Gate results...'
                script {
                    timeout(time: 5, unit: 'MINUTES') {
                        // Set abortPipeline: true to fail the build if gate fails
                        // Set abortPipeline: false to only warn
                        waitForQualityGate abortPipeline: false
                    }
                }
            }
        }
    }
    
    post {
        always {
            echo 'Build finished'
        }
        success {
            echo '✅ Pipeline succeeded!'
        }
        failure {
            echo '❌ Pipeline failed!'
        }
    }
}
```

**Key points:**
- `mvn clean` - removes previous build artifacts
- `sonar-maven-plugin:sonar` - Maven SonarQube plugin
- `-Dsonar.projectKey` - unique identifier for your project in SonarQube
- `-Dsonar.host.url` - where SonarQube is running
- `-Dsonar.login` - the token for authentication
- `waitForQualityGate` - checks if quality gate passed

---

## Testing the Integration

### Test Manually

1. Open Jenkins: `http://localhost:8085`
2. Create or select a pipeline job
3. Click **Build Now**
4. Watch the console output - you should see:
   ```
   🔍 Running SonarQube Analysis...
   [INFO] Scanning for projects...
   [INFO] SonarScanner 4.x
   [INFO] Analyzing...
   [INFO] SUCCESS
   ```
5. After build completes, go to SonarQube: `http://localhost:9000`
6. You should see your project listed with analysis results

### Expected Results in SonarQube

After successful build, you'll see:
- **Project name:** Buy-02 Backend
- **Code metrics:**
  - Lines of code
  - Code coverage %
  - Bugs found
  - Security vulnerabilities
  - Code smells
  - Duplications

---

## Troubleshooting

### "Failed to connect to SonarQube server"

**Cause:** Jenkins can't reach SonarQube at `http://sonarqube:9000`

**Solutions:**
1. Check SonarQube is running: `docker compose ps | grep sonarqube`
2. Verify URL in Jenkinsfile uses `http://sonarqube:9000` (internal Docker network name)
3. Check Jenkins and SonarQube are on same Docker network: `docker compose` config

### "Invalid token"

**Cause:** SonarQube token is wrong or expired

**Solutions:**
1. Generate new token in SonarQube: **My Account** → **Security** → **Generate**
2. Copy new token to Jenkins credentials: **Manage Credentials** → update `sonar-token`
3. Restart Jenkins or re-run build

### "Quality Gate failed"

**Cause:** Your code doesn't meet quality standards

**Solutions:**
1. Check SonarQube dashboard for issues
2. Fix the issues in your code
3. Re-run the build
4. Lower quality gate standards if they're too strict (edit Quality Gate)

### "SonarQube not initializing"

**Cause:** Database connection issues

**Solutions:**
```bash
# Check logs
docker compose logs sonarqube | tail -50

# Restart SonarQube and database
docker compose restart sonarqube sonarqube-db

# Or full restart
docker compose down
docker compose up -d
```

---

## Commands Reference

### Start Services
```bash
docker compose up -d
```

### Check Service Status
```bash
docker compose ps
```

### View Logs
```bash
# SonarQube logs
docker compose logs sonarqube -f

# Jenkins logs
docker compose logs jenkins -f

# All services
docker compose logs -f
```

### Access Services
- **Jenkins:** `http://localhost:8085`
- **SonarQube:** `http://localhost:9000`
- **MongoDB:** `mongodb://localhost:27017/buy01`
- **Kafka:** `kafka:9092` (internal) or `localhost:9092` (external)

---

## Summary Checklist

### Jenkins
- [ ] SonarQube Scanner plugin installed
- [ ] SonarQube server added in Configure System
- [ ] Credentials added (`sonar-token`)
- [ ] SonarQube token configured in server settings

### SonarQube
- [ ] Logged in (admin/admin)
- [ ] Token created (`jenkins-token`)
- [ ] Quality Gates configured (optional)

### Code
- [ ] Jenkinsfile updated with SonarQube stages
- [ ] Maven pom.xml has jacoco plugin for coverage
- [ ] Tests are passing locally

### Testing
- [ ] Build triggered in Jenkins
- [ ] SonarQube Analysis stage executes
- [ ] Results appear in SonarQube dashboard
- [ ] Quality Gate passes or fails correctly

---

## Next Steps

1. **Configure for your project:** Update `sonar.projectKey` and `sonar.projectName` in Jenkinsfile to match your project
2. **Set coverage requirements:** Add JaCoCo plugin to pom.xml for code coverage tracking
3. **Configure frontend scanning:** If using Angular/TypeScript, install `sonar-scanner` CLI
4. **Enable webhooks:** Set up GitHub webhook to trigger Jenkins on push
5. **Monitor quality:** Regularly check SonarQube dashboard and fix issues

---

**Last Updated:** May 26, 2026
**Version:** 1.0
