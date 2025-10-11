pipeline {
    agent any

    tools {
        jdk 'JDK11'
        maven 'Maven'
    }

    environment {
        BUILD_OUTPUT_DIR  = "${env.WORKSPACE}\\Builds"
        CONFIG_REPO       = "https://github.com/pvaranasi95/CICD.git"
        CONFIG_BRANCH     = "main"
        PROP_FILE         = "Properties/Adressbook_Properies.yaml"
    }

    stages {
        stage('Read Config') {
            steps {
                dir("CICD") {
                    // Checkout the config repository
                    checkout([$class: 'GitSCM',
                        branches: [[name: env.CONFIG_BRANCH]],
                        userRemoteConfigs: [[url: env.CONFIG_REPO]]
                    ])
                    script {
                        def props = readYaml file: env.PROP_FILE

                        // Set env variables for all stages
                        env.SOURCE_REPO       = props.git_repo_url
                        env.SOURCE_BRANCH     = props.git_branch
                        env.BUILD_WORKDIR     = props.workspace ?: "release-source"
                        env.ARTIFACTORY_REPO  = props.artifactory_repo ?: "default-repo"
                        env.ARTIFACTORY_URL   = props.artifactory_url
                        env.ARTIFACTORY_CREDS = props.artifactory_credentials
                        env.EMAIL_NOTIFY      = props.email_notify

                        echo "✅ Loaded config from ${env.PROP_FILE}"
                        echo "SOURCE_REPO       = ${env.SOURCE_REPO}"
                        echo "SOURCE_BRANCH     = ${env.SOURCE_BRANCH}"
                        echo "BUILD_WORKDIR     = ${env.BUILD_WORKDIR}"
                        echo "ARTIFACTORY_REPO  = ${env.ARTIFACTORY_REPO}"
                        echo "ARTIFACTORY_URL   = ${env.ARTIFACTORY_URL}"
                        echo "ARTIFACTORY_CREDS = ${env.ARTIFACTORY_CREDS}"
                        echo "EMAIL_NOTIFY      = ${env.EMAIL_NOTIFY}"
                    }
                }
            }
        }

        stage('Checkout Release Branch') {
            steps {
                dir(env.BUILD_WORKDIR) {
                    // Checkout the source repo release branch
                    checkout([$class: 'GitSCM',
                        branches: [[name: "*/${env.SOURCE_BRANCH}"]],
                        userRemoteConfigs: [[url: env.SOURCE_REPO]]
                    ])
                    echo "✅ Checked out ${env.SOURCE_BRANCH} from ${env.SOURCE_REPO} into ${env.BUILD_WORKDIR}"
                }
            }
        }

        stage('Maven Build') {
            steps {
                dir(env.BUILD_WORKDIR) {
                    bat "mvn clean install"
                }
            }
        }

        stage('Zip Build Output') {
            steps {
                script {
                    env.ZIP_FILE_PATH = "${env.BUILD_OUTPUT_DIR}\\${env.JOB_NAME}-${env.BUILD_NUMBER}.zip"
                    powershell """
                        if (!(Test-Path -Path '${env.BUILD_OUTPUT_DIR}')) { 
                            New-Item -ItemType Directory -Path '${env.BUILD_OUTPUT_DIR}' 
                        }
                        Compress-Archive -Path '${env.WORKSPACE}\\${env.BUILD_WORKDIR}\\target\\*' `
                                         -DestinationPath '${env.ZIP_FILE_PATH}' -Force
                    """
                    echo "✅ Build output zipped to ${env.ZIP_FILE_PATH}"
                }
            }
        }

        stage('Upload to Artifactory') {
            steps {
                withCredentials([usernamePassword(credentialsId: env.ARTIFACTORY_CREDS, usernameVariable: 'ART_USER', passwordVariable: 'ART_PASS')]) {
                    bat """
                        curl -u %ART_USER%:%ART_PASS% -T "${env.ZIP_FILE_PATH}" ^
                        "${env.ARTIFACTORY_URL}/artifactory/${env.ARTIFACTORY_REPO}/${env.JOB_NAME}/${env.BUILD_NUMBER}/${env.JOB_NAME}.zip"
                    """
                }
            }
        }
    }

    post {
        success { echo "✅ Build ${env.BUILD_NUMBER} completed successfully." }
        failure { echo "❌ Build ${env.BUILD_NUMBER} failed." }
    }
}
