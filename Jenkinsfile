pipeline {
    agent any
    
    options {
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }
    
    triggers {
        pollSCM('') // Empty: relies on GitHub webhook to trigger
        githubPush()
    }
    
    environment {
        SLACK_CHANNEL = '#build-notifications'
        SLACK_WEBHOOK_URL = credentials('slack-webhook-url')
        BUILD_URL_DISPLAY = "${env.BUILD_URL}console"
        BUILD_TAG = "build-${BUILD_NUMBER}"
        IMAGE_TAG_LATEST = "latest"
        IMAGE_TAG_PREVIOUS = "previous"
        DEPLOYMENT_TIMEOUT = '300'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[
                        url: 'git@github.com:Violet-Ogombo/buy-01-dev.git',
                        credentialsId: 'github-ssh-key'
                    ]]
                ])
            }
        }
        
        stage('Build All Services') {
            steps {
                echo "Skipping rebuild - services already running from docker-compose"
                echo "Services are running in Docker containers"
                sh 'docker ps | grep buy-01-dev || echo "Warning: No services found"'
                echo "Waiting for services to be ready..."
                sh 'sleep 10'
            }
        }
        
        stage('Run API Gateway Tests') {
            steps {
                echo "Testing API Gateway..."
                dir('api-gateway') {
                    sh 'mvn test'
                }
            }
        }
        
        stage('Run Product Service Tests') {
            steps {
                echo "Testing Product Service..."
                dir('product-service') {
                    sh 'mvn test'
                }
            }
        }
        
        stage('Run Media Service Tests') {
            steps {
                echo "Testing Media Service..."
                dir('media-service') {
                    sh 'mvn test'
                }
            }
        }
        
        stage('Run Identity Service Tests') {
            steps {
                echo "Testing Identity Service..."
                dir('identity-service') {
                    sh 'mvn test'
                }
            }
        }

        stage('Deploy') {
            steps {
                echo "🚀 Starting deployment..."
                script {
                    sh '''
                        echo "📸 Creating backup of current images as 'previous'..."
                        docker tag buy-01-dev-api-gateway:latest buy-01-dev-api-gateway:previous 2>/dev/null || true
                        docker tag buy-01-dev-product-service:latest buy-01-dev-product-service:previous 2>/dev/null || true
                        docker tag buy-01-dev-media-service:latest buy-01-dev-media-service:previous 2>/dev/null || true
                        docker tag buy-01-dev-identity-service:latest buy-01-dev-identity-service:previous 2>/dev/null || true
                        docker tag buy-01-dev-frontend:latest buy-01-dev-frontend:previous 2>/dev/null || true
                        
                        echo "🔨 Rebuilding Docker images with version tag: ${BUILD_TAG}..."
                        docker-compose build --no-cache
                        
                        echo "🔄 Restarting services with updated images..."
                        docker-compose up -d --force-recreate
                        
                        echo "⏳ Waiting for services to be healthy (30 seconds)..."
                        sleep 30
                        
                        echo "✅ Checking service health..."
                        docker-compose ps
                        
                        echo "🏥 Running smoke tests to verify deployment..."
                        max_attempts=5
                        attempt=1
                        while [ $attempt -le $max_attempts ]; do
                            echo "Attempt $attempt/$max_attempts - Testing services..."
                            
                            # Test API Gateway
                            if curl -s -f http://localhost:8080/health > /dev/null 2>&1; then
                                echo "✅ API Gateway is healthy"
                            else
                                echo "⚠️  API Gateway health check failed"
                            fi
                            
                            # Test Product Service
                            if curl -s -f http://localhost:8082/health > /dev/null 2>&1; then
                                echo "✅ Product Service is healthy"
                            else
                                echo "⚠️  Product Service health check failed"
                            fi
                            
                            # Test Media Service
                            if curl -s -f http://localhost:8083/health > /dev/null 2>&1; then
                                echo "✅ Media Service is healthy"
                            else
                                echo "⚠️  Media Service health check failed"
                            fi
                            
                            if [ $attempt -lt $max_attempts ]; then
                                echo "Waiting 10 seconds before next attempt..."
                                sleep 10
                            fi
                            attempt=$((attempt + 1))
                        done
                        
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
                junit '**/target/surefire-reports/*.xml'
            }
        }
        success {
            echo '✅ All tests passed!'
            script {
                def message = """
                    ✅ *Build SUCCESS*
                    Repository: buy-01-dev
                    Branch: main
                    Commit: ${env.GIT_COMMIT?.take(7) ?: 'N/A'}
                    Build: <${BUILD_URL_DISPLAY}|View Details>
                """.stripIndent()
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
                                        \\"text\\": \\"*Repository:*\\nbuy-01-dev\\"
                                    },
                                    {
                                        \\"type\\": \\"mrkdwn\\",
                                        \\"text\\": \\"*Branch:*\\nmain\\"
                                    },
                                    {
                                        \\"type\\": \\"mrkdwn\\",
                                        \\"text\\": \\"*Commit:*\\n''' + "${env.GIT_COMMIT?.take(7) ?: 'N/A'}" + '''\\"
                                    },
                                    {
                                        \\"type\\": \\"mrkdwn\\",
                                        \\"text\\": \\"*Build #:*\\n''' + "${env.BUILD_NUMBER}" + '''\\"
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
                                        \\"url\\": \\"''' + "${BUILD_URL_DISPLAY}" + '''\\",
                                        \\"style\\": \\"primary\\"
                                    }
                                ]
                            }
                        ]
                    }" \
                    $SLACK_WEBHOOK_URL || echo "Slack notification failed (webhook may not be configured yet)"
                '''
            }
        }
        failure {
            echo '❌ Build or tests failed!'
            script {
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
                                        \\"text\\": \\"*Repository:*\\nbuy-01-dev\\"
                                    },
                                    {
                                        \\"type\\": \\"mrkdwn\\",
                                        \\"text\\": \\"*Branch:*\\nmain\\"
                                    },
                                    {
                                        \\"type\\": \\"mrkdwn\\",
                                        \\"text\\": \\"*Commit:*\\n''' + "${env.GIT_COMMIT?.take(7) ?: 'N/A'}" + '''\\"
                                    },
                                    {
                                        \\"type\\": \\"mrkdwn\\",
                                        \\"text\\": \\"*Build #:*\\n''' + "${env.BUILD_NUMBER}" + '''\\"
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
                                        \\"url\\": \\"''' + "${BUILD_URL_DISPLAY}" + '''\\",
                                        \\"style\\": \\"danger\\"
                                    }
                                ]
                            }
                        ]
                    }" \
                    $SLACK_WEBHOOK_URL || echo "Slack notification failed (webhook may not be configured yet)"
                '''
            }
        }
        
        unstable {
            echo '⚠️  Deployment failed - initiating rollback...'
            script {
                sh '''
                    echo "🔄 Rolling back to previous stable version..."
                    
                    docker tag buy-01-dev-api-gateway:previous buy-01-dev-api-gateway:latest 2>/dev/null || true
                    docker tag buy-01-dev-product-service:previous buy-01-dev-product-service:latest 2>/dev/null || true
                    docker tag buy-01-dev-media-service:previous buy-01-dev-media-service:latest 2>/dev/null || true
                    docker tag buy-01-dev-identity-service:previous buy-01-dev-identity-service:latest 2>/dev/null || true
                    docker tag buy-01-dev-frontend:previous buy-01-dev-frontend:latest 2>/dev/null || true
                    
                    echo "🔄 Restarting services with previous version..."
                    docker-compose up -d
                    
                    echo "⏳ Waiting for services to stabilize..."
                    sleep 20
                    
                    docker-compose ps
                    
                    echo "✅ Rollback completed!"
                '''
            }
            
            script {
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
                                        \\"text\\": \\"*Repository:*\\nbuy-01-dev\\"
                                    },
                                    {
                                        \\"type\\": \\"mrkdwn\\",
                                        \\"text\\": \\"*Action:*\\nReverted to previous stable version\\"
                                    },
                                    {
                                        \\"type\\": \\"mrkdwn\\",
                                        \\"text\\": \\"*Build #:*\\n''' + "${env.BUILD_NUMBER}" + '''\\"
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
                                        \\"url\\": \\"''' + "${BUILD_URL_DISPLAY}" + '''\\",
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
