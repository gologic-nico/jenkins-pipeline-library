#!groovy

def projectsJson = this.readFileFromWorkspace("ressources/projects.json")
def root = new groovy.json.JsonSlurperClassic().parseText(projectsJson)

//Create MultiBranch pipeline
for (projet in root.projets) {

	def multibranchPipelineJob = multibranchPipelineJob(projet.name) {
		branchSources {
			git { remote(projet.gitUrl) }
		}
		orphanedItemStrategy {
			discardOldItems { numToKeep(20) }
		}
		triggers { 
			periodic(1) 
		}
	}
}