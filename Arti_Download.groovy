pipeline {
    agent any
        environment {
    ARTIFACTORY_CRED = credentials('Jfrog_Artifactory')
}


    tools {
        jdk 'JDK17'
        maven 'Maven'
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

//         stage('Sonar scan') {
//     steps {
//         withCredentials([string(credentialsId: 'Sonar', variable: 'SONAR_TOKEN')]) {
//             bat """
//             mvn -U verify org.sonarsource.scanner.maven:sonar-maven-plugin:3.11.0.3922:sonar ^
//              -Dsonar.projectKey=petclinic ^
//              -Dsonar.projectName=petclinic ^
//              -Dsonar.host.url=http://localhost:9000 ^
//              -Dsonar.token=%SONAR_TOKEN%
//             """
//         }
//     }
// }
        
        stage('Artifactory Download') {
    steps {
        bat """
            jfrog rt dl params.$ARTIFACTORY_REPONAME/ params.$LOCAL_PATH
        """
      echo "Artifacts downloaded Successfully"
    }
}


    }
}
