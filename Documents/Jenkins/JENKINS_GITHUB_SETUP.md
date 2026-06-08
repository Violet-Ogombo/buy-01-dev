# Jenkins Credentials Setup - GitHub Only

## Quick Setup for Your Environment

Since everything runs in Docker (databases, services, Jenkins), you only need **GitHub SSH credentials** for Jenkins to clone your repository.

---

## Step 1: Generate SSH Key for Jenkins

Run this on your Mac:

```bash
ssh-keygen -t rsa -b 4096 -f ~/.ssh/jenkins_github -N ""
```

This creates:
- `~/.ssh/jenkins_github` (private key - for Jenkins)
- `~/.ssh/jenkins_github.pub` (public key - for GitHub)

---

## Step 2: Add Public Key to GitHub

```bash
# Copy your public key
cat ~/.ssh/jenkins_github.pub
```

1. Go to **GitHub.com**
2. Click your profile → **Settings**
3. Click **SSH and GPG keys**
4. Click **New SSH key**
5. **Title:** `Jenkins Build Server`
6. **Key:** Paste the content from `jenkins_github.pub`
7. Click **Add SSH key**

**Test it works:**
```bash
ssh -i ~/.ssh/jenkins_github -T git@github.com
# Should output: Hi your-username! You've successfully authenticated...
```

---

## Step 3: Add SSH Key to Jenkins

### 3.1 Access Jenkins Credentials Manager:
1. Go to `http://localhost:8085`
2. Click **Manage Jenkins** (left sidebar)
3. Click **Manage Credentials**
4. Click **System**
5. Click **Global credentials (unrestricted)**
6. Click **+ Add Credentials**

### 3.2 Fill in the Form:
- **Kind:** `SSH Username with private key`
- **Scope:** `Global`
- **Username:** `git`
- **Private Key:** Select "Enter directly"
  - Open this file: `~/.ssh/jenkins_github`
  - Copy and paste the ENTIRE content (including `-----BEGIN RSA PRIVATE KEY-----` and `-----END RSA PRIVATE KEY-----`)
- **ID:** `github-ssh-key`
- **Description:** `GitHub SSH Key for Jenkins`
- Leave **Passphrase** empty

8. Click **Create**

---

## Step 4: Update Your Jenkinsfile

Replace your current Jenkinsfile with this (already handles GitHub credentials):

```groovy
pipeline {
    agent any
    
    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[
                        url: 'git@github.com:YOUR_USERNAME/buy-01-dev.git',
                        credentialsId: 'github-ssh-key'
                    ]]
                ])
            }
        }
        
        stage('Build Services') {
            steps {
                echo "Building all microservices..."
                sh './docker.sh'
            }
        }
        
        stage('Run Tests') {
            steps {
                echo "Running tests on API Gateway..."
                sh 'docker exec api-gateway mvn test'
            }
        }
        
        stage('Product Service Tests') {
            steps {
                echo "Running tests on Product Service..."
                sh 'docker exec product-service mvn test'
            }
        }
    }
    
    post {
        always {
            echo "Collecting test results..."
            junit '**/target/surefire-reports/*.xml' || true
        }
        success {
            echo '✅ Build successful!'
        }
        failure {
            echo '❌ Build failed!'
        }
    }
}
```

**Important:** Replace `YOUR_USERNAME` with your actual GitHub username.

---

## Step 5: Test Jenkins Can Access GitHub

1. Go to Jenkins: `http://localhost:8085`
2. Click **New Item**
3. **Item name:** `Test-GitHub-Connection`
4. **Type:** `Pipeline`
5. Click **OK**
6. Under **Pipeline**, paste this minimal pipeline:
   ```groovy
   pipeline {
       agent any
       stages {
           stage('Test Git Access') {
               steps {
                   checkout([
                       $class: 'GitSCM',
                       branches: [[name: '*/main']],
                       userRemoteConfigs: [[
                           url: 'git@github.com:YOUR_USERNAME/buy-01-dev.git',
                           credentialsId: 'github-ssh-key'
                       ]]
                   ])
                   sh 'ls -la'
               }
           }
       }
   }
   ```
7. Click **Save**
8. Click **Build Now**

**Expected Result:**
- Build console shows successful Git clone
- No "permission denied" errors
- You see files from your repository listed

---

## Step 6: Deploy Your Real Jenkinsfile

1. Go back to your project directory
2. Verify your Jenkinsfile exists:
   ```bash
   cat Jenkinsfile
   ```

3. In Jenkins, create a new Pipeline job:
   - **Item name:** `Buy-01-Microservices`
   - **Type:** `Pipeline`
   - Under **Pipeline Definition:**
     - **Definition:** `Pipeline script from SCM`
     - **SCM:** `Git`
     - **Repository URL:** `git@github.com:YOUR_USERNAME/buy-01-dev.git`
     - **Credentials:** `github-ssh-key`
     - **Branch:** `*/main`
     - **Script Path:** `Jenkinsfile`
   - Click **Save**

4. Click **Build Now**
   - Jenkins will checkout your code
   - Read the Jenkinsfile
   - Run the pipeline stages

---

## Step 7: Verify Secrets Are Masked

After a successful build:
1. Click on the build number
2. Click **Console Output**
3. Verify:
   - ✅ Git SSH key is NOT shown
   - ✅ Private keys are masked
   - ✅ Build shows actual output (which microservices built, test results, etc.)

---

## Troubleshooting

### Error: "Permission denied (publickey)"
**Cause:** SSH key not added to GitHub or wrong credentials

**Fix:**
```bash
# Test SSH key locally
ssh -i ~/.ssh/jenkins_github -T git@github.com

# Should output: Hi your-username! You've successfully authenticated...
```

If it fails:
- [ ] Verify public key is added to GitHub (Settings → SSH Keys)
- [ ] Verify private key path is correct in Jenkins
- [ ] Try restarting Jenkins

### Error: "Could not find credential ID"
**Cause:** Credential ID doesn't match

**Fix:**
1. Go to Jenkins Credentials → check the exact ID
2. Update Jenkinsfile with correct ID
3. Rebuild

### Git clone hangs or times out
**Cause:** Network or firewall issue

**Fix:**
```bash
# Test connection from your Mac
ssh -i ~/.ssh/jenkins_github -v git@github.com
```

---

## Your Final Setup Checklist

- [ ] SSH key generated: `~/.ssh/jenkins_github`
- [ ] Public key added to GitHub
- [ ] SSH key added to Jenkins Credentials (ID: `github-ssh-key`)
- [ ] Jenkinsfile updated with your GitHub username
- [ ] Test build succeeds (Test-GitHub-Connection)
- [ ] Real pipeline job created and runs
- [ ] Secrets are masked in build logs

---

## Next Steps

After credentials are working:
1. **Set up GitHub Webhook** (auto-trigger builds on push)
2. **Configure Notifications** (email/Slack on build completion)
3. **Run Full Audit** (use JENKINS_AUDIT_CHECKLIST.md)

Would you like help with GitHub webhook setup next?
