# Jenkins CI/CD Audit Checklist - Buy-01 Microservices

## Pre-Audit Setup
- [ ] Log into Jenkins at `http://localhost:8085`
- [ ] Verify all Docker containers are running: `docker ps`
- [ ] Confirm microservices are healthy (discovery-server, api-gateway, etc.)

---

## 1. FUNCTIONAL TESTING

### 1.1 Pipeline Setup & Execution
**Status:** ⚠️ **NOT YET IMPLEMENTED** - No Jenkinsfile found in project
- [ ] **Create a Jenkinsfile** at project root with pipeline stages:
  - [ ] Checkout source code
  - [ ] Build each microservice
  - [ ] Run unit tests
  - [ ] Build Docker images
  - [ ] Deploy to containers
  - [ ] Run integration tests
  
**Test:** After creating Jenkinsfile:
- [ ] Manually trigger a build in Jenkins
- [ ] **Verify pipeline runs successfully from start to finish**
- [ ] All stages complete without errors
- [ ] Build artifacts are generated

### 1.2 Error Handling & Resilience
**Test Steps:**
1. [ ] Introduce intentional error in one of the services (e.g., syntax error in pom.xml)
2. [ ] Trigger Jenkins build
3. **Verify Expected Behavior:**
   - [ ] Build fails at appropriate stage
   - [ ] Error message is clear and informative
   - [ ] Build log shows exact failure point
   - [ ] Subsequent stages are skipped (not executed)
   - [ ] Email/notification sent (if configured)

### 1.3 Automated Testing
**Test Steps:**
1. [ ] Check if test execution is in the pipeline:
   - [ ] Unit tests run: `mvn test`
   - [ ] Integration tests run
   - [ ] Frontend tests run (if applicable)

2. [ ] Verify test failure behavior:
   - [ ] Create a failing test case
   - [ ] Commit and push code
   - [ ] Trigger build
   - **Verify:**
     - [ ] Tests execute automatically
     - [ ] Pipeline **halts on test failure**
     - [ ] Failed tests are reported in Jenkins UI
     - [ ] Build marked as FAILED (not SUCCESS)

### 1.4 Automatic Pipeline Triggering
**Status:** ⚠️ **NOT YET TESTED**

**Test Steps:**
1. [ ] Configure webhook in Git repository (GitHub/GitLab):
   - [ ] Jenkins webhook URL: `http://localhost:8085/github-webhook/`
   - [ ] Configure payload URL in Git settings
   - [ ] Test webhook delivery

2. [ ] Test automatic trigger:
   - [ ] Make a minor code change (e.g., update README)
   - [ ] Commit: `git add . && git commit -m "Test commit"`
   - [ ] Push: `git push origin main`
   - [ ] **Verify:**
     - [ ] Jenkins automatically detects the push
     - [ ] New build is triggered automatically (within 5 seconds)
     - [ ] No manual build trigger needed

### 1.5 Deployment & Rollback
**Test Steps:**
1. [ ] Verify deployment stage in pipeline:
   - [ ] Build succeeds
   - [ ] Docker images created: `docker ps | grep buy-01-dev`
   - [ ] Containers updated with new code
   - [ ] Services remain healthy

2. [ ] Test rollback strategy:
   - [ ] Deploy version 1.0
   - [ ] Deploy version 1.1 (with intentional issue)
   - [ ] Verify Jenkins can rollback to 1.0
   - [ ] **Verify:**
     - [ ] Rollback script exists in pipeline
     - [ ] Previous version is accessible
     - [ ] Minimal downtime during rollback
     - [ ] Data integrity maintained

---

## 2. SECURITY TESTING

### 2.1 Jenkins Permissions & Access Control
**Test Steps:**
1. [ ] Go to **Manage Jenkins** → **Manage Users**
   - [ ] Verify user list (should only have admin and necessary users)
   - [ ] No default credentials exist

2. [ ] Test Permission Levels:
   - [ ] Create a test user role with **limited permissions**
   - [ ] Test user can view jobs but cannot modify
   - [ ] Test user cannot change Jenkins configuration
   - **Verify:**
     - [ ] Role-Based Access Control (RBAC) is configured
     - [ ] Unauthorized access is prevented
     - [ ] Audit logs show permission checks

3. [ ] Test admin access:
   - [ ] Only authorized admins can modify jobs
   - [ ] Only authorized admins can access Jenkins configuration
   - [ ] Password policy is enforced (minimum complexity)

### 2.2 Sensitive Data Management
**Test Steps:**
1. [ ] Check **Manage Jenkins** → **Manage Credentials**
   - [ ] All sensitive data stored as Jenkins Secrets
   - [ ] Verify:
     - [ ] Git credentials (SSH keys, tokens)
     - [ ] Docker registry credentials
     - [ ] API keys (Kafka, MongoDB, etc.)
     - [ ] Database passwords
     - [ ] JWT secrets

2. [ ] **Verify NO hardcoded secrets in:**
   - [ ] Jenkinsfile ❌ Should NOT contain passwords
   - [ ] docker-compose.yml ❌ Should NOT contain passwords
   - [ ] application.yml files ❌ Should NOT contain passwords

3. [ ] Environment Variables Security:
   - [ ] Secrets are injected via Jenkins credentials binding
   - [ ] Secrets are masked in build logs (****)
   - [ ] Build logs are not accessible to unauthorized users

4. [ ] **Test Secret Masking:**
   - [ ] Run a build
   - [ ] Output any secret in the build log: `echo ${SOME_SECRET}`
   - [ ] **Verify:** Secret appears as `****` in Jenkins UI

---

## 3. CODE QUALITY & STANDARDS

### 3.1 Jenkinsfile Organization & Best Practices
**Status:** ⚠️ **NOT YET CREATED**

**Checklist for Jenkinsfile:**
```
- [ ] Jenkinsfile exists at project root
- [ ] Uses declarative pipeline syntax (not scripted)
- [ ] Clear stage names that describe what they do
- [ ] Proper error handling (try-catch or post{})
- [ ] Environment variables defined at top
- [ ] No hardcoded values
- [ ] Comments explaining complex logic
- [ ] Follows Jenkins best practices
- [ ] Version controlled (in Git)
```

**Example Structure:**
```groovy
pipeline {
    agent any
    
    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    
    environment {
        REGISTRY = 'your-registry'
        DOCKER_CREDENTIALS = credentials('docker-creds')
    }
    
    stages {
        stage('Checkout') { ... }
        stage('Build') { ... }
        stage('Test') { ... }
        stage('Deploy') { ... }
    }
    
    post {
        always { ... cleanup ... }
        success { ... notify ... }
        failure { ... notify ... }
    }
}
```

### 3.2 Test Reports
**Test Steps:**
1. [ ] After a successful build, check **Build Artifacts**:
   - [ ] Test reports are generated
   - [ ] Coverage reports are available
   - [ ] Reports are stored for historical reference

2. [ ] Verify report formats:
   - [ ] JUnit XML reports for Java tests
   - [ ] Coverage reports (target/site/jacoco/)
   - [ ] Test results visible in Jenkins UI
   - [ ] Trending graphs show test health over time

3. [ ] **Verify Reports Are Comprehensive:**
   - [ ] Total tests passed/failed
   - [ ] Code coverage percentage
   - [ ] Failed test details with stack traces
   - [ ] Performance metrics (if applicable)

### 3.3 Notifications & Alerting
**Test Steps:**
1. [ ] **Check Notification Configuration:**
   - [ ] Go to **Manage Jenkins** → **Configure System**
   - [ ] Verify email/Slack/Teams integration is configured

2. [ ] **Test Email Notifications:**
   - [ ] Trigger a successful build
     - [ ] **Verify:** Notification sent to team
     - [ ] **Verify:** Notification includes build status and link
   - [ ] Trigger a failed build
     - [ ] **Verify:** Failure alert sent
     - [ ] **Verify:** Alert includes failure details
     - [ ] **Verify:** Alert includes link to fix logs

3. [ ] **Verify Notification Content:**
   - [ ] Build number and status
   - [ ] Changes included in build
   - [ ] Link to Jenkins job
   - [ ] Link to build logs
   - [ ] Failure details (if failed)
   - [ ] Estimated fix time (if available)

4. [ ] **Test on Different Events:**
   - [ ] Build started
   - [ ] Build succeeded
   - [ ] Build failed
   - [ ] Build unstable (test warnings)

---

## Audit Summary Template

### Results

**Functional Testing:**
- Pipeline Execution: [ ] PASS [ ] FAIL [ ] NOT TESTED
- Error Handling: [ ] PASS [ ] FAIL [ ] NOT TESTED
- Automated Testing: [ ] PASS [ ] FAIL [ ] NOT TESTED
- Auto-Trigger (Git Push): [ ] PASS [ ] FAIL [ ] NOT TESTED
- Deployment: [ ] PASS [ ] FAIL [ ] NOT TESTED
- Rollback Strategy: [ ] PASS [ ] FAIL [ ] NOT TESTED

**Security Testing:**
- Permissions & Access Control: [ ] PASS [ ] FAIL [ ] NOT TESTED
- Sensitive Data Management: [ ] PASS [ ] FAIL [ ] NOT TESTED
- Secret Masking: [ ] PASS [ ] FAIL [ ] NOT TESTED
- Credential Storage: [ ] PASS [ ] FAIL [ ] NOT TESTED

**Code Quality:**
- Jenkinsfile Organization: [ ] PASS [ ] FAIL [ ] MISSING
- Test Reports: [ ] PASS [ ] FAIL [ ] NOT TESTED
- Notifications: [ ] PASS [ ] FAIL [ ] NOT CONFIGURED

### Issues Found
1. [ ] Issue: ...
   - Severity: [ ] Critical [ ] High [ ] Medium [ ] Low
   - Resolution: ...

### Recommendations
1. [ ] Create Jenkinsfile with declarative pipeline
2. [ ] Configure Git webhook for auto-trigger
3. [ ] Set up email/Slack notifications
4. [ ] Implement RBAC for Jenkins users
5. [ ] Add rollback strategy
6. [ ] Enable code coverage reporting

---

## Next Steps
1. **Create Jenkinsfile** (most critical missing component)
2. **Set up credentials** (GitHub SSH key, Docker credentials, etc.)
3. **Configure webhooks** in Git repository
4. **Create first pipeline** and test manually
5. **Configure notifications**
6. **Implement RBAC** if multiple team members
