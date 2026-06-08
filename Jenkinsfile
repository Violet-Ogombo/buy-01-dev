pipeline {
    agent any

    options {
        timeout(time: 45, unit: 'MINUTES')
        timestamps()
        // SafeZone: SonarQube security scanning enabled
    }

    triggers {
        pollSCM('* * * * *')
        githubPush()
    }

    environment {
        PROJECT_NAME = 'buy-01-dev'
        SLACK_CHANNEL = '#build-notifications'
        BUILD_URL_DISPLAY = "${env.BUILD_URL}console"
        BUILD_TAG = "build-${BUILD_NUMBER}"
        IMAGE_TAG_LATEST = "latest"
        IMAGE_TAG_PREVIOUS = "previous"
        DEPLOYMENT_TIMEOUT = '300'
        APP_SERVICES = "api-gateway product-service media-service identity-service frontend"
        SONARQUBE_URL = 'http://sonarqube:9000'
        MVN_TEST_CMD = 'mvn test'
    }

    stages {
        stage('Checkout') {
            steps {
                echo "========== Checking out code =========="
                checkout scm
                sh 'git log --oneline -1'
            }
        }

        stage('Build & Analyze Java Services') {
            parallel {
                stage('API Gateway') {
                    steps {
                        echo 'Building & Analyzing api-gateway...'
                        withSonarQubeEnv('SonarQube') {
                            dir('api-gateway') {
                                sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=api-gateway'
                            }
                        }
                    }
                }
                stage('Product Service') {
                    steps {
                        echo 'Building & Analyzing product-service...'
                        withSonarQubeEnv('SonarQube') {
                            dir('product-service') {
                                sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=product-service'
                            }
                        }
                    }
                }
                stage('Media Service') {
                    steps {
                        echo 'Building & Analyzing media-service...'
                        withSonarQubeEnv('SonarQube') {
                            dir('media-service') {
                                sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=media-service'
                            }
                        }
                    }
                }
                stage('Identity Service') {
                    steps {
                        echo 'Building & Analyzing identity-service...'
                        withSonarQubeEnv('SonarQube') {
                            dir('identity-service') {
                                sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=identity-service'
                            }
                        }
                    }
                }
                stage('Discovery Server') {
                    steps {
                        echo 'Building discovery-server...'
                        withSonarQubeEnv('SonarQube') {
                            dir('discovery-server') {
                                sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=discovery-service'
                            }
                        }
                    }
                }
            }
        }

        stage('Build Frontend') {
            steps {
                echo 'Building Angular frontend...'
                dir('buy-01-frontend') {
                    sh '''
                        npm install
                        npm run build
                    '''
                }
            }
        }

        stage('Frontend Unit Tests') {
            steps {
                echo '========== Running Frontend Unit Tests =========='
                dir('buy-01-frontend') {
                    sh 'npm run test -- --watch=false'
                }
            }
        }

        stage('Quality Gate Check') {
            steps {
                echo '========== Checking SonarQube Quality Gate =========='
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                echo "========== Building Docker Images =========="
                sh '''
                    echo "========== Creating Backups of Current Images =========="
                    for svc in $APP_SERVICES; do
                        docker tag buy-01-dev-$svc:latest buy-01-dev-$svc:backup 2>/dev/null || true
                    done

                    docker compose build $APP_SERVICES discovery-server
                '''
            }
        }

        stage('Deploy') {
            steps {
                echo "========== Deploying Application =========="
                script {
                    def jwtSecret = '404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970' // Default fallback
                    try {
                        withCredentials([string(credentialsId: 'jwt-secret', variable: 'SECRET')]) {
                            jwtSecret = SECRET
                        }
                    } catch (Exception e) {
                        echo "jwt-secret credential not found in Jenkins - using default fallback secret key."
                    }

                    withEnv(["JWT_SECRET=${jwtSecret}"]) {
                        sh '''
                            docker compose up -d --build $APP_SERVICES discovery-server
                            sleep 10
                            docker compose ps
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            echo '========== Collecting Artifacts =========='
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
        }

        success {
            echo '✓ Build and Deploy Successful!'
            script {
                catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                    def slackCredId = null
                    try {
                        withCredentials([string(credentialsId: 'slack-webhook-url', variable: 'DUMMY')]) {
                            slackCredId = 'slack-webhook-url'
                        }
                    } catch (Exception e) {
                        try {
                            withCredentials([string(credentialsId: 'slack token bot', variable: 'DUMMY')]) {
                                slackCredId = 'slack token bot'
                            }
                        } catch (Exception ex) {
                            echo "Slack notification skipped: No credential 'slack-webhook-url' or 'slack token bot' configured."
                        }
                    }

                    if (slackCredId != null) {
                        withCredentials([string(credentialsId: slackCredId, variable: 'SLACK_WEBHOOK_URL')]) {
                            sh '''
                                PAYLOAD="{
                                    \\"text\\": \\"✅ Build SUCCESS\\",
                                    \\"blocks\\": [
                                        {
                                            \\"type\\": \\"header\\",
                                            \\"text\\": {
                                                \\"type\\": \\"plain_text\\",
                                                \\"text\\": \\"✅ Build Successful\\"
                                            }
                                        },
                                        {
                                            \\"type\\": \\"section\\",
                                            \\"fields\\": [
                                                {
                                                    \\"type\\": \\"mrkdwn\\",
                                                    \\"text\\": \\"*Repository:*\\\\nbuy-01-dev\\"
                                                },
                                                {
                                                    \\"type\\": \\"mrkdwn\\",
                                                    \\"text\\": \\"*Branch:*\\\\nmain\\"
                                                },
                                                {
                                                    \\"type\\": \\"mrkdwn\\",
                                                    \\"text\\": \\"*Commit:*\\\\n''' + "${env.GIT_COMMIT?.take(7) ?: 'N/A'}" + '''\\"
                                                },
                                                {
                                                    \\"type\\": \\"mrkdwn\\",
                                                    \\"text\\": \\"*Build #:*\\\\n''' + "${env.BUILD_NUMBER}" + '''\\"
                                                }
                                            ]
                                        },
                                        {
                                            \\"type\\": \\"actions\\",
                                            \\"elements\\": [
                                                {
                                                    \\"type\\": \\"button\\",
                                                    \\"text\\": {
                                                        \\"type\\": \\"plain_text\\",
                                                        \\"text\\": \\"View Build Details\\"
                                                    },
                                                    \\"url\\": \\"''' + "${env.BUILD_URL_DISPLAY ?: ''}" + '''\\",
                                                    \\"style\\": \\"primary\\"
                                                }
                                            ]
                                        }
                                    ]
                                }"

                                BLOCKS='[
                                    {
                                        "type": "header",
                                        "text": {
                                            "type": "plain_text",
                                            "text": "✅ Build Successful"
                                        }
                                    },
                                    {
                                        "type": "section",
                                        "fields": [
                                            {
                                                "type": "mrkdwn",
                                                "text": "*Repository:*\\nbuy-01-dev"
                                            },
                                            {
                                                "type": "mrkdwn",
                                                "text": "*Branch:*\\nmain"
                                            },
                                            {
                                                "type": "mrkdwn",
                                                "text": "*Commit:*\\n''' + "${env.GIT_COMMIT?.take(7) ?: 'N/A'}" + '''"
                                            },
                                            {
                                                "type": "mrkdwn",
                                                "text": "*Build #:*\\n''' + "${env.BUILD_NUMBER}" + '''"
                                            }
                                        ]
                                    },
                                    {
                                        "type": "actions",
                                        "elements": [
                                            {
                                                "type": "button",
                                                "text": {
                                                    "type": "plain_text",
                                                    "text": "View Build Details"
                                                },
                                                "url": "''' + "${env.BUILD_URL_DISPLAY ?: ''}" + '''",
                                                "style": "primary"
                                            }
                                        ]
                                    }
                                ]'

                                if [[ "$SLACK_WEBHOOK_URL" == http* ]]; then
                                    curl -X POST -H 'Content-type: application/json' --data "$PAYLOAD" "$SLACK_WEBHOOK_URL" || echo "Slack notification failed"
                                elif [[ "$SLACK_WEBHOOK_URL" == xoxb-* ]]; then
                                    curl -X POST -H 'Content-type: application/json' -H "Authorization: Bearer $SLACK_WEBHOOK_URL" \
                                    --data "{ \\"channel\\": \\"${SLACK_CHANNEL}\\", \\"text\\": \\"✅ Build SUCCESS\\", \\"blocks\\": $BLOCKS }" \
                                    https://slack.com/api/chat.postMessage || echo "Slack notification failed"
                                else
                                    curl -X POST -H 'Content-type: application/json' --data "$PAYLOAD" "https://hooks.slack.com/services/$SLACK_WEBHOOK_URL" || echo "Slack notification failed"
                                fi
                            '''
                        }
                    }
                }
            }
        }

        failure {
            echo '✗ Build or Deploy Failed! Attempting Rollback to Backup...'
            sh '''
                # Revert tags from backup back to latest
                for svc in $APP_SERVICES; do
                    docker tag buy-01-dev-$svc:backup buy-01-dev-$svc:latest 2>/dev/null || true
                done

                # Restart using the reverted images (no --build flag)
                docker compose up -d $APP_SERVICES discovery-server
            '''

            echo '✗ Sending Failure Notifications...'
            script {
                catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                    def slackCredId = null
                    try {
                        withCredentials([string(credentialsId: 'slack-webhook-url', variable: 'DUMMY')]) {
                            slackCredId = 'slack-webhook-url'
                        }
                    } catch (Exception e) {
                        try {
                            withCredentials([string(credentialsId: 'slack token bot', variable: 'DUMMY')]) {
                                slackCredId = 'slack token bot'
                            }
                        } catch (Exception ex) {
                            echo "Slack notification skipped: No credential 'slack-webhook-url' or 'slack token bot' configured."
                        }
                    }

                    if (slackCredId != null) {
                        withCredentials([string(credentialsId: slackCredId, variable: 'SLACK_WEBHOOK_URL')]) {
                            sh '''
                                PAYLOAD="{
                                    \\"text\\": \\"❌ Build FAILED\\",
                                    \\"blocks\\": [
                                        {
                                            \\"type\\": \\"header\\",
                                            \\"text\\": {
                                                \\"type\\": \\"plain_text\\",
                                                \\"text\\": \\"❌ Build Failed\\"
                                            }
                                        },
                                        {
                                            \\"type\\": \\"section\\",
                                            \\"fields\\": [
                                                {
                                                    \\"type\\": \\"mrkdwn\\",
                                                    \\"text\\": \\"*Repository:*\\\\nbuy-01-dev\\"
                                                },
                                                {
                                                    \\"type\\": \\"mrkdwn\\",
                                                    \\"text\\": \\"*Branch:*\\\\nmain\\"
                                                },
                                                {
                                                    \\"type\\": \\"mrkdwn\\",
                                                    \\"text\\": \\"*Commit:*\\\\n''' + "${env.GIT_COMMIT?.take(7) ?: 'N/A'}" + '''\\"
                                                },
                                                {
                                                    \\"type\\": \\"mrkdwn\\",
                                                    \\"text\\": \\"*Build #:*\\\\n''' + "${env.BUILD_NUMBER}" + '''\\"
                                                }
                                            ]
                                        },
                                        {
                                            \\"type\\": \\"actions\\",
                                            \\"elements\\": [
                                                {
                                                    \\"type\\": \\"button\\",
                                                    \\"text\\": {
                                                        \\"type\\": \\"plain_text\\",
                                                        \\"text\\": \\"View Failure Details\\"
                                                    },
                                                    \\"url\\": \\"''' + "${env.BUILD_URL_DISPLAY ?: ''}" + '''\\",
                                                    \\"style\\": \\"danger\\"
                                                }
                                            ]
                                        }
                                    ]
                                }"

                                BLOCKS='[
                                    {
                                        "type": "header",
                                        "text": {
                                            "type": "plain_text",
                                            "text": "❌ Build Failed"
                                        }
                                    },
                                    {
                                        "type": "section",
                                        "fields": [
                                            {
                                                "type": "mrkdwn",
                                                "text": "*Repository:*\\nbuy-01-dev"
                                            },
                                            {
                                                "type": "mrkdwn",
                                                "text": "*Branch:*\\nmain"
                                            },
                                            {
                                                "type": "mrkdwn",
                                                "text": "*Commit:*\\n''' + "${env.GIT_COMMIT?.take(7) ?: 'N/A'}" + '''"
                                            },
                                            {
                                                "type": "mrkdwn",
                                                "text": "*Build #:*\\n''' + "${env.BUILD_NUMBER}" + '''"
                                            }
                                        ]
                                    },
                                    {
                                        "type": "actions",
                                        "elements": [
                                            {
                                                "type": "button",
                                                "text": {
                                                    "type": "plain_text",
                                                    "text": "View Failure Details"
                                                },
                                                "url": "''' + "${env.BUILD_URL_DISPLAY ?: ''}" + '''",
                                                "style": "danger"
                                            }
                                        ]
                                    }
                                ]'

                                if [[ "$SLACK_WEBHOOK_URL" == http* ]]; then
                                    curl -X POST -H 'Content-type: application/json' --data "$PAYLOAD" "$SLACK_WEBHOOK_URL" || echo "Slack notification failed"
                                elif [[ "$SLACK_WEBHOOK_URL" == xoxb-* ]]; then
                                    curl -X POST -H 'Content-type: application/json' -H "Authorization: Bearer $SLACK_WEBHOOK_URL" \
                                    --data "{ \\"channel\\": \\"${SLACK_CHANNEL}\\", \\"text\\": \\"❌ Build FAILED\\", \\"blocks\\": $BLOCKS }" \
                                    https://slack.com/api/chat.postMessage || echo "Slack notification failed"
                                else
                                    curl -X POST -H 'Content-type: application/json' --data "$PAYLOAD" "https://hooks.slack.com/services/$SLACK_WEBHOOK_URL" || echo "Slack notification failed"
                                fi
                            '''
        }
    }
}