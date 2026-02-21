pipeline {
    agent any

    environment {
        BUILD_OUTPUT_DIR = "${env.WORKSPACE}/Builds"
        CONFIG_REPO      = "https://github.com/pvaranasi95/CICD.git"
        CONFIG_BRANCH    = "main"
        PROP_FILE        = "Properties/Adressbook_Properies.yaml"
    }

    stages {

        stage('Load Build Configuration') {
            steps {
                dir("CICD") {
                    checkout([$class: 'GitSCM',
                        branches: [[name: env.CONFIG_BRANCH]],
                        userRemoteConfigs: [[url: env.CONFIG_REPO]]
                    ])

                    script {
                        def props = readYaml file: env.PROP_FILE

                        env.SOURCE_REPO       = props.git_repo_url
                        env.SOURCE_BRANCH     = props.git_branch
                        env.BUILD_WORKDIR     = props.workspace ?: "release-source"
                        env.ARTIFACTORY_REPO  = props.artifactory_repo ?: "default-repo"
                        env.ARTIFACTORY_URL   = props.artifactory_url
                        env.ARTIFACTORY_CREDS = props.artifactory_credentials
                        env.EMAIL_NOTIFY      = props.email_notify

                        echo "✅ Config Loaded"
                        echo "Repo: ${env.SOURCE_REPO}"
                        echo "Branch: ${env.SOURCE_BRANCH}"
                    }
                }
            }
        }

        stage('Checkout Source') {
            steps {
                dir(env.BUILD_WORKDIR) {
                    checkout([$class: 'GitSCM',
                        branches: [[name: "*/${env.SOURCE_BRANCH}"]],
                        userRemoteConfigs: [[url: env.SOURCE_REPO]]
                    ])
                }
            }
        }

        stage('Build Application') {
            steps {
                dir(env.BUILD_WORKDIR) {
                    sh 'mvn clean install -DskipTests'
                }
            }
        }

        stage('Package Artifact') {
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
                }
            }
        }
    }

 post {
    always {
        script {
            def jenkinsBuildData = [
                job_name: env.JOB_NAME,
                build_number: env.BUILD_NUMBER.toInteger(),
                status: currentBuild.currentResult,
                timestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", TimeZone.getTimeZone('UTC')),
                duration: currentBuild.duration,
                url: env.BUILD_URL
            ]

            def jsonBody = groovy.json.JsonOutput.toJson(jenkinsBuildData)

            echo "Sending build data to Elasticsearch: ${jsonBody}"

            node {
                sh """
                curl -X POST "http://host.docker.internal:9200/jenkins/_doc" \
                     -H "Content-Type: application/json" \
                     -d '${jsonBody}'
                """
            }
        }
    }
}
}
