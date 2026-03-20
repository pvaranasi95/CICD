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

                sh '''
                    java -jar jenkins-cli.jar -http -auth pavanvaranasi95:11fa7390e7a1b0114123e7034528793f9f -s http://localhost:8080/ get-view ${params.APP_CODE}
                    '''
                //     if [ "$CHECK_VIEW" == "${params.APP_CODE}" ]; then
                //          echo "View already exists. Adding new job to ${params.APP_CODE} view."
                                    // CHECK_VIEW=$(java -jar jenkins-cli.jar -http -auth pavanvaranasi95:11fa7390e7a1b0114123e7034528793f9f -s http://localhost:8080/ get-view ${params.APP_CODE})
                          
                //     else
                //         echo "View not exists. Creating new view"
                //         java -jar jenkins-cli.jar -http -auth pavanvaranasi95:11fa7390e7a1b0114123e7034528793f9f -s http://localhost:8080/ create-view < Pipeline_Creation_XML/view.xml
                //     fi   


                // echo "Adding Job To View"
                // java -jar jenkins-cli.jar -http -auth pavanvaranasi95:11fa7390e7a1b0114123e7034528793f9f -s http://localhost:8080/ add-job-to-view ${params.APP_CODE} ${params.Job_Name}
                // '''
                }
            }
        }
                
        //  stage('clean workspace') {
        //     steps {
        //         cleanWs()
        //         echo "Workspace cleaning done"
        //     }
        // }        
                
    }
}
