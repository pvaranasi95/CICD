pipeline {
    agent any
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
                    sh "curl -O http://localhost:8080/jnlpJars/jenkins-cli.jar"
                    echo "jenkins-cli.jar downloaded"

                }
            }
        }
      stage("Jenkins_Job_Creation") {
            steps {
              script {
                def xmlFile

                    if (params.Type == 'Pipeline') {
                        xmlFile = 'pipeline.xml'
                    } else if (params.Type == 'Freestyle') {
                        xmlFile = 'freestyle.xml'
                    } else if (params.Type == 'Multi-configuration') {
                        xmlFile = 'Multi-configuration.xml'
                    } else if (params.Type == 'Multi-Branch') {
                        xmlFile = 'Multi-Branch.xml'
                    } else if (params.Type == 'Folder') {
                        xmlFile = 'Folder.xml'
                    } else {
                        xmlFile = 'organization.xml'
                    }
                            
                sh "java -jar jenkins-cli.jar -s http://localhost:32768/create-job ${params.Job_Name} < Pipeline_Creation_XML/${xmlFile}"
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
