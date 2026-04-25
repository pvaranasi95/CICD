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

        stage('Artifactory Upload') {
            steps {
                script {
                    // Ensure local path exists
                    // Download using JFrog CLI
                    bat """
                    curl.exe -u admin:password -T "${params.Source_File_Path}\\${params.File_Name}" "http://localhost:8082/${params.Artifactory_Target_Folder}/${params.File_Name}"
                    
                    """
                }
                echo "Artifacts uploaded successfully to ${params.Artifactory_Target_Folder}"
            }
        }
    }
}
