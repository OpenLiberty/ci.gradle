package net.wasdev.wlp.gradle.plugins.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class CreateServerXmlTask extends AbstractServerTask {

  String configFilename = "server.xml"

//  @OutputFile
//  File getServerXmlOutFile() {
//    new File(getServerDir(project), configFilename)
//  }

  @Input
  @Optional
  File getServerXmlFile() {
    if (!server.configFile.exists()) {
      logger.warn("The server configFile was configured but was not found at: ${server.configFile}")
    }
    return server.configFile
  }

  @TaskAction
  void createServerConfig() {
    if (serverXmlFile.exists()) {
      project.copy {
        from serverXmlFile
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
