pipeline {
    agent any

    tools {
        jdk 'JDK11'
        maven 'Maven'
    }

    stages {
        stage('Load Config') {
            steps {
                script {
                    def configFile = "Properties/Test1_Properies.yaml"
                    echo "Loading config: ${configFile}"
                    def props = readYaml file: configFile

                    // Local variables
                    def BUILD_WORKDIR = props.workspace ?: "source-code"
                    def SOURCE_REPO   = props.git_repo_url
                    def SOURCE_BRANCH = props.git_branch ?: "main"

                    echo "Workspace: ${BUILD_WORKDIR}, Repo: ${SOURCE_REPO}, Branch: ${SOURCE_BRANCH}"

                    // Do something with these variables in the same script block
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    bat "mvn clean install"
                }
            }
        }

        stage('Zip') {
            steps {
                script {
                    powershell """
                        if (!(Test-Path -Path '${env.WORKSPACE}\\Builds')) { 
                            New-Item -ItemType Directory -Path '${env.WORKSPACE}\\Builds' 
                        }
                        Compress-Archive -Path '${env.WORKSPACE}\\target\\*' -DestinationPath '${env.WORKSPACE}\\Builds\\${env.JOB_NAME}-${env.BUILD_NUMBER}.zip' -Force
                    """
                }
            }
        }
    }
}
