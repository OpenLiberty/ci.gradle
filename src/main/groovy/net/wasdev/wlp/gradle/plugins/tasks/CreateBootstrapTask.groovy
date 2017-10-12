package net.wasdev.wlp.gradle.plugins.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class CreateBootstrapTask extends AbstractServerTask {

  @OutputFile
  File getBootStrapProperties() {
    new File(getServerDir(project), "bootstrap.properties")
  }

  @Input
  @Optional
  Map<String, Object> getBootStrapData() {
    if (server.bootstrapProperties) {
      return server.bootstrapProperties
    } else if (server.bootstrapPropertiesFile.exists()) {
      return loadPropertiesToMap(server.bootstrapPropertiesFile)
    } else {
      logger.warn("Default bootstrap.properties file not found at: ${server.bootstrapPropertiesFile}")
    }

    return [:]
  }

  @TaskAction
  void createBootstrap() {
    writeBootstrapProperties(bootStrapProperties, bootStrapData)
  }

  private void writeBootstrapProperties(File file, Map<String, Object> properties) throws IOException {
    makeParentDirectory(file)

    if (file.exists()) {
      file.delete()
    }

    file.withWriter('UTF-8') { writer ->
      writer.write(HEADER)
      properties.each{ k, v ->
        String val = v.toString().replace("\\", "/")
        writer.write("${k}=${val}\n" )
      }
    }
  }
}
