import jenkins.model.*
import hudson.util.*
import jenkins.install.*
import hudson.model.UpdateSite
import hudson.PluginWrapper
import hudson.PluginManager
import hudson.security.*

String PIPELINE_LIB_ID = "PIPELIB"
String PIPELINE_DEFAULT_BRANCH = "master"
String PIPELINE_LIB_GIT_URL = "https://github.com/gologic-nico/jenkins-pipeline-library.git"
String JENKINS_ADMIN_USERNAME = "admin"
String JENKINS_ADMIN_PASSWORD = "admin"
String MAVEN_SETTINGS_ABSOLUTE_PATH = "/var/lib/jenkins/jenkins-pipeline-library/ressources/settings.xml"

def pluginsList = ["ace-editor","analysis-core","ant","antisamy-markup-formatter","apache-httpcomponents-client-4-api","authentication-tokens","blueocean","blueocean-autofavorite","blueocean-bitbucket-pipeline","blueocean-commons","blueocean-config","blueocean-core-js","blueocean-dashboard","blueocean-display-url","blueocean-events","blueocean-github-pipeline","blueocean-git-pipeline","blueocean-i18n","blueocean-jira","blueocean-jwt","blueocean-personalization","blueocean-pipeline-api-impl","blueocean-pipeline-editor","blueocean-pipeline-scm-api","blueocean-rest","blueocean-rest-impl","blueocean-web","bouncycastle-api","branch-api","build-timeout","cloudbees-bitbucket-branch-source","cloudbees-folder","command-launcher","config-file-provider","credentials","credentials-binding","display-url-api","docker-commons","docker-java-api","docker-plugin","docker-workflow","durable-task","email-ext","favorite","git","git-client","github","github-api","github-branch-source","git-server","gradle","handlebars","handy-uri-templates-2-api","htmlpublisher","jackson2-api","javadoc","jdk-tool","jenkins-design-language","jira","jquery-detached","jsch","junit","ldap","lockable-resources","mailer","mapdb-api","matrix-auth","matrix-project","maven-plugin","mercurial","momentjs","msbuild","pam-auth","pipeline-build-step","pipeline-github-lib","pipeline-graph-analysis","pipeline-input-step","pipeline-milestone-step","pipeline-model-api","pipeline-model-declarative-agent","pipeline-model-definition","pipeline-model-extensions","pipeline-rest-api","pipeline-stage-step","pipeline-stage-tags-metadata","pipeline-stage-view","plain-credentials","pubsub-light","resource-disposer","scm-api","script-security","sse-gateway","ssh-credentials","ssh-slaves","structs","subversion","timestamper","token-macro","trilead-api","variant","workflow-aggregator","workflow-api","workflow-basic-steps","workflow-cps","workflow-cps-global-lib","workflow-durable-task-step","workflow-job","workflow-multibranch","workflow-scm-step","workflow-step-api","workflow-support","ws-cleanup", "robot"]

println "---------------------------------------------------------------"
println "-"
println "- Run JENINS_HOME/jenkins-pipeline-library/init.groovy"
println "-"
println "---------------------------------------------------------------"

def JENKINS = Jenkins.get()

println "--------------- Set INITIAL_PLUGINS_INSTALLING"
try {
    JENKINS.setInstallState(InstallState.INITIAL_PLUGINS_INSTALLING)
    println "setInstallState INITIAL_PLUGINS_INSTALLING done!"
}
catch (Exception e)  {
    println "---------------Error INITIAL_PLUGINS_INSTALLING --------------- : \n" + e.printStackTrace()
}
JENKINS.save()

println "--------------- Start install all plugins!"
UpdateSite updateSite = JENKINS.get().getUpdateCenter().getById('default')
PluginManager pluginManager = JENKINS.getPluginManager()

for (pluginId in pluginsList) {

    if (pluginManager.getPlugin(pluginId) == null ) {

        println "--------------- Install start install plugins : ${pluginId}"
        UpdateSite.Plugin plugin = updateSite.getPlugin(pluginId)
        Throwable error = plugin.deploy(true).get().getError()

        if(error != null) {
            println "--------------- ERROR installing ${pluginId} : ${error}"
        } else{
            println "--------------- Successfully installed plugin ${pluginId}"
        }
    }
    else{
        println "--------------- Plugin ${pluginId} already installed"
    }
}
JENKINS.save()

println "--------------- Set CREATE_ADMIN_USER"
try {
    JENKINS.setInstallState(InstallState.CREATE_ADMIN_USER)
    println "--------------- setInstallState CREATE_ADMIN_USER done!"
}
catch (Exception e)  {
    println "---------------Error CREATE_ADMIN_USER --------------- : \n" + e.printStackTrace()
}
JENKINS.save()

def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount(JENKINS_ADMIN_USERNAME, JENKINS_ADMIN_PASSWORD)
JENKINS.setSecurityRealm(hudsonRealm)
JENKINS.save()

println "--------------- Set INITIAL_SETUP_COMPLETED"
try {
    JENKINS.setInstallState(InstallState.INITIAL_SETUP_COMPLETED)
    println "--------------- setInstallState INITIAL_SETUP_COMPLETED done!"
}
catch (Exception e)  {
    println "--------------- Error INITIAL_SETUP_COMPLETED --------------- : \n" + e.printStackTrace()
}

JENKINS.setNumExecutors(10)
JENKINS.setLabelString("master jenkins-pipeline-library")

//Permet de visualiser les rapports HTML robot framework
System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "")

JENKINS.save()

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
	
	List<BranchSpec> branches = [
		new BranchSpec(PIPELINE_DEFAULT_BRANCH)
	]
	dslGeneratorJob.setScm(new GitSCM(GitSCM.createRepoList(PIPELINE_LIB_GIT_URL, ""), branches, false, null, null, null, null))

	ExecuteDslScripts executeDslScripts = new ExecuteDslScripts()
	executeDslScripts.setTargets("src/mtl/devops/dslGeneratorJobs.groovy")
	executeDslScripts.setRemovedJobAction(RemovedJobAction.DELETE)
	executeDslScripts.setRemovedViewAction(RemovedViewAction.DELETE)
	executeDslScripts.setRemovedConfigFilesAction(RemovedConfigFilesAction.DELETE)
	executeDslScripts.setAdditionalClasspath("src")
	dslGeneratorJob.getBuildersList().add(executeDslScripts)

	dslGeneratorJob.save()
	dslGeneratorJob.scheduleBuild(0)
}

def globalConfigFileStore = org.jenkinsci.plugins.configfiles.GlobalConfigFiles.get();

def stringWriter = new StringWriter()
def settingsXmlFile = new XmlSlurper().parse(new File(MAVEN_SETTINGS_ABSOLUTE_PATH))
new XmlNodePrinter(new PrintWriter(stringWriter)).print(settingsXmlFile)

def mavenSettingsConfig = new org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig("nexus-settings", "nexus-settings", "", stringWriter.toString(), null, null);
globalConfigFileStore.save(mavenSettingsConfig)

JENKINS.save()

