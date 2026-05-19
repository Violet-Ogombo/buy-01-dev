# GitHub PR Integration with SonarQube

**Purpose:** Enable SonarQube to analyze pull requests and post code quality comments directly on GitHub PRs, with automated blocking of PRs that fail quality gates.

---

## What is a PR?

**PR** = **Pull Request**

A Pull Request is GitHub's way of proposing code changes:

1. Developer creates a branch with code changes
2. Developer pushes the branch to GitHub
3. Developer opens a PR to request merging into `main`
4. Reviewers can see and comment on the changes
5. After approval, the PR gets merged into `main`

**Visual:**
```
Your branch:        feature/add-user-login
Target branch:      main
                            ↓
                    Open Pull Request
                            ↓
                    SonarQube analyzes
                            ↓
                    Comments appear on PR
                            ↓
                    Approved & merged
```

---

## What is GitHub PR Integration?

**GitHub PR Integration** = SonarQube automatically analyzing your PR code and posting quality feedback directly on the GitHub PR page.

### Without PR Integration (Current State)

```
Developer pushes code
         ↓
Jenkins runs SonarQube analysis
         ↓
Results only visible in Jenkins UI
         ↓
Developer must check Jenkins manually
         ↓
Issues found AFTER code is merged (too late!)
```

**Problems:**
- Quality feedback not visible in GitHub
- Developers must check Jenkins separately
- Issues discovered after merge
- No automatic blocking of bad code

### With PR Integration (Recommended)

```
Developer opens PR
         ↓
SonarQube analyzes PR code immediately
         ↓
Comments posted directly on GitHub PR
         ↓
Quality Gate status displayed on PR
         ↓
Bad code blocked from merging
         ↓
Developer fixes issues before merge
```

**Benefits:**
- Instant feedback in GitHub PR
- Issues visible on exact lines
- Automated quality gate enforcement
- Better code review experience
- Prevents bad code from merging

---

## Example: SonarQube Comments on PR

When you open a PR with code quality issues, SonarQube posts comments like:

### On PR Conversation Tab:
```
🔴 Code Analysis Found Issues

Files analyzed: 4
Issues found: 8
  • 1 CRITICAL issue
  • 3 MAJOR issues
  • 4 MINOR issues

Quality Gate: FAILED ❌
  Required: 0 Critical issues
  Found: 1 Critical issue
  
Change Quality: -15%
```

### On Code Diff (Inline Comments):

**File: src/main/java/com/example/product/service/ProductService.java**

```java
Line 45:
    String query = "SELECT * FROM products WHERE id = " + productId;
    
🔴 CRITICAL: SQL Injection Vulnerability
   Severity: CRITICAL
   Issue: User input directly concatenated into SQL query
   Fix: Use parameterized queries or PreparedStatement
   SonarQube Rule: squid:S2077
```

```java
Line 67:
    public void processProduct(Product product) {
        // TODO: fix this later
        if (product != null) {
            // complex logic
        }
    }
    
⚠️ MAJOR: Complex Method
   Severity: MAJOR
   Cognitive Complexity: 12 (threshold: 10)
   Issue: Method is too complex to understand
   Fix: Break into smaller methods
```

---

## Setup Instructions

### Prerequisites
- SonarQube running in Docker (already have this ✅)
- Jenkins with Jenkinsfile (already have this ✅)
- GitHub repository admin access
- GitHub organization or personal account

---

## Step 1: Install SonarQube GitHub App

### 1.1 Access GitHub Marketplace

1. Go to **GitHub Marketplace**: https://github.com/marketplace
2. Search for: **"SonarQube"**
3. Click **"SonarQube for GitHub"** (by SonarSource - official)
4. Click **Install** button

### 1.2 Select Installation Target

1. Choose your account type:
   - Personal account: Select your username
   - Organization: Select organization name
   
2. Choose installation scope:
   - **"All repositories"** - SonarQube analyzes all your repos
   - **"Only select repositories"** - Choose specific repos (recommended)
   
3. If "Only select repositories":
   - [ ] Check `buy-01-dev`
   - Leave other repos unchecked

4. Click **Install** (green button)

### 1.3 GitHub Generates Credentials

GitHub will display:
- **App ID** (example: 123456)
- **Private Key** (large text block, starts with `-----BEGIN RSA PRIVATE KEY-----`)
- **Installation ID** (example: 12345678)

⚠️ **IMPORTANT:** Copy these credentials - you'll need them in Step 2.

---

## Step 2: Configure SonarQube Server

### 2.1 Access SonarQube Administration

1. Open SonarQube: `http://localhost:9000`
2. Log in (default: admin / admin)
3. Click **Administration** (top right corner, gear icon ⚙️)
4. Click **Integrations** (left sidebar)
5. Click **GitHub** (or search for "GitHub")

### 2.2 Enter GitHub App Credentials

Fill in the GitHub integration form:

```
GitHub API URL: https://api.github.com
(or https://your-github-enterprise-domain/api/v3 if self-hosted)

GitHub App ID: [paste from Step 1.3]

GitHub App Private Key: [paste from Step 1.3]
```

### 2.3 Save Configuration

1. Click **Save** button
2. Should show: ✅ "GitHub integration configured successfully"

If you see an error, check:
- App ID is correct
- Private key includes BEGIN/END markers
- GitHub app is installed on your repository

---

## Step 3: Update Jenkins Pipeline

Your Jenkinsfile already runs SonarQube analysis. To ensure PR analysis is enabled, verify these stages exist:

### 3.1 Verify SonarQube Analysis Stage

**Current (lines 43-75):** ✅ Already configured
```groovy
stage('SonarQube Analysis') {
    steps {
        echo "🔍 Running SonarQube Analysis on all services..."
        withSonarQubeEnv('SonarQube') {
            script {
                // Analyze services...
                dir('product-service') {
                    sh '''
                        mvn clean verify sonar:sonar \
                          -Dsonar.projectKey=product-service
                    '''
                }
            }
        }
    }
}
```

**This automatically posts to GitHub PRs when:**
- SonarQube GitHub App is installed ✅
- SonarQube server is configured with GitHub ✅
- Build is triggered from a PR branch ✅

### 3.2 Verify Quality Gate Stage

**Current (lines 84-91):** ✅ Already configured
```groovy
stage('Quality Gate') {
    steps {
        echo "🚪 Checking Quality Gate status for all projects..."
        timeout(time: 5, unit: 'MINUTES') {
            waitForQualityGate abortPipeline: true
        }
    }
}
```

**Effect:** Pipeline **fails** if Quality Gate is not passed

---

## Step 4: Configure GitHub Branch Protection

This **blocks** PRs with quality issues from being merged.

### 4.1 Access Branch Protection Settings

1. Go to GitHub repository: https://github.com/Violet-Ogombo/buy-01-dev
2. Click **Settings** (repository settings)
3. Click **Branches** (left sidebar)
4. Click **Add rule** button (under "Branch protection rules")

### 4.2 Configure Protection Rule for `main`

**Section 1: Basic Settings**
```
Branch name pattern: main
```

**Section 2: Require Pull Request Reviews**
- ✅ Require a pull request before merging
- ✅ Dismiss stale pull request approvals when new commits are pushed
- Set required approving reviewers: 1 (minimum)

**Section 3: Require Status Checks to Pass**
- ✅ Require status checks to pass before merging
- ✅ Require branches to be up to date before merging

**Section 4: Select Status Checks**
Look for these checks (depends on your Jenkins setup):
- ✅ `jenkins/build-status` or similar
- ✅ `sonarqube-qualitygate` or `quality-gate`
- ✅ `SonarQube` or `SonarQube Quality Gate`

If you don't see these, run a PR build first to trigger them.

**Section 5: Other Protections** (optional)
- ✅ Include administrators (admins must follow rules too)
- ✅ Restrict who can push to matching branches

### 4.3 Save Branch Protection Rule

Click **Create** or **Save** button.

**Result:** 🔒 `main` branch is now protected

---

## Step 5: Test the Integration

### 5.1 Create a Test PR with Quality Issues

1. Create a new branch:
```bash
git checkout -b test/sonarqube-pr-test
```

2. Introduce an intentional code quality issue in one service.

**Example: Add code to `product-service/src/main/java/com/example/product/service/ProductService.java`:**

```java
// Bad code for testing
public String badSqlQuery(String productId) {
    // SQL Injection vulnerability
    String query = "SELECT * FROM products WHERE id = " + productId;
    return query;
}

public void unusedVariable() {
    // Unused variable (code smell)
    String neverUsed = "This is never used";
    System.out.println("Hello");
}
```

3. Commit and push:
```bash
git add product-service/
git commit -m "test: Add intentional quality issues"
git push origin test/sonarqube-pr-test
```

### 5.2 Open a Pull Request

1. Go to https://github.com/Violet-Ogombo/buy-01-dev
2. Click **Pull requests** tab
3. Click **New pull request** button
4. **Base:** `main`
5. **Compare:** `test/sonarqube-pr-test`
6. Click **Create pull request**
7. Add title: "Test: SonarQube PR Integration"
8. Click **Create pull request**

### 5.3 Wait for Checks to Run

GitHub will show:
```
Some checks are still in progress...
  • Jenkins CI - pending
  • SonarQube - pending
```

Wait 2-5 minutes for Jenkins to:
1. Build the code
2. Run SonarQube analysis
3. Post results to GitHub

### 5.4 Verify SonarQube Comments

Once complete, you should see:

**In PR Conversation Tab:**
- 🔴 SonarQube comment showing issues found
- Quality Gate status (FAILED or PASSED)
- List of bugs, vulnerabilities, code smells

**On Code Diff:**
- 🔴 Inline comments on exact lines with issues
- Links to SonarQube rules
- Suggested fixes

**PR Checks:**
- 🔴 "SonarQube Quality Gate" showing FAILED
- Merge button showing: "This branch has conflicts with the base branch" or "PR checks must pass"

### 5.5 Verify Merge is Blocked

Try to merge the PR:
1. Scroll to bottom of PR
2. Look for **Merge pull request** button
3. Button should be **DISABLED** (grayed out)
4. Message should show: "Required status check 'SonarQube' is failing"

✅ **Success!** Quality gate is blocking the merge.

### 5.6 Fix Issues and Re-push

1. Fix the bad code in your branch:

```java
// Fixed code
public String goodSqlQuery(String productId) {
    // Use parameterized queries
    return "SELECT * FROM products WHERE id = ?";
}

public void properlyUsedVariable() {
    String message = "This is used";
    System.out.println(message);
}
```

2. Commit and push:
```bash
git add product-service/
git commit -m "fix: Resolve SonarQube quality issues"
git push origin test/sonarqube-pr-test
```

3. Return to GitHub PR
4. Wait for checks to run again (1-2 minutes)
5. Once passing:
   - 🟢 "SonarQube Quality Gate" showing PASSED
   - Merge button now **ENABLED**
   - Can click **Merge pull request**

✅ **Success!** Merge is now allowed after fixes.

---

## Troubleshooting

### Issue: SonarQube Comments Not Appearing on PR

**Causes & Fixes:**

| Problem | Solution |
|---------|----------|
| GitHub App not installed | Re-do Step 1: Install the app |
| App not configured in SonarQube | Re-do Step 2: Configure GitHub in SonarQube |
| Private key is invalid | Check if key has BEGIN/END markers, no extra spaces |
| App not installed on this repo | Go to GitHub App settings, add `buy-01-dev` repo |
| Jenkins not running SonarQube | Verify Jenkinsfile has SonarQube Analysis stage |

**Debug:**
1. Check SonarQube logs: `docker logs sonarqube`
2. Check Jenkins logs: `docker logs jenkins`
3. Verify GitHub App is installed: GitHub Settings → Installed GitHub Apps

### Issue: Merge Button Still Disabled After Fixes

**Causes & Fixes:**

| Problem | Solution |
|---------|----------|
| Checks still running | Wait 2-5 minutes for Jenkins to complete |
| Branch is out of date | Click "Update branch" to sync with main |
| Multiple reviewers required | Get required number of approvals |
| Admin override needed | Only admins can bypass branch protection |

### Issue: Quality Gate Always Passes (Not Finding Issues)

**Causes & Fixes:**

| Problem | Solution |
|---------|----------|
| Default rules not strict enough | Configure custom quality profile |
| Code quality is actually good | ✅ This is good! |
| SonarQube scanner not running | Verify `mvn sonar:sonar` in Jenkinsfile |
| Analysis scope too narrow | Check project key settings |

---

## Configuration Summary

After completing all steps, your system will have:

```
Developer → GitHub PR
     ↓
Jenkins detects PR
     ↓
Jenkins runs tests & SonarQube analysis
     ↓
SonarQube posts comments on GitHub PR
     ↓
Branch protection checks SonarQube status
     ↓
If FAILED: Merge button disabled 🔴
If PASSED: Merge button enabled 🟢
     ↓
Developer fixes issues (if needed)
     ↓
PR merged only after Quality Gate passes
```

---

## Files & Commands Reference

### Docker Commands

```bash
# View SonarQube logs
docker logs sonarqube

# View Jenkins logs
docker logs jenkins

# Restart SonarQube if needed
docker restart sonarqube

# Check if SonarQube is healthy
docker exec sonarqube curl -s http://localhost:9000/api/system/health
```

### GitHub URLs

```
SonarQube GitHub App:
https://github.com/apps/sonarqube

Repository Settings:
https://github.com/Violet-Ogombo/buy-01-dev/settings

Branch Protection:
https://github.com/Violet-Ogombo/buy-01-dev/settings/branches

Installed Apps:
https://github.com/settings/installations
```

### Jenkins URLs

```
Jenkins Dashboard:
http://localhost:8085

Build History:
http://localhost:8085/job/buy-01-dev/

SonarQube Configuration:
http://localhost:8085/configure
```

### SonarQube URLs

```
SonarQube Dashboard:
http://localhost:9000

Administration:
http://localhost:9000/admin

GitHub Integration:
http://localhost:9000/admin/integrations/github
```

---

## Next Steps

1. ✅ Complete Setup Steps 1-5 above
2. ✅ Test integration with a PR (Step 5)
3. ✅ Fix any issues and verify merge blocking works
4. 📝 Document your GitHub branch protection policy
5. 📝 Create CODEOWNERS file for code review assignments
6. 📚 Train team on the new PR workflow

---

## Related Documentation

- [SONARQUBE_AUDIT_REPORT.md](SONARQUBE_AUDIT_REPORT.md) - Full audit findings
- [SONARQUBE_CONFIG.md](SONARQUBE_CONFIG.md) - Quality profile configuration (create this)
- [CODE_REVIEW_POLICY.md](CODE_REVIEW_POLICY.md) - Code review workflow (create this)
- [Official SonarQube GitHub Integration](https://docs.sonarqube.org/latest/devops-platform-integration/github-integration/)
- [GitHub Branch Protection](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)

---

**Last Updated:** May 19, 2026  
**Status:** Complete Setup Guide  
**Complexity:** Medium (5 steps, ~30 minutes)
