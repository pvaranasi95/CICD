pipeline {
    agent any

    tools {
        jdk 'JDK11'
        maven 'Maven'
    }

    environment {
        CONFIG_REPO       = "https://github.com/pvaranasi95/CICD.git"
        CONFIG_BRANCH     = "main"
        CONFIG_FILE       = "Properties/Adressbook_Properies.yaml"
        JIRA_URL          = 'https://pavan-varanasi.atlassian.net'
        JIRA_PROJECT      = 'DEVOPS'              // Jira project key
        JIRA_ISSUE_TYPE   = 'Task'               // Valid Jira issue type
        JIRA_CREDENTIALS  = 'Jira'               // Jenkins Jira credential ID
        BUILD_OUTPUT_DIR  = "${env.WORKSPACE}\\Builds"
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
            script { safeCreateJira("Build Successful", currentBuild) }
        }
        failure {
            script { safeCreateJira("Build Failed", currentBuild) }
        }
        unstable {
            script { safeCreateJira("Build Unstable", currentBuild) }
        }
    }
}

// Safe Jira creation with error handling
def safeCreateJira(String title, build) {
    try {
        def description = """Jenkins Job: ${build.fullDisplayName}
Build Number: ${build.number}
Status: ${build.currentResult}
URL: ${build.absoluteUrl}"""

        createJiraIssue(title, description)
    } catch (err) {
        echo "⚠️ Failed to create Jira issue: ${err}"
    }
}

// Create Jira issue using Jenkins Jira plugin
def createJiraIssue(String summary, String description) {
    def issue = createJiraIssue(
        site: 'Jira',  // Jira site ID in Jenkins
        issue: [
            fields: [
                project   : [key: env.JIRA_PROJECT],
                summary   : summary,
                description: description,
                issuetype : [name: env.JIRA_ISSUE_TYPE]
            ]
        ]
    )

    echo "✅ Jira issue created: ${issue.key}"
}
