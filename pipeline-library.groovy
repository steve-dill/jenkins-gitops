// pipelineLibrary.groovy
def call(Map config = [:]) {
    pipeline {
        agent any
        stages {
            stage('Build') {
                steps {
                    script {
                        // Placeholder for build logic
                        echo 'Running Build Stage...'
                    }
                }
            }
            stage('Test') {
                steps {
                    script {
                        // Placeholder for test logic
                        echo 'Running Test Stage...'
                    }
                }
            }
            stage('Create OpenShift ArgoCD Application') {
                steps {
                    script {
                        // Load Helm values and ArgoCD configuration from the values.yml file
                        def helmValues = readYaml(file: config.helmValuesPath ?: 'values.yml')

                        // Extract dynamic parameters from values.yml
                        def appName = helmValues.argocdApp.name ?: 'default-app'
                        def appNamespace = helmValues.argocdApp.namespace ?: 'openshift-gitops'
                        def repoURL = helmValues.argocdApp.repoURL ?: 'https://example-repo.git'
                        def targetRevision = helmValues.argocdApp.targetRevision ?: 'HEAD'
                        def path = helmValues.argocdApp.path ?: 'helm-chart'

                        // Write the overrides to a separate file (e.g., overrides.yml)
                        writeYaml(file: 'overrides.yml', data: helmValues)

                        // Load the ArgoCD template from the library
                        def argocdTemplatePath = "${libraryResource('argocd-template.yaml')}"
                        def argocdTemplate = readFile(argocdTemplatePath)

                        // Replace placeholders in the ArgoCD template
                        def argocdApp = argocdTemplate
                            .replaceAll(/__APP_NAME__/, appName)
                            .replaceAll(/__NAMESPACE__/, appNamespace)
                            .replaceAll(/__REPO_URL__/, repoURL)
                            .replaceAll(/__TARGET_REVISION__/, targetRevision)
                            .replaceAll(/__PATH__/, path)

                        // Save the final ArgoCD application YAML
                        writeFile(file: 'argocd-application.yaml', text: argocdApp)
                        echo "ArgoCD Application YAML created for ${appName} in namespace ${appNamespace}."

                        // Apply the ArgoCD application using OpenShift CLI
                        sh "oc apply -f argocd-application.yaml"
                    }
                }
            }
        }
        post {
            success {
                echo 'Pipeline completed successfully on OpenShift!'
            }
            failure {
                echo 'Pipeline failed on OpenShift.'
            }
        }
    }
}
