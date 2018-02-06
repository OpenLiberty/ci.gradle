package net.wasdev.wlp.gradle.plugins.tasks

import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class CreateServerEnvTask extends AbstractServerTask {

  String configFilename = "server.env"

  @OutputFile
  File getServerEnvOutFile() {
    new File(getServerDir(project), configFilename)
  }

  @InputFile
  @Optional
  File getServerEnvFile() {
    return server.serverEnv
  }

  @TaskAction
  void createServerConfig() {
    project.copy {
      from serverEnvFile
      into getServerDir(project)
      rename { String fileName ->
        if (fileName != configFilename) {
          fileName.replace(fileName, configFilename)
        }
      }
    }
  }
}
