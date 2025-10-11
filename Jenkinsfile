pipeline {
    agent any

    }

    tools {
        jdk 'JDK11'
        maven 'Maven'
    }

    environment {
        BUILD_OUTPUT_BASE = "${env.WORKSPACE}/${params.FOLDER_NAME}"
    }

    stages {

        stage('Run Only for Release Branches') {
            when {
                expression { env.BRANCH_NAME.startsWith('release/') }
            }
            steps {
                script {
                    echo "✅ Running pipeline for release branch: ${env.BRANCH_NAME}"
                }
            }
        }

        stage('Load Config') {
            when {
                expression { env.BRANCH_NAME.startsWith('release/') }
            }
            steps {
                script {
                    // Checkout the config repo
                    dir("cicd-config") {
                        checkout([$class: 'GitSCM',
                            branches: [[name: 'main']],
                            userRemoteConfigs: [[url: 'https://github.com/pvaranasi95/CICD.git']]
                        ])
                    }

                    // Load YAML config for this job
                    def configFile = "cicd-config/Properties/${env.JOB_NAME}_Properies.yaml"
                    def props = readYaml file: configFile
                    echo "✅ Loaded config from ${configFile}"

                    // Local variables
                    BUILD_WORKDIR     = props.workspace ?: "source-code"
                    SOURCE_REPO       = props.git_repo_url
                    SOURCE_BRANCH     = props.git_branch ?: env.BRANCH_NAME
                    ARTIFACTORY_URL   = props.artifactory_url
                    ARTIFACTORY_REPO  = props.artifactory_repo ?: "default-repo"
                    ARTIFACTORY_CREDS = props.artifactory_credentials
                }
            }
        }

        stage('Checkout Source Code') {
            when {
                expression { env.BRANCH_NAME.startsWith('releas_
