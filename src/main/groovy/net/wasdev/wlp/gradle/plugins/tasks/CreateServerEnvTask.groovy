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
    if (!server.serverEnv.exists()) {
      logger.warn("The jvm.options was configured but was not found at: ${server.serverEnv}")
    }
    return server.serverEnv
  }

  @TaskAction
  void createServerConfig() {
    if (serverEnvFile.exists()) {
      project.copy {
        from serverEnvFile.parent
        into getServerDir(project)
        rename { String fileName ->
          if (fileName != configFilename) {
            fileName.replace(fileName, configFilename)
          }
        }
      }
    }
  }
}
