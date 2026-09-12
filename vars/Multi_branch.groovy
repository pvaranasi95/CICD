def call ()
pipeline {
  agent any
  stages {
    stage('Check Diff') {
      steps {
        sh "git fetch origin master"
         sh "git diff --name-only FETCH_HEAD...HEAD > file.txt"
        sh "cat file.txt"
      }
    }
    stage('Package') {
      steps {
        sh '''
             mkdir -p build
             while read -r file
             do
               cp --parents $file ./build/ 
             done < file.txt
             
             find ./build/ -type d -mindepth 1 | awk -F '/' {'print $3'} > file1.txt
             while read -r file1
             do
               rm -rf $file1
               cp -r ./build/$file1 .
               zip -r ./$file1.zip ./$file1
             done < file1.txt
               
        '''
      }
    }
    stage('Publish to Artifactory') {
        environment {
        ARTIFACTORY_CRED = credentials('Jfrog_Artifactory')
    }
          steps {
           sh '''
                    rm -rf ./build/
                    find . -type f -name "*.zip" > folder.txt
                    while read -r name
                    do
                     echo "Pusblishing to Artifactory"     
                     curl -X PUT \
                        -u "$ARTIFACTORY_CRED_USR:$ARTIFACTORY_CRED_PSW" \
                        --upload-file "$name" \
                        "http://host.docker.internal:8082/artifactory/DevOps/${JOB_NAME}/${BUILD_NUMBER}/$name"
                   done < folder.txt
        '''
    }
}
    stage('clean ws') {
      steps {
        cleanWs()
      }
    }
               
  }
}
