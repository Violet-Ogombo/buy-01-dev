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
    }
}
