#!/usr/bin/env groovy
pipeline{
	agent any
	stages{
		stage('Build'){
			steps{
				echo currentBuild.displayName
			}
		}
		stage('Test'){
			steps{
				echo currentBuild.displayName
			}
		}
		stage('Deploy'){
			steps{
				echo currentBuild.displayName
			}
		}
	}
	
}

node{
	stage('Build'){
		echo 'Build...'
	}
	stage('Test'){
		echo 'Test...'
	}
	stage('Deploy'){
		echo 'Deploy...'
	}
	
}