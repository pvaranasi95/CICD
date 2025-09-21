pipeline {
    agent any

    tools {
        jdk 'JDK11'
        maven 'Maven'
    }

    environment {
        CONFIG_REPO   = "https://github.com/pvaranasi95/CICD.git"
        CONFIG_BRANCH = "main"
        CONFIG_FILE   = "Properties/Adressbook_Properies.yaml"
        JIRA_URL = 'https://pavan-varanasi.atlassian.net'
        JIRA_PROJECT = 'DevOps'
        JIRA_ISSUE_TYPE = 'Status'
        JIRA_CREDENTIALS = 'Jira-credentials'
    }

    stages {
        stage('Read Config') {
            steps {
                dir("cicd-config") {
                    checkout([$class: 'GitSCM',
                        branches: [[name: env.CONFIG_BRANCH]],
                        userRemoteConfigs: [[url: env.CONFIG_REPO]]
                    ])

                    script {
                        def props = readYaml file: "${env.CONFIG_FILE}"

                        env.SOURCE_REPO      = props.git_repo_url
                        env.SOURCE_BRANCH    = props.git_branch ?: "main"
                        env.BUILD_WORKDIR    = props.workspace
                        env.ARTIFACTORY_REPO = props.artifactory_repo
                        env.ARTIFACTORY_URL  = props.artifactory_url
                        env.ARTIFACTORY_CREDS= props.artifactory_credentials
                        env.EMAIL_NOTIFY     = props.email_notify

                        echo "✅ Loaded config from ${env.CONFIG_FILE}"
                    }
                }
            }
        }

        stage('Checkout Source Code') {
            steps {
                dir("source-code") {
                    checkout([$class: 'GitSCM',
                        branches: [[name: "*/${env.SOURCE_BRANCH}"]],
                        userRemoteConfigs: [[url: "${env.SOURCE_REPO}"]]
                    ])
                }
            }
        }

        stage('Maven Build') {
            steps {
                dir("source-code") {
                    bat "mvn clean install"
                }
            }
        }

        stage('Zip Build Output') {
            steps {
                powershell """
                \$source = "${env.WORKSPACE}\\source-code\\target"
                \$destination = "C:\\Users\\pavan\\OneDrive\\Desktop\\DevOps\\Jenkins\\Builds\\${env.JOB_NAME}-${env.BUILD_NUMBER}.zip"

                if (Test-Path \$destination) { Remove-Item \$destination -Force }

                Compress-Archive -Path "\$source\\*" -DestinationPath \$destination
                """
            }
        }

        stage('Upload to Artifactory') {
            steps {
                withCredentials([usernamePassword(credentialsId: "${env.ARTIFACTORY_CREDS}", usernameVariable: 'ART_USER', passwordVariable: 'ART_PASS')]) {
                    bat """
                    curl -v -u %ART_USER%:%ART_PASS% -T "C:\\Users\\pavan\\OneDrive\\Desktop\\DevOps\\Jenkins\\Builds\\${env.JOB_NAME}-${env.BUILD_NUMBER}.zip" ^
                    "${env.ARTIFACTORY_URL}/artifactory/${env.ARTIFACTORY_REPO}/${env.JOB_NAME}/${env.BUILD_NUMBER}/${env.JOB_NAME}.zip"
                    """
                }
            }
        }
    }
       post {
        success {
            script {
                createJiraIssue("Build Successful", "Build completed successfully.")
            }
        }
        failure {
            script {
                createJiraIssue("Build Failed", "Build failed. Check Jenkins console.")
            }
        }
        unstable {
            script {
                createJiraIssue("Build Unstable", "Build is unstable. Review test results.")
            }
        }
    }
}

// Function to create Jira Issue
def createJiraIssue(String summary, String description) {
    def payload = [
        fields: [
            project: [key: env.JIRA_PROJECT],
            summary: summary,
            description: description,
            issuetype: [name: env.JIRA_ISSUE_TYPE]
        ]
    ]

    def response = httpRequest(
        httpMode: 'POST',
        contentType: 'APPLICATION_JSON',
        acceptType: 'APPLICATION_JSON',
        url: "${env.JIRA_URL}/rest/api/3/issue",
        authentication: 'Jira-credentials', // Jenkins credentials ID
        requestBody: groovy.json.JsonOutput.toJson(payload)
    )

    echo "Jira response: ${response.content}"
}
}
