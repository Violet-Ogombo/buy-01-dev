pipeline {
    agent any
    
    options {
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
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
        }
        failure {
            echo '❌ Build or tests failed!'
        }
    }
}
