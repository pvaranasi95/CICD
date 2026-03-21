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
                sh "cd .."         
               sh """java -jar jenkins-cli.jar -http -auth pavanvaranasi95:11fa7390e7a1b0114123e7034528793f9f -s http://localhost:8080/ create-job ${params.Job_Name} < Pipeline_Creation_XML/pipeline.xml"""
              }
            }
      }
stage("Add Jobs to View") {
    steps {
        script {

            def app = params.APP_CODE
            def job = params.Job_Name
            def app_status = params.APP_CODE_STATUS
            sh """  
                if [ ${app_status} == "NEW" ]; then
                   java -jar jenkins-cli.jar -http -auth pavanvaranasi95:11fa7390e7a1b0114123e7034528793f9f -s http://localhost:8080/ create-view ${app} < Pipeline_Creation_XML/view.xml
                   echo "${app} view created."
                   java -jar jenkins-cli.jar -http -auth pavanvaranasi95:11fa7390e7a1b0114123e7034528793f9f -s http://localhost:8080/ add-job-to-view ${app} ${job}
                   echo "${job} added to ${app} view"
                else
                   echo "Adding Job To Existing View"
                   java -jar jenkins-cli.jar -http -auth pavanvaranasi95:11fa7390e7a1b0114123e7034528793f9f -s http://localhost:8080/ add-job-to-view ${app} ${job}
                fi
            """
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
