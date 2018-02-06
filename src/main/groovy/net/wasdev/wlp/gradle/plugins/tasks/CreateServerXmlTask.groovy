package net.wasdev.wlp.gradle.plugins.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class CreateServerXmlTask extends AbstractServerTask {

  String configFilename = "server.xml"

  @Input
  @Optional
  File getServerXmlFile() {
    return server.configFile
  }

  @TaskAction
  void createServerConfig() {
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
