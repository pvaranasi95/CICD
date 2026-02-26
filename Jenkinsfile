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
                    echo "Stages to run: ${stagesToRun}"
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
    }

    post {
        always {
            script {
                def jenkinsBuildData = [
                    job_name: env.CLEAN_JOB_NAME,
                    build_number: env.BUILD_NUMBER.toInteger(),
                    status: currentBuild.currentResult,
                    timestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", TimeZone.getTimeZone('UTC')),
                    duration: currentBuild.duration,
                    url: env.BUILD_URL
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
        always{
            emailext subject: "Build ${BUILD_STATUS} for: ${JOB_NAME} #${BUILD_NUMBER}",
    body: """Hi,

Your Jenkins build status is: ${BUILD_STATUS}

Job: ${JOB_NAME}
Build Number: ${BUILD_NUMBER}

Check details here:
${BUILD_URL}
""",
                to: "${env.EMAIL_NOTIFY},
                from: "pavanvaranasi95@gmail.com"
       }
    }
}
