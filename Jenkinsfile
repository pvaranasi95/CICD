pipeline {
    agent any

    environment {
        BUILD_OUTPUT_DIR = "${env.WORKSPACE}/Builds"
        CONFIG_REPO      = "https://github.com/pvaranasi95/CICD.git"
        CONFIG_BRANCH    = "main"
    }

    stages {

     stage('Load Build Configuration') {
    steps {
       dir('CICD') {
    git branch: 'main', url: 'https://github.com/pvaranasi95/CICD.git'
}

script {
     echo "${env.JOB_NAME}"
    def yamlPath = "./Properties/${env.JOB_NAME}_Properties.yaml"

    if (!fileExists(yamlPath)) {
        error "❌ Config file not found: ${yamlPath}"
    }

    def props = readYaml file: yamlPath

    env.SOURCE_REPO       = props.git_repo_url
    env.SOURCE_BRANCH     = props.git_branch ?: "main"
    env.BUILD_WORKDIR     = props.workspace ?: "release-source"
    env.ARTIFACTORY_REPO  = props.artifactory_repo ?: "default-repo"
    env.ARTIFACTORY_URL   = props.artifactory_url
    env.ARTIFACTORY_CREDS = props.artifactory_credentials
    env.EMAIL_NOTIFY      = props.email_notify
}
    }
     }
        stage('Checkout Source') {
            when { expression { env.STAGES_TO_RUN.contains('checkout') } }
            steps {
                dir(env.BUILD_WORKDIR) {
                    checkout([$class: 'GitSCM',
                        branches: [[name: "*/${env.SOURCE_BRANCH}"]],
                        userRemoteConfigs: [[url: env.SOURCE_REPO]]
                    ])
                    echo "✅ Checked out ${env.SOURCE_BRANCH}"
                }
            }
        }

        stage('Build Application') {
            when { expression { env.STAGES_TO_RUN.contains('build') } }
            steps {
                dir(env.BUILD_WORKDIR) {
                    sh 'mvn clean install -DskipTests'
                    echo "✅ Build completed"
                }
            }
        }

        stage('Run Tests') {
            when { expression { env.STAGES_TO_RUN.contains('test') } }
            steps {
                dir(env.BUILD_WORKDIR) {
                    sh 'mvn test'
                    echo "✅ Tests executed"
                }
            }
        }

        stage('Package Artifact') {
            when { expression { env.STAGES_TO_RUN.contains('package') } }
            steps {
                script {
                    env.ZIP_FILE_PATH = "${env.BUILD_OUTPUT_DIR}/${env.JOB_NAME}-${env.BUILD_NUMBER}.zip"

                    sh """
                    mkdir -p ${env.BUILD_OUTPUT_DIR}
                    zip -r ${env.ZIP_FILE_PATH} ${env.WORKSPACE}/${env.BUILD_WORKDIR}/target/*
                    """

                    echo "✅ Artifact packaged: ${env.ZIP_FILE_PATH}"
                }
            }
        }

        stage('Upload to Artifactory') {
            when { expression { env.STAGES_TO_RUN.contains('upload') } }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: env.ARTIFACTORY_CREDS,
                    usernameVariable: 'ART_USER',
                    passwordVariable: 'ART_PASS'
                )]) {
                    sh """
                    curl -u $ART_USER:$ART_PASS -T "${env.ZIP_FILE_PATH}" \
                    "${env.ARTIFACTORY_URL}/artifactory/${env.ARTIFACTORY_REPO}/${env.JOB_NAME}/${env.BUILD_NUMBER}/${env.JOB_NAME}.zip"
                    """
                    echo "✅ Uploaded to Artifactory"
                }
            }
        }
    }

    post {
        always {
            script {
                // Send build data to Elasticsearch
                def jenkinsBuildData = [
                    job_name: env.JOB_NAME,
                    build_number: env.BUILD_NUMBER.toInteger(),
                    status: currentBuild.currentResult,
                    timestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", TimeZone.getTimeZone('UTC')),
                    duration: currentBuild.duration,
                    url: env.BUILD_URL
                ]
                def jsonBody = groovy.json.JsonOutput.toJson(jenkinsBuildData)
                echo "Sending build data to Elasticsearch"

                sh """
                curl -X POST "http://host.docker.internal:9200/jenkins/_doc" \
                     -H "Content-Type: application/json" \
                     -d '${jsonBody}' || true
                """
            }
        }
    }
}
