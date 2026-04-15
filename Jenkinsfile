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
                echo "Building and starting all microservices..."
                sh './docker.sh'
                echo "Waiting for services to be healthy..."
                sh 'sleep 30'
            }
        }
        
        stage('Run API Gateway Tests') {
            steps {
                echo "Testing API Gateway..."
                sh 'docker exec api-gateway mvn test'
            }
        }
        
        stage('Run Product Service Tests') {
            steps {
                echo "Testing Product Service..."
                sh 'docker exec product-service mvn test'
            }
        }
        
        stage('Run Media Service Tests') {
            steps {
                echo "Testing Media Service..."
                sh 'docker exec media-service mvn test'
            }
        }
        
        stage('Run Identity Service Tests') {
            steps {
                echo "Testing Identity Service..."
                sh 'docker exec identity-service mvn test'
            }
        }
    }
    
    post {
        always {
            echo "Collecting test results..."
            junit '**/target/surefire-reports/*.xml' || true
        }
        success {
            echo '✅ All tests passed!'
        }
        failure {
            echo '❌ Build or tests failed!'
        }
    }
}
