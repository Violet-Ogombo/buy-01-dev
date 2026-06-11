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
        APP_SERVICES = "api-gateway product-service order-service media-service identity-service frontend"
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

        stage('Build & Analyze API Gateway') {
            steps {
                echo 'Building & Analyzing api-gateway...'
                withSonarQubeEnv('SonarQube') {
                    dir('api-gateway') {
                        sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=api-gateway'
                    }
                }
            }
        }

        stage('Build & Analyze Product Service') {
            steps {
                echo 'Building & Analyzing product-service...'
                withSonarQubeEnv('SonarQube') {
                    dir('product-service') {
                        sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=product-service'
                    }
                }
            }
        }

        stage('Build & Analyze Order Service') {
            steps {
                echo 'Building & Analyzing order-service...'
                withSonarQubeEnv('SonarQube') {
                    dir('order-service') {
                        sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=order-service'
                    }
                }
            }
        }

        stage('Build & Analyze Media Service') {
            steps {
                echo 'Building & Analyzing media-service...'
                withSonarQubeEnv('SonarQube') {
                    dir('media-service') {
                        sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=media-service'
                    }
                }
            }
        }

        stage('Build & Analyze Identity Service') {
            steps {
                echo 'Building & Analyzing identity-service...'
                withSonarQubeEnv('SonarQube') {
                    dir('identity-service') {
                        sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=identity-service'
                    }
                }
            }
        }

        stage('Build & Analyze Discovery Server') {
            steps {
                echo 'Building discovery-server...'
                withSonarQubeEnv('SonarQube') {
                    dir('discovery-server') {
                        sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=discovery-service'
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
                    slackSend(
                        tokenCredentialId: 'slack-webhook-url',
                        color: 'good',
                        message: """:white_check_mark: *Build SUCCESS*
Job: ${env.JOB_NAME}
Build #: ${env.BUILD_NUMBER}
Status: SUCCESS
URL: ${env.BUILD_URL}"""
                    )
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
                    slackSend(
                        tokenCredentialId: 'slack-webhook-url',
                        color: 'danger',
                        message: """:x: *Build FAILED*
Job: ${env.JOB_NAME}
Build #: ${env.BUILD_NUMBER}
Status: FAILED
URL: ${env.BUILD_URL}"""
                    )
                }
            }
        }
    }
}