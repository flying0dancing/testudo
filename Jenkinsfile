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
			when{
				expression{
					currentBuild.result==null||currentBuild.result=='SUCCESS'
				}
			}
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
		if(currentBuild.result==null||currentBuild.result=='SUCCESS'){
			echo 'Deploy...'
		}
	}
	
}