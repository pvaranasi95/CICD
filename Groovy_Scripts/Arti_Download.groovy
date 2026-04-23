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
                    bat "mkdir \"${params.LOCAL_PATH}\" || exit 0"

                    // Download using JFrog CLI
                    bat """
            jf rt dl "Test//*" "C:\\artifacts\\" --flat=false --url=http://localhost:8082 --user=admin --password=%ARTIFACTORY_CRED_PSW%
                    """
                }
                echo "Artifacts downloaded successfully to ${params.LOCAL_PATH}"
            }
        }
    }
}
