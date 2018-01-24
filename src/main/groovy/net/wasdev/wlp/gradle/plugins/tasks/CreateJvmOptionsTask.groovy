package net.wasdev.wlp.gradle.plugins.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Optional

class CreateJvmOptionsTask extends AbstractServerTask {

  String configFilename = "jvm.options"

  @OutputFile
  File getJvmOptionsOutFile() {
    new File(getServerDir(project), configFilename)
  }

  @InputFile
  @Optional
  File getJvmOptionsFile() {
    if (!server.jvmOptionsFile.exists()) {
      return null
    }
    return server.jvmOptionsFile
  }

  @Input
  @Optional
  List<String> getJvmOptionsData() {
    return server.jvmOptions
  }

  @TaskAction
  void createJvmOptions() {
    if (jvmOptionsData) {
      logger.info("Using the jvm.options in the build file")
      writeJvmOptions(getJvmOptionsOutFile(), jvmOptionsData)
    } else if (jvmOptionsFile != null) {
      logger.info("Using the jvm.options at: ${jvmOptionsFile}")
      project.copy {
        from jvmOptionsFile
        into getServerDir(project)
        rename { String fileName ->
          if (fileName != configFilename) {
            fileName.replace(fileName, configFilename)
          }
        }
      }
    } else {
      logger.info("No jvm.options configured")
    }
  }


  void writeJvmOptions(File file, List<String> options) throws IOException {
    makeParentDirectory(file)

    if (file.exists()) {
      file.delete()
    }

    file.withWriter('UTF-8') { writer ->
      writer.write(HEADER)
      for (String option : options) {
        writer.println(option)
      }
    }
  }
}
