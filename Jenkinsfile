pipeline {
    agent any

    tools {
        jdk 'JDK11'
        maven 'Maven'
    }

    environment {
        // Base output directory
        BUILD_OUTPUT_BASE = "${env.WORKSPACE}/${params.FOLDER_NAME}"
    }

    stages {

        stage('Load Config') {
            steps {
                script {
                    // --- Checkout config repo ---
                    dir("cicd-config") {
                        checkout([$class: 'GitSCM',
                            branches: [[name: 'main']],
                            userRemoteConfigs: [[url: 'https://github.com/pvaranasi95/CICD.git']]
                        ])

                        // --- Load YAML config ---
                        def configFile = "Properties/${env.JOB_NAME}_Properies.yaml"
                        def props = readYaml file: configFile
                        echo "✅ Loaded config from ${configFile}"

                        // --- Local variables ---
                        BUILD_WORKDIR     = props.workspace ?: "source-code"
                        SOURCE_REPO       = props.git_repo_url
                        SOURCE_BRANCH     = props.git_branch ?: "main"
                        ARTIFACTORY_URL   = props.artifactory_url
                        ARTIFACTORY_REPO  = props.artifactory_repo ?: "default-repo"
                        ARTIFACTORY_CREDS = props.artifactory_credentials

                        echo "Config: repo=${SOURCE_REPO}, branch=${SOURCE_BRANCH}, workspace=${BUILD_WORKDIR}"
                    }
                }
            }
        }

        stage('Checkout Source Code') {
            steps {
                script {
                    // Multibranch automatically checks out the branch, but if you need a separate repo:
                    dir(BUILD_WORKDIR) {
                        checkout([$class: 'GitSCM',
                            branches: [[name: 'main']],
                            userRemoteConfigs: [[url: SOURCE_REPO]]
                        ])
                    }
                }
            }
        }

        stage('Maven Build') {
            steps {
                script {
                    dir(BUILD_WORKDIR) {
                        bat "mvn clean install"
                    }
                }
            }
        }

        stage('Zip Build Output') {
            steps {
                script {
                    def zipFile = "${BUILD_OUTPUT_BASE}/${env.JOB_NAME}-${env.BUILD_NUMBER}.zip"

                    // Create output folder if it doesn't exist and zip
                    powershell """
                        if (!(Test-Path -Path '${BUILD_OUTPUT_BASE}')) { 
                            New-Item -ItemType Directory -Path '${BUILD_OUTPUT_BASE}' 
                        }
                        Compress-Archive -Path '${env.WORKSPACE}\\${BUILD_WORKDIR}\\target\\*' `
                                         -DestinationPath '${zipFile}' -Force
                    """
                    echo "✅ Build output zipped to ${zipFile}"
                }
            }
        }

        stage('Upload to Artifactory') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: ARTIFACTORY_CREDS, usernameVariable: 'ART_USER', passwordVariable: 'ART_PASS')]) {
                        bat """
                            curl -u %ART_USER%:%ART_PASS% -T "${BUILD_OUTPUT_BASE}\\${env.JOB_NAME}-${env.BUILD_NUMBER}.zip" ^
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
        unstable { echo "⚠️ Build ${env.BUILD_NUMBER} is unstable." }
    }
}
