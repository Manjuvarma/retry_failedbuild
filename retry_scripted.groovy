#!groovy

timestamps {
    properties([parameters([string(defaultValue: '0', description: 'No of Times you want to retry?', name: 'No', trim: false)])])
node {
    try {

        stage('Checkout') {
            checkout([$class: 'GitSCM', branches: [[name: '*/main']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '', url: 'https://github.com/avinash0106/retry_failedbuild.git']]])
        }

        stage('Build') {
//            bat 'mvn clean install -DskipTests'
            bat 'mvn clean package'
        }

        stage('Unit Test') {
            bat 'mvn test'
        }

        stage('Integration Test') {
            bat 'mvn verify -DskipUnitTests -Parq-wildfly-swarm'
        }

//         stage('SonarQube Analysis') {
//             withSonarQubeEnv(credentialsId: 'sonarscanner') {
//                 sh 'mvn sonar:sonar'
//             }
//         }
        
        stage('Archive Artifacts') {
            archiveArtifacts allowEmptyArchive: false, artifacts: 'target//*.war', caseSensitive: true, defaultExcludes: true, fingerprint: false, onlyIfSuccessful: false
        }
        
    } catch (err) {
        echo 'Pipeline failed'
        currentBuild.result = 'FAILURE'
    }
    
    if ("${params.No}" == "1" && currentBuild.result == 'FAILURE') {
        echo "Aborting the build due to failed Retry!"
        build.getExecutor().interrupt(Result.ABORTED)
    }
    
    if (currentBuild.result == 'FAILURE') {
        echo "Retrying Job!"
//         currentBuild.addAction(upstreamBuildRun.getAction(hudson.model.ParametersAction))
        build quietPeriod: 300, job: "${JOB_NAME}" 
       }
    }
}

