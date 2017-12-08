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
      logger.warn("The jvm.options was configured but was not found at: ${server.jvmOptionsFile}")
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
      writeJvmOptions(getJvmOptionsOutFile(), jvmOptionsData)
    } else if (jvmOptionsFile != null) {
      project.copy {
        from jvmOptionsFile
        into getServerDir(project)
        rename { String fileName ->
          if (fileName != configFilename) {
            fileName.replace(fileName, configFilename)
          }
        }
      }
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
