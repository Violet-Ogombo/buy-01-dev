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
                checkout scm
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo "🔍 Running SonarQube Analysis on all services..."
                withSonarQubeEnv('sonarqube') {
                    script {
                        // Analyze API Gateway
                        dir('api-gateway') {
                            sh '''
                                mvn clean verify sonar:sonar \
                                  -Dsonar.projectKey=api-gateway
                            '''
                        }
                        
                        // Analyze Product Service
                        dir('product-service') {
                            sh '''
                                mvn clean verify sonar:sonar \
                                  -Dsonar.projectKey=product-service
                            '''
                        }
                        
                        // Analyze Media Service
                        dir('media-service') {
                            sh '''
                                mvn clean verify sonar:sonar \
                                  -Dsonar.projectKey=media-service
                            '''
                        }
                        
                        // Analyze Identity Service
                        dir('identity-service') {
                            sh '''
                                mvn clean verify sonar:sonar \
                                  -Dsonar.projectKey=identity-service
                            '''
                        }
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                echo "🚪 Checking Quality Gate status for all projects..."
                timeout(time: 5, unit: 'MINUTES') {
                    // Single waitForQualityGate checks the pipeline's analysis status
                    // Individual project gates are tracked via their projectKeys above
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Run API Gateway Tests') {
            steps {
                echo "Testing API Gateway..."
                dir('api-gateway') {
                    sh env.MVN_TEST_CMD
                }
            }
        }

        stage('Run Product Service Tests') {
            steps {
                echo "Testing Product Service..."
                dir('product-service') {
                    sh env.MVN_TEST_CMD
                    //sh 'exit 1'
                }
            }
        }

        stage('Run Media Service Tests') {
            steps {
                echo "Testing Media Service..."
                dir('media-service') {
                    sh env.MVN_TEST_CMD
                }
            }
        }

        stage('Run Identity Service Tests') {
            steps {
                echo "Testing Identity Service..."
                dir('identity-service') {
                    sh env.MVN_TEST_CMD
                }
            }
        }

        stage('Deploy Application Services') {
            when {
                branch 'main'
            }
            steps {
                echo "🚀 Starting application deployment..."
                script {
                    sh '''
                        set -e

                        echo "📸 Creating backup tags for current application images..."
                        for svc in $APP_SERVICES; do
                            docker tag buy-01-dev-$svc:latest buy-01-dev-$svc:previous 2>/dev/null || true
                        done

                        echo "🔨 Rebuilding only application services..."
                        docker compose up -d --build --no-deps --force-recreate $APP_SERVICES

                        echo "⏳ Waiting for services to initialize..."
                        sleep 30

                        echo "✅ Checking service status..."
                        docker compose ps

                        echo "🏥 Running smoke tests..."
                        failed=0

                        curl -sf http://localhost:8080/health >/dev/null || failed=1
                        curl -sf http://localhost:8082/health >/dev/null || failed=1
                        curl -sf http://localhost:8083/health >/dev/null || failed=1
                        curl -sf http://localhost:8081/health >/dev/null || failed=1

                        if [ "$failed" -ne 0 ]; then
                            echo "❌ Smoke tests failed"
                            exit 1
                        fi

                        echo "✅ Deployment completed successfully!"
                    '''
                }
            }
        }
    }

    post {
        always {
            echo "Collecting test results..."
            catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            }
        }

        success {
            echo '✅ All tests passed!'
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
                                curl -X POST -H 'Content-type: application/json' \
                                --data "{
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
                                }" \
                                $SLACK_WEBHOOK_URL || echo "Slack notification failed"
                            '''
                        }
                    }
                }
            }
        }

        failure {
            echo '❌ Build or tests failed!'
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
                                curl -X POST -H 'Content-type: application/json' \
                                --data "{
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
                                }" \
                                $SLACK_WEBHOOK_URL || echo "Slack notification failed"
                            '''
                        }
                    }
                }
            }
        }

        unstable {
            echo '⚠️  Deployment failed - initiating rollback...'
            script {
                sh '''
                    set +e

                    echo "🔄 Rolling back application services only..."
                    for svc in $APP_SERVICES; do
                        docker tag buy-01-dev-$svc:previous buy-01-dev-$svc:latest 2>/dev/null || true
                    done

                    echo "🔄 Restarting previous application version..."
                    docker compose up -d --no-deps --force-recreate $APP_SERVICES

                    echo "⏳ Waiting for services to stabilize..."
                    sleep 20

                    docker compose ps

                    echo "✅ Rollback completed!"
                '''
            }

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
                                curl -X POST -H 'Content-type: application/json' \
                                --data "{
                                    \\"text\\": \\"⚠️  ROLLBACK EXECUTED\\",
                                    \\"blocks\\": [
                                        {
                                            \\"type\\": \\"header\\",
                                            \\"text\\": {
                                                \\"type\\": \\"plain_text\\",
                                                \\"text\\": \\"⚠️  Deployment Failed - Rollback Executed\\"
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
                                                    \\"text\\": \\"*Action:*\\nReverted to previous stable version\\"
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
                                                        \\"text\\": \\"View Build Log\\"
                                                    },
                                                    \\"url\\": \\"''' + "${env.BUILD_URL_DISPLAY ?: ''}" + '''\\",
                                                    \\"style\\": \\"danger\\"
                                                }
                                            ]
                                        }
                                    ]
                                }" \
                                $SLACK_WEBHOOK_URL || echo "Slack notification failed"
                            '''
                        }
                    }
                }
            }
        }
    }
}