pipeline {
    agent {
        label 'Windows_Agent'
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

        stage('Jenkins_cli.jar Download') {
            steps {
                script {
                    // Ensure local path exists
                    bat "curl.exe -O http://localhost:8080/jnlpJars/jenkins-cli.jar"
                    echo "jenkins-cli.jar downloaded"

                }
            }
        }
      stage("Jenkins_Job_Creation") {
            steps {
              script {
                            
               bat """java -jar jenkins-cli.jar -auth pvaranasi95:11fa7390e7a1b0114123e7034528793f9f -s http://localhost:8080/ create-job ${params.Job_Name} --file Pipeline_Creation_XML//${params.Type}.xml"""
              }
            }
      }
         stage('clean workspace') {
            steps {
                cleanWs()
                echo "Workspace cleaning done"
            }
        }        
                
    }
}
