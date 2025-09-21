pipeline {
    agent any

    tools {
        jdk 'JDK11'
        maven 'Maven'
    }

    environment {
        CONFIG_REPO    = "https://github.com/pvaranasi95/CICD.git"
        CONFIG_BRANCH  = "main"
        CONFIG_FILE    = "Properties/Adressbook_Properies.yaml"
        JIRA_URL       = 'https://pavan-varanasi.atlassian.net'
        JIRA_PROJECT   = 'DevOps'
        JIRA_ISSUE_TYPE= 'Status'
        JIRA_CREDENTIALS = 'Jira-credentials'
        BUILD_OUTPUT_DIR = "${env.WORKSPACE}\\Builds"
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

                        env.SOURCE_REPO       = props.git_repo_url
                        env.SOURCE_BRANCH     = props.git_branch ?: "main"
                        env.BUILD_WORKDIR     = props.workspace ?: "source-code"
                        env.ARTIFACTORY_REPO  = props.artifactory_repo ?: "default-repo"
                        env.ARTIFACTORY_URL   = props.artifactory_url
                        env.ARTIFACTORY_CREDS = props.artifactory_credentials
                        env.EMAIL_NOTIFY      = props.email_notify

                        echo "✅ Loaded config from ${env.CONFIG_FILE}"
                    }
                }
            }
        }

        stage('Checkout Source Code') {
            steps {
                dir(env.BUILD_WORKDIR) {
                    checkout([$class: 'GitSCM',
                        branches: [[name: "*/${env.SOURCE_BRANCH}"]],
                        userRemoteConfigs: [[url: "${env.SOURCE_REPO}"]]
                    ])
                }
            }
        }

        stage('Maven Build') {
            steps {
                dir(env.BUILD_WORKDIR) {
                    bat "mvn clean install"
                }
            }
        }

        stage('Zip Build Output') {
            steps {
                script {
                    def zipFile = "${env.BUILD_OUTPUT_DIR}\\${env.JOB_NAME}-${env.BUILD_NUMBER}.zip"
                    powershell """
                        if (!(Test-Path -Path '${env.BUILD_OUTPUT_DIR}')) { New-Item -ItemType Directory -Path '${env.BUILD_OUTPUT_DIR}' }
                        Compress-Archive -Path '${env.WORKSPACE}\\${env.BUILD_WORKDIR}\\target\\*' -DestinationPath '${zipFile}' -Force
                    """
                    env.ZIP_FILE_PATH = zipFile
                    echo "✅ Build output zipped to ${zipFile}"
                }
            }
        }

        stage('Upload to Artifactory') {
            steps {
                withCredentials([usernamePassword(credentialsId: env.ARTIFACTORY_CREDS, usernameVariable: 'ART_USER', passwordVariable: 'ART_PASS')]) {
                    bat """
                        curl -u %ART_USER%:%ART_PASS% -T "${env.ZIP_FILE_PATH}" ^
                        "${env.ARTIFACTORY_URL}/artifactory/${env.ARTIFACTORY_REPO}/${env.JOB_NAME}/${env.BUILD_NUMBER}/${env.JOB_NAME}.zip"
                    """
                }
            }
        }
    }

    post {
        success {
            script { safeCreateJira("Build Successful", "Build completed successfully.") }
        }
        failure {
            script { safeCreateJira("Build Failed", "Build failed. Check Jenkins console.") }
        }
        unstable {
            script { safeCreateJira("Build Unstable", "Build is unstable. Review test results.") }
        }
    }
}

// Safe Jira creation with error handling
def safeCreateJira(String summary, String description) {
    try {
        createJiraIssue(summary, description)
    } catch (err) {
        echo "⚠️ Failed to create Jira issue: ${err}"
    }
}

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
        authentication: env.JIRA_CREDENTIALS,
        requestBody: groovy.json.JsonOutput.toJson(payload)
    )

    echo "✅ Jira response: ${response.content}"
}
