pipeline {
    agent any
    environment {
        ARTIFACTORY_CRED = credentials('Jfrog_Artifactory')
    }
    tools {
        jdk 'JDK17'
        maven 'Maven'
    }
    parameters {
        string(name: 'ARTIFACTORY_REPONAME', defaultValue: 'Test1', description: 'Artifactory repository to download')
        string(name: 'ARTIFACTORY_FOLDER', defaultValue: '', description: 'Folder inside repo')
        string(name: 'LOCAL_PATH', defaultValue: 'C:\\artifacts', description: 'Local folder to save artifacts')
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
                    jfrog rt dl "${params.ARTIFACTORY_REPONAME}/${params.ARTIFACTORY_FOLDER}/" "${params.LOCAL_PATH}/" --flat=false
                    """
                }
                echo "Artifacts downloaded successfully to ${params.LOCAL_PATH}"
            }
        }
    }
}
