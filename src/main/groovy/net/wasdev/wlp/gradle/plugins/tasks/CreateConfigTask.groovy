package net.wasdev.wlp.gradle.plugins.tasks

import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class CreateConfigTask extends AbstractServerTask {

  //@OutputDirectory
  File getServerOutDir() {
    getServerDir(project)
  }

  @TaskAction
  void createServerConfig() {
    project.copy {
      from project.sourceSets.libertyBase.allLibertyBase
      into getServerOutDir()
    }
  }
}
