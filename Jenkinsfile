pipeline {
    agent any

    tools {
        jdk 'JDK11'
        maven 'Maven'
    }

    environment {
        BUILD_OUTPUT_DIR  = "${env.WORKSPACE}\\Builds"
    }

    stages {
stage('Read Config') {
    steps {
        dir("CICD") {
            checkout([$class: 'GitSCM',
                branches: [[name: 'main']],
                userRemoteConfigs: [[url: 'https://github.com/pvaranasi95/CICD.git']]
            ])
            script {
                def props = readYaml file: "Properties/Adressbook_Properies.yaml"

                // Set env variables for use in all stages
                env.SOURCE_REPO       = props.git_repo_url
                env.SOURCE_BRANCH     = props.git_branch
                env.BUILD_WORKDIR     = props.workspace ?: "release-source"
                env.ARTIFACTORY_REPO  = props.artifactory_repo ?: "default-repo"
                env.ARTIFACTORY_URL   = props.artifactory_url
                env.ARTIFACTORY_CREDS = props.artifactory_credentials
                env.EMAIL_NOTIFY      = props.email_notify

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
                script {
                    dir(BUILD_WORKDIR) {
                        checkout([$class: 'GitSCM',
                            branches: [[name: "*/${SOURCE_BRANCH}"]],
                            userRemoteConfigs: [[url: SOURCE_REPO]]
                        ])
                        echo "✅ Checked out ${SOURCE_BRANCH} from ${SOURCE_REPO} into ${BUILD_WORKDIR}"
                    }
                }
            }
        }

        stage('Maven Build') {
            steps {
                dir(BUILD_WORKDIR) {
                    bat "mvn clean install"
                }
            }
        }

        stage('Zip Build Output') {
            steps {
                script {
                    def zipFile = "${BUILD_OUTPUT_DIR}\\${env.JOB_NAME}-${env.BUILD_NUMBER}.zip"
                    powershell """
                        if (!(Test-Path -Path '${BUILD_OUTPUT_DIR}')) { 
                            New-Item -ItemType Directory -Path '${BUILD_OUTPUT_DIR}' 
                        }
                        Compress-Archive -Path '${env.WORKSPACE}\\${BUILD_WORKDIR}\\target\\*' `
                                         -DestinationPath '${zipFile}' -Force
                    """
                    ZIP_FILE_PATH = zipFile
                    echo "✅ Build output zipped to ${zipFile}"
                }
            }
        }

        stage('Upload to Artifactory') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: ARTIFACTORY_CREDS, usernameVariable: 'ART_USER', passwordVariable: 'ART_PASS')]) {
                        bat """
                            curl -u %ART_USER%:%ART_PASS% -T "${ZIP_FILE_PATH}" ^
                            "${ARTIFACTORY_URL}/artifactory/${ARTIFACTORY_REPO}/${env.JOB_NAME}/${env.BUILD_NUMBER}/${env.JOB_NAME}.zip"
                        """
                    }
                }
            }
        }
    }

    post {
        success { echo "✅ Build ${env.BUILD_NUMBER} completed successfully." }
        failure { echo "❌ Build ${env.BUILD_NUMBER} failed." }
    }
}
