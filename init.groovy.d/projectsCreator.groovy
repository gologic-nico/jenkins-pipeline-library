import jenkins.model.*
import jenkins.install.*
import hudson.util.*
import hudson.model.UpdateSite
import hudson.model.TopLevelItem
import hudson.PluginWrapper
import hudson.PluginManager
import hudson.security.*
import hudson.model.labels.LabelAtom
import hudson.tasks.LogRotator
import hudson.triggers.SCMTrigger
import javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration
import jenkins.model.GlobalConfiguration

//Désactiver la sécurité DSL
GlobalConfiguration.all().get(GlobalJobDslSecurityConfiguration.class).useScriptSecurity=false
GlobalConfiguration.all().get(GlobalJobDslSecurityConfiguration.class).save()

String PIPELINE_LIB_ID = "PIPELIB"
String PIPELINE_DEFAULT_BRANCH = "master"
String PIPELINE_LIB_GIT_URL = "https://github.com/gologic-nico/jenkins-pipeline-library.git"
String MAVEN_SETTINGS_ABSOLUTE_PATH = "/var/lib/jenkins/jenkins-pipeline-library/ressources/settings.xml"

def pluginsList = ["ace-editor","analysis-core","ant","antisamy-markup-formatter","apache-httpcomponents-client-4-api","authentication-tokens","blueocean","blueocean-autofavorite","blueocean-bitbucket-pipeline","blueocean-commons","blueocean-config","blueocean-core-js","blueocean-dashboard","blueocean-display-url","blueocean-events","blueocean-github-pipeline","blueocean-git-pipeline","blueocean-i18n","blueocean-jira","blueocean-jwt","blueocean-personalization","blueocean-pipeline-api-impl","blueocean-pipeline-editor","blueocean-pipeline-scm-api","blueocean-rest","blueocean-rest-impl","blueocean-web","bouncycastle-api","branch-api","build-timeout","cloudbees-bitbucket-branch-source","cloudbees-folder","command-launcher","config-file-provider","credentials","credentials-binding","display-url-api","docker-commons","docker-java-api","docker-plugin","docker-workflow","durable-task","email-ext","favorite","git","git-client","github","github-api","github-branch-source","git-server","gradle","handlebars","handy-uri-templates-2-api","htmlpublisher","jackson2-api","javadoc","jdk-tool","jenkins-design-language","jira","jquery-detached","jsch","junit","ldap","lockable-resources","mailer","mapdb-api","matrix-auth","matrix-project","maven-plugin","mercurial","momentjs","msbuild","pam-auth","pipeline-build-step","pipeline-github-lib","pipeline-graph-analysis","pipeline-input-step","pipeline-milestone-step","pipeline-model-api","pipeline-model-declarative-agent","pipeline-model-definition","pipeline-model-extensions","pipeline-rest-api","pipeline-stage-step","pipeline-stage-tags-metadata","pipeline-stage-view","plain-credentials","pubsub-light","resource-disposer","scm-api","script-security","sse-gateway","ssh-credentials","ssh-slaves","structs","subversion","timestamper","token-macro","trilead-api","variant","workflow-aggregator","workflow-api","workflow-basic-steps","workflow-cps","workflow-cps-global-lib","workflow-durable-task-step","workflow-job","workflow-multibranch","workflow-scm-step","workflow-step-api","workflow-support","ws-cleanup", "robot", "job-dsl"]

println "---------------------------------------------------------------"
println "-"
println "- Run JENINS_HOME/jenkins-pipeline-library/init.groovy.d/projectsCreator.groovy"
println "-"
println "---------------------------------------------------------------"

def JENKINS = Jenkins.get()

String dslGeneratorJobName = "DSL-Generator"
TopLevelItem dslGeneratorJob = JENKINS.getItem(dslGeneratorJobName)

if(dslGeneratorJob == null){
	dslGeneratorJob = JENKINS.createProject(hudson.model.FreeStyleProject.class, dslGeneratorJobName)
	dslGeneratorJob.setDescription("Générateur DSL de tâches Jenkins")
	dslGeneratorJob.setAssignedLabel(new LabelAtom("master"))

	LogRotator logRotator = new LogRotator(-1, 50, -1, -1)
	dslGeneratorJob.setLogRotator(logRotator)
	
	SCMTrigger scmTrigger = new SCMTrigger("* * * * *")
	scmTrigger.start(dslGeneratorJob, true)
	dslGeneratorJob.addTrigger(scmTrigger)
	
	List<hudson.plugins.git.BranchSpec> branches = [
		new hudson.plugins.git.BranchSpec(PIPELINE_DEFAULT_BRANCH)
	]
	dslGeneratorJob.setScm(new hudson.plugins.git.GitSCM(hudson.plugins.git.GitSCM.createRepoList(PIPELINE_LIB_GIT_URL, ""), branches, false, null, null, null, null))

	javaposse.jobdsl.plugin.ExecuteDslScripts executeDslScripts = new javaposse.jobdsl.plugin.ExecuteDslScripts()
	executeDslScripts.setTargets("src/mtl/devops/dslGeneratorJobs.groovy")
	executeDslScripts.setRemovedJobAction(javaposse.jobdsl.plugin.RemovedJobAction.DELETE)
	executeDslScripts.setRemovedViewAction(javaposse.jobdsl.plugin.RemovedViewAction.DELETE)
	executeDslScripts.setRemovedConfigFilesAction(javaposse.jobdsl.plugin.RemovedConfigFilesAction.DELETE)
	executeDslScripts.setAdditionalClasspath("src")
	dslGeneratorJob.getBuildersList().add(executeDslScripts)

	dslGeneratorJob.save()
	dslGeneratorJob.scheduleBuild(0)
}

JENKINS.save()

