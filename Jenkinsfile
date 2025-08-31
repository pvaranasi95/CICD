pipeline {
    agent any

    tools {
        jdk 'JDK11'
        maven 'Maven'
    }

    environment {
        SOURCE_REPO   = "https://github.com/pvaranasi95/CICD.git"
        SOURCE_BRANCH = "main"
        PROP_FILE     = "Properties/Adressbook_Properies.yaml"
    }

    stages {
        stage('Read Config') {
            steps {
                script {
                    // Checkout the repo that contains Jenkinsfile + properties
                    checkout([$class: 'GitSCM',
                        branches: [[name: env.SOURCE_BRANCH]],
                        userRemoteConfigs: [[url: env.SOURCE_REPO]]
                    ])

                    // Load properties YAML from repo
                    def props = readYaml file: "${env.PROP_FILE}"

                    // Export variables
                    env.GIT_REPO_URL      = props.git_repo_url
                    env.WORKSPACE_PATH    = props.workspace
                    env.ARTIFACTORY_REPO  = props.artifactory_repo
                    env.ARTIFACTORY_URL   = props.artifactory_url
                    env.ARTIFACTORY_CREDS = props.artifactory_credentials
                    env.Email_Notify      = props.email_notify

                    echo "✅ Loaded props from ${env.PROP_FILE}"
                }
            }
        }

        stage('Git checkout Source Code') {
            steps {
                checkout([$class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[url: "${env.GIT_REPO_URL}"]]
                ])
            }
        }

        stage('Maven Build') {
            steps {
                bat "mvn clean install"
            }
        }

        stage('Zip Workdir') {
            steps {
                powershell """
                \$source = "${env.WORKSPACE}"
                \$destination = "${env.WORKSPACE_PATH}\\${env.JOB_NAME}-build${env.BUILD_NUMBER}.zip"

                if (Test-Path \$destination) { Remove-Item \$destination -Force }

                Compress-Archive -Path "\$source\\*" -DestinationPath \$destination
                """
            }
        }

        stage('Upload to Artifactory') {
            steps {
                withCredentials([usernamePassword(credentialsId: "${env.ARTIFACTORY_CREDS}", usernameVariable: 'ART_USER', passwordVariable: 'ART_PASS')]) {
                    bat """
                    curl -v -u %ART_USER%:%ART_PASS% -T "${env.WORKSPACE_PATH}\\${env.JOB_NAME}-build%BUILD_NUMBER%.zip" ^
                    "${env.ARTIFACTORY_URL}/${env.ARTIFACTORY_REPO}/${env.JOB_NAME}/%BUILD_NUMBER%/${env.JOB_NAME}.zip"
                    """
                }
            }
        }
    }
}
