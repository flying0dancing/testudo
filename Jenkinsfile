#!/usr/bin/env groovy
pipeline{
	agent any

	options { skipDefaultCheckout true }

	environment {
		JAVA_HOME = tool 'OracleJDK8'
		TEAMS_CHANNEL_URL = 'https://outlook.office.com/webhook/edee457e-63bc-4e9d-8eca-750bbaa3045c@4a78f2c0-297f-426d-b09f-5986924d38e7/IncomingWebhook/7e1ca379466c4e8ab0062ef2f0f605b7/e3ad24e4-1f66-4d7c-aed5-11e37953f5d0'
	}

	stages {
		stage('Setup') {
			steps {
				setupBuild()
			}
		}
		stage('Version') {
			steps {
				generateVersion()
			}
		}
		stage('Unit Tests & S3 Temp') {
			steps {
				runBuild()
			}
			post {
				always {
					junit '**/target/*-reports/*.xml'
				}
			}
		}
		stage('Sonar & Release') {
			parallel {
				stage('SonarQube Analysis') {
					steps {
						runSonarAnalysis()
					}
				}
				stage('Deploy & Tag') {
					steps {
						deployToArtifactory()

						tagBuild()
					}
				}
			}
		}
	}

}

void setupBuild() {
	checkout scm

	BUILD_INFO.env.capture = true
}

void generateVersion() {
	echo "Update artifacts to version [$BUILD_VERSION]"

	notifier.runAndNotifyOnFailure('Failed to generate and update versions') {
		def commitBuildVersion = "versions:set versions:commit -DnewVersion=$BUILD_VERSION".toString()

		MVN.run goals: commitBuildVersion, pom: 'pom.xml'
	}
}


void runBuild() {
	MVN.run goals: 'clean install', pom: 'pom.xml', buildInfo: BUILD_INFO
}

void runSonarAnalysis() {
	notifier.runAndNotifyOnFailure('Failed Sonar analysis') {
		MVN.run goals: 'sonar:sonar', pom: 'pom.xml'
	}
}

void deployToArtifactory() {
	notifier.runAndNotifyOnFailure('Failed deploying to Artifactory') {
		MVN.deployer.deployArtifacts BUILD_INFO

		maven.artifactoryServer().publishBuildInfo BUILD_INFO
	}
}

void tagBuild() {
	bitbucketTag tag: BUILD_VERSION, projectPath: 'projects/CPROD/repos/testudo'

	currentBuild.displayName = BUILD_VERSION
}
