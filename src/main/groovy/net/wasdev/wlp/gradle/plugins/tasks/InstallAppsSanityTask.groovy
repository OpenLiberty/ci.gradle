package net.wasdev.wlp.gradle.plugins.tasks

import net.wasdev.wlp.gradle.plugins.ILibertyDefinitions
import org.gradle.api.tasks.TaskAction

class InstallAppsSanityTask extends InstallAppsTask implements ILibertyDefinitions {

  final String ERROR_IF_FILE_IN_BOTH = """The same file appears in both apps and dropins folders.  
    | Recommend you do a clean and start over""".stripMargin().stripIndent()

  @TaskAction
  void sanityCheck() {
    compareAppDropinFolders()
  }

  def compareAppDropinFolders() {
    List<String> collection1 =  filterBaseNames(appsDir().listFiles().toList())
    List<String> collection2 =  filterBaseNames(dropinsDir().listFiles().toList())

    def commons = collection1.intersect(collection2 as Iterable<String>)
    assert [] == commons : ERROR_IF_FILE_IN_BOTH
  }

  static List<String> filterBaseNames(List<File> files) {
    List<String> ret = []

    files.each { file->
      ret << file.getName().split("\\.")[0]
    }

    return ret
  }
}
