# SafeZone: Code Quality & Security Enhancement

## Project Overview

SafeZone is an automated code quality and security enhancement framework for the buy-01-dev e-commerce microservices project. It integrates **SonarQube** with **GitHub** and **GitHub Actions** to provide continuous code analysis, detect quality issues, and enforce quality gates across all microservices.

**Key Objectives:**
- Automated code quality checks using SonarQube
- GitHub integration for PR-based analysis
- Security vulnerability detection
- Continuous monitoring and compliance tracking
- Code review and approval workflows

---

## Architecture

### Components

| Component | Purpose | Details |
|-----------|---------|---------|
| **SonarCloud** | Cloud Code Quality Platform | Analyzes code and provides quality metrics |
| **GitHub Actions** | CI/CD Pipeline | Triggers analysis on push/PR events |
| **SonarQube (local)** | Local Analysis Server | Optional: for Jenkins integration |
| **Jenkins** | Local CI/CD | Optional: alternative to GitHub Actions |

### Supported Microservices

- **api-gateway**: API Gateway service (Port 8080)
- **product-service**: Product management (Port 8082)
- **media-service**: Media handling (Port 8083)
- **identity-service**: User authentication (Port 8081)
- **buy-01-frontend**: Angular frontend application

---

## Setup Instructions

### Prerequisites

- Git and GitHub account
- SonarCloud account (https://sonarcloud.io)
- GitHub repository access (Violet-Ogombo/buy-01-dev)
- Docker (for local SonarQube, optional)

### Step 1: Set Up SonarCloud

1. Sign up at https://sonarcloud.io with your GitHub account
2. Import your GitHub repository
3. Generate a user authentication token:
   - Navigate to: **Account → Security**
   - Click: **Generate Tokens**
   - Name: `GitHub Actions`
   - Copy the token (shown only once)

### Step 2: Add GitHub Secrets

1. Go to GitHub repository: https://github.com/Violet-Ogombo/buy-01-dev
2. Navigate to: **Settings → Secrets and variables → Actions**
3. Create secret: `SONAR_TOKEN`
   - Paste the token from Step 1

### Step 3: Configure SonarQube Properties

File: `sonar-project.properties` (repo root)

```properties
# Project Identification
sonar.projectKey=buy-01-dev
sonar.projectName=buy-01-dev
sonar.organization=violet-ogombo

# Code Analysis
sonar.sources=.
sonar.java.binaries=**/target/classes

# Language & Encoding
sonar.sourceEncoding=UTF-8

# Test Reports
sonar.junit.reportPaths=**/target/surefire-reports

# Exclusions
sonar.exclusions=\
  **/node_modules/**,\
  **/.git/**,\
  **/target/**,\
  **/dist/**,\
  **/.angular/**
```

### Step 4: Set Up GitHub Actions Workflow

File: `.github/workflows/sonarqube.yml`

```yaml
name: SonarQube Analysis

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  sonarqube:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: 17
          distribution: 'temurin'

      - name: Run SonarQube Analysis
        uses: SonarSource/sonarqube-scan-action@master
        env:
          SONAR_HOST_URL: https://sonarcloud.io
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_ORGANIZATION: violet-ogombo

      - name: SonarQube Quality Gate
        uses: SonarSource/sonarqube-quality-gate-action@master
        timeout-minutes: 5
        env:
          SONAR_HOST_URL: https://sonarcloud.io
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

---

## Configuration Details

### SonarQube Properties Explained

| Property | Purpose |
|----------|---------|
| `sonar.projectKey` | Unique project identifier in SonarCloud |
| `sonar.organization` | SonarCloud organization namespace |
| `sonar.sources` | Source code directory to analyze |
| `sonar.java.binaries` | Compiled Java classes location |
| `sonar.junit.reportPaths` | JUnit test results location |
| `sonar.exclusions` | Directories to skip during analysis |

### Exclusions

The following directories are **excluded** from analysis:
- `node_modules/` - Frontend dependencies
- `target/` - Maven build artifacts
- `dist/` - Build output
- `.git/` - Version control
- `.angular/` - Angular build cache

---

## GitHub Integration

### Workflow Triggers

**GitHub Actions runs automatically on:**

1. **Push to main branch**
   ```bash
   git push origin main
   ```

2. **Pull Requests to main**
   ```bash
   git checkout -b feature/my-feature
   # Make changes
   git push origin feature/my-feature
   # Create PR via GitHub UI
   ```

### PR Analysis Results

After workflow completion:
- ✅ SonarCloud analysis status posted to PR
- ✅ Quality gate results shown
- ✅ Code issues highlighted
- ❌ PR can be blocked if quality gate fails (with branch protection rules)

### Branch Protection Rules (Recommended)

1. Go to: **Settings → Branches → Branch protection rules**
2. Add rule for `main` branch
3. Require: "Status checks to pass before merging"
4. Select: SonarQube Quality Gate

---

## Code Analysis

### Analysis Scope

SonarQube analyzes:
- ✅ Java code (all microservices)
- ✅ Test coverage
- ✅ Code smells
- ✅ Security vulnerabilities
- ✅ Bugs and potential issues

### Running Analysis Locally

For **SonarCloud** (recommended):
```bash
# GitHub Actions runs automatically on push/PR
# View results at: https://sonarcloud.io/dashboard?id=buy-01-dev
```

For **local SonarQube** (optional):
```bash
# Start SonarQube
docker-compose up -d sonarqube

# Access at: http://localhost:9000
# Admin: admin / admin
```

---

## Continuous Monitoring

### SonarCloud Dashboard

Monitor code quality at: **https://sonarcloud.io/dashboard?id=buy-01-dev**

Metrics tracked:
- **Code Coverage**: Percentage of code tested
- **Technical Debt**: Time to fix all issues
- **Code Smells**: Maintainability issues
- **Bugs**: Critical issues
- **Vulnerabilities**: Security issues
- **Security Hotspots**: Potential security concerns

### Quality Gate Status

- **PASSED** ✅: Code meets quality standards
- **FAILED** ❌: Issues detected, PR blocked (with branch rules)

---

## Code Review & Approval Process

### Recommended Workflow

1. **Create Feature Branch**
   ```bash
   git checkout -b feature/description
   ```

2. **Commit Changes**
   ```bash
   git commit -m "feat: description"
   ```

3. **Push and Create PR**
   ```bash
   git push origin feature/description
   ```

4. **Wait for GitHub Actions**
   - SonarCloud analysis runs automatically
   - Results posted to PR

5. **Review SonarCloud Results**
   - Check quality gate status
   - Review code issues
   - Address findings

6. **Code Review by Teammates**
   - Team members review code
   - SonarCloud findings discussed
   - Approval required before merge

7. **Merge to Main**
   ```bash
   # Merge via GitHub UI (requires approval + quality gate)
   ```

---

## Notifications & Monitoring (Bonus)

### GitHub Notifications

- PR comments with SonarCloud results
- Commit status checks
- Branch protection rule feedback

---

## Testing & Verification

### Verify SonarCloud Integration

1. **Check Workflow Status**
   ```bash
   # On GitHub: Actions tab
   # Should show: SonarQube Analysis workflow runs
   ```

2. **Create Test PR**
   ```bash
   git checkout -b test/sonarqube-integration
   # Make small change
   git commit -m "test: SonarQube integration"
   git push origin test/sonarqube-integration
   # Create PR via GitHub
   ```

3. **Verify Results**
   - ✅ GitHub Actions runs
   - ✅ SonarCloud analysis completes
   - ✅ PR shows quality gate status
   - ✅ Issues highlighted in PR

### Local Testing (Optional)

If using local SonarQube:
```bash
# Start containers
docker-compose up -d

# Verify SonarQube is running
curl http://localhost:9000/api/system/health

# Run analysis for a service
cd product-service
mvn clean verify sonar:sonar -Dsonar.projectKey=product-service
```

---

## Resources

- [SonarQube Official Documentation](https://docs.sonarqube.org/latest/)
- [SonarCloud Documentation](https://docs.sonarcloud.io/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [SonarLint IDE Integration](https://www.sonarlint.org/)

---

**Project**: buy-01-dev  
**Framework**: SafeZone Code Quality Initiative  
**Last Updated**: May 2026
