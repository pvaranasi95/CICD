pipeline {
    agent { label 'Windows' }
    environment {
        ARTIFACTORY_CRED = credentials('Jfrog_Artifactory')
    }
    stages {
        stage('Git checkout') {
            steps {
                checkout scmGit(
                    branches: [[name: '*/main']],
                    extensions: [],
                    userRemoteConfigs: [[
                        url: 'https://github.com/pvaranasi95/CICD.git',
                        credentialsId: 'GitHub_Cred'
                    ]]
                )
            }
        }

        stage('Artifactory Download') {
            steps {
                script {
                    // Ensure local path exists
                    // Download using JFrog CLI
                    bat """
                    curl.exe -u admin:password -O ${params.Artifactory_Path}
                    """
                }
                echo "Artifacts downloaded successfully to ${params.LOCAL_PATH}"
            }
        }
    }
}
