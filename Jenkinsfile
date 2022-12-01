def app_version = ''
def app_name = ''
def jar_name = ''

pipeline {

    agent {
        label 'jdk-17'
    }

   options {
       disableConcurrentBuilds()
       buildDiscarder(logRotator(daysToKeepStr: '60', numToKeepStr: '10', artifactNumToKeepStr: '1'))
   }

    stages {
        stage('Build&Test') {
            steps {
                script {
                    withMaven(maven: 'maven-3.8.3') {
                        sh('mvn install -T 4 -Dmaven.test.failure.ignore=true')
                        pom = readMavenPom file: 'pom.xml'
                        app_version = pom.version
                        app_name = pom.name
                        jar_name = pom.name + '-' + pom.version
                    }
                }
            }
        }

/*        stage('Release without Branch') {
            environment {
                GIT_ACCESS = credentials('my-file-browser-app-credentials')
            }

            when {
                expression {
                    params.RELEASE_VERSION
                }
            }

            steps {
                // Set user info for git commit which are absent in jenkins
                sh('git config user.name "jenkins" && git config user.email "jenkins@ci-jenkins.apps.okd.fluff.chef.at"')
                sh('git remote set-url origin https://${GIT_ACCESS_USR}:${GIT_ACCESS_PSW}@' + env.GIT_URL.replace("https://", ""))
                sh('git config remote.origin.fetch "+refs/heads/*:refs/remotes/origin/*"')
                sh('git fetch --all')
                sh('git fetch origin refs/heads/main:refs/remotes/origin/main')
                // Set credentials for git push.
                // Workaround for jenkins: https://www.jenkins.io/doc/pipeline/examples/#push-git-repo
                withMaven(maven: 'maven-3.8.3') {
                    sh('mvn gitflow:release -Dmaven.test.failure.ignore=true '
                                         + '-DreleaseVersion=${RELEASE_VERSION} '
                                         + '-Drevision=${RELEASE_VERSION} '
                                         + '-DpostReleaseGoals=deploy')
                }
                script {
                    pom = readMavenPom file: 'pom.xml'
                    app_name = pom.name
                    app_version = RELEASE_VERSION
                    jar_name = pom.name + '-' + RELEASE_VERSION
                }
            }
        }*/

        stage('Create Image & deploy to Dev') {
            when {
                expression {
                    'SUCCESS' == currentBuild.currentResult && env.BRANCH_NAME == 'develop'
                }
            }
            steps {
                script {
                    withEnv(["PATH+OC=${tool 'oc-client-4.8.0'}"]) {
                        openshift.withCluster('fluff') {
                            openshift.withProject('aas-builds') {
                                apply = openshift.raw("apply", "--filename=./openshift/image-stream.yaml", "--namespace=aas-builds", "--wait").out.trim()
                                echo "OCP image stream configuration applied: ${apply}"
                                apply = openshift.raw("apply", "--filename=./openshift/build-config.yaml", "--namespace=aas-builds", "--wait").out.trim()
                                echo "OCP build configuration applied: ${apply}"
                                build = openshift.startBuild("${app_name}", "--from-file=target/${jar_name}.jar", "--wait").out.trim()
                                echo "Build finished: ${build}"
                                openshift.tag("${app_name}:latest", "${app_name}:v${app_version}")
                                echo "OCP tagged from latest to v${app_version}"
                            }
                            openshift.withProject('aas-dev') {
                                apply = openshift.raw("apply", "--filename=./openshift/application-config.yaml", "--namespace=aas-dev", "--wait").out.trim()
                                echo "OCP application config configuration map applied: ${apply}"
                                apply = openshift.raw("apply", "--filename=./openshift/route.yaml", "--namespace=aas-dev", "--wait").out.trim()
                                echo "OCP route configuration applied: ${apply}"
                                apply = openshift.raw("apply", "--filename=./openshift/service.yaml", "--namespace=aas-dev", "--wait").out.trim()
                                echo "OCP service configuration applied: ${apply}"
                                apply = openshift.raw("apply", "--filename=./openshift/service-actuator.yaml", "--namespace=aas-dev", "--wait").out.trim()
                                echo "OCP service (actuator) configuration applied: ${apply}"
                                apply = openshift.raw("apply", "--filename=./openshift/deployment.yaml", "--namespace=aas-dev", "--wait").out.trim()
                                echo "OCP deployment configuration applied: ${apply}"
                                restart = openshift.raw("rollout", "restart", "deploy/${app_name}")
                                echo "Restart finished: ${restart}"
                                deploy = openshift.selector("deploy", "${app_name}")
                                deployStatus = deploy.rollout().status()
                                echo "Rollout finished: ${deployStatus}"
                            }
                        }
                    }
                }
            }
        }
    }
}
