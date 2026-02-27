pipeline {
    agent any

    environment {
        BUILD_OUTPUT_DIR = "${env.WORKSPACE}/Builds"
        CONFIG_REPO      = "https://github.com/pvaranasi95/CICD.git"
        CONFIG_BRANCH    = "main"
    }

    stages {
        stage('Load Build Configuration') {
            steps {
                dir('CICD') {
                    git branch: 'main', url: 'https://github.com/pvaranasi95/CICD.git'
                }

                script {
                    // Determine job name for properties
                    def cleanJobName = env.JOB_NAME.split('/')[0].split('@')[0]
                    echo "Job short name: ${cleanJobName}"
                    env.CLEAN_JOB_NAME = cleanJobName

                    // YAML path
                    def yamlPath = "${env.WORKSPACE}/CICD/Properties/${cleanJobName}_Properties.yaml"
                    echo "yamlPath"

                    if (!fileExists(yamlPath)) {
                        error "❌ Config file not found: ${yamlPath}"
                    }

                    // Read properties
                    def props = readYaml file: yamlPath

                    // Set env vars
                    env.SOURCE_REPO       = props.git_repo_url
                    env.SOURCE_BRANCH     = props.git_branch ?: "main"
                    env.BUILD_WORKDIR     = props.workspace ?: "release-source"
                    env.ARTIFACTORY_REPO  = props.artifactory_repo ?: "default-repo"
                    env.ARTIFACTORY_URL   = props.artifactory_url
                    env.ARTIFACTORY_CREDS = props.artifactory_credentials
                    env.EMAIL_NOTIFY      = props.email_notify

                    // Stages to run
                    stagesToRun = props.stages_to_run ?: ["checkout", "build", "test", "package", "upload"]

                    echo "SOURCE_REPO     : ${env.SOURCE_REPO}"
                    echo " "
                    echo "SOURCE_BRANCH   : ${env.SOURCE_BRANCH}"
                    echo " "
                    echo "ARTIFACTORY_REPO: ${env.ARTIFACTORY_REPO}"
                    echo " "
                    echo "ARTIFACTORY_URL : ${env.ARTIFACTORY_URL}"
                    echo " "
                    echo "EMAIL_NOTIFY    : ${env.EMAIL_NOTIFY}"
                    echo " "
                    echo "Stages to run   : ${stagesToRun}"
                }
            }
        }

        stage('Checkout Source') {
            when { expression { stagesToRun.contains('checkout') } }
            steps {
                dir(env.BUILD_WORKDIR) {
                    checkout([$class: 'GitSCM',
                        branches: [[name: "*/${env.SOURCE_BRANCH}"]],
                        userRemoteConfigs: [[url: env.SOURCE_REPO]]
                    ])
                    echo "✅ Checked out ${env.SOURCE_BRANCH}"
                }
            }
        }

        stage('Build Application') {
            when { expression { stagesToRun.contains('build') } }
            steps {
                dir(env.BUILD_WORKDIR) {
                    sh 'mvn clean install -DskipTests'
                    echo "✅ Build completed"
                }
            }
        }

        stage('Run Tests') {
            when { expression { stagesToRun.contains('test') } }
            steps {
                dir(env.BUILD_WORKDIR) {
                    sh 'mvn test'
                    echo "✅ Tests executed"
                }
            }
        }

        stage('Package Artifact') {
            when { expression { stagesToRun.contains('package') } }
            steps {
                script {
                    env.ZIP_FILE_PATH = "${env.BUILD_OUTPUT_DIR}/${env.CLEAN_JOB_NAME}-${env.BUILD_NUMBER}.zip"
                    sh """
                    mkdir -p ${env.BUILD_OUTPUT_DIR}
                    zip -r ${env.ZIP_FILE_PATH} ${env.WORKSPACE}/${env.BUILD_WORKDIR}/target/*
                    """
                    echo "✅ Artifact packaged: ${env.ZIP_FILE_PATH}"
                }
            }
        }

        stage('Upload to Artifactory') {
            when { expression { stagesToRun.contains('upload') } }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: env.ARTIFACTORY_CREDS,
                    usernameVariable: 'ART_USER',
                    passwordVariable: 'ART_PASS'
                )]) {
                    sh """
                    curl -u $ART_USER:$ART_PASS -T "${env.ZIP_FILE_PATH}" \
                    "${env.ARTIFACTORY_URL}/artifactory/${env.ARTIFACTORY_REPO}/${env.CLEAN_JOB_NAME}/${env.BUILD_NUMBER}/${env.CLEAN_JOB_NAME}.zip"
                    """
                    echo "✅ Uploaded to Artifactory"
                }
            }
        }
        stage('clean workspace') {
            steps {
                cleanWs()
                echo "Workspace cleaning done"
                sh "ls -ltr"
                sh "rm -rf ."
            }
        }        
    }

post {
failure {
    script {
        try {
            def response = jiraNewIssue(
                site: "Jira",
                issue: [
                    fields: [
                        project: [ key: "JIRA" ],
                        summary: "Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        description: """
Jenkins Job Failed

Job: ${env.JOB_NAME}
Build Number: ${env.BUILD_NUMBER}
URL: ${env.BUILD_URL}
""",
                        issuetype: [ name: "Task" ]
                    ]
                ]
            )

            if(response?.data?.key) {
                env.issueKey = response.data.key
                def issueKey = env.issueKey

                echo "Created Jira issue: ${issueKey}"

                if(fileExists("${env.WORKSPACE}/build.log")) {
                    jiraUploadAttachment(
                        site: "Jira",
                        issueKey: issueKey,
                        file: "${env.WORKSPACE}/build.log"
                    )
                    echo "Attached build.log to ${issueKey}"
                }
            }

        } catch(Exception e) {
            echo "Jira creation failed: ${e}"
        }
                    // Prepare JSON for Elasticsearch
           def jenkinsBuildData = [
    job_name    : env.CLEAN_JOB_NAME ?: env.JOB_NAME,
    build_number: env.BUILD_NUMBER.toInteger(),
    status      : currentBuild.currentResult,
    timestamp   : new Date().format(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    TimeZone.getTimeZone('UTC')
                  ),
    duration    : currentBuild.duration,
    url         : env.BUILD_URL,
    jira_key    : env.issueKey ?: ""
]

            def jsonBody = groovy.json.JsonOutput.toJson(jenkinsBuildData)

            echo "Sending build data to Elasticsearch"

            sh """
                curl -X POST "http://host.docker.internal:9200/jenkins/_doc" \
                     -H "Content-Type: application/json" \
                     -d '${jsonBody}' || true
            """
    }
}
        success {
        script {

            // Prepare JSON for Elasticsearch
           def jenkinsBuildData = [
    job_name    : env.CLEAN_JOB_NAME ?: env.JOB_NAME,
    build_number: env.BUILD_NUMBER.toInteger(),
    status      : currentBuild.currentResult,
    timestamp   : new Date().format(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    TimeZone.getTimeZone('UTC')
                  ),
    duration    : currentBuild.duration,
    url         : env.BUILD_URL
]

            def jsonBody = groovy.json.JsonOutput.toJson(jenkinsBuildData)

            echo "Sending build data to Elasticsearch"

            sh """
                curl -X POST "http://host.docker.internal:9200/jenkins/_doc" \
                     -H "Content-Type: application/json" \
                     -d '${jsonBody}' || true
            """
        }
    }


    always {
        script {
            // Send Email
            emailext(
                subject: "Build ${currentBuild.currentResult} for: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """Hi,

Your Jenkins build status is: ${currentBuild.currentResult}

Job: ${env.JOB_NAME}
Build Number: ${env.BUILD_NUMBER}

Check details here:
${env.BUILD_URL}
""",
                to: "${env.EMAIL_NOTIFY}",
            )
        }
    }

   }
}
