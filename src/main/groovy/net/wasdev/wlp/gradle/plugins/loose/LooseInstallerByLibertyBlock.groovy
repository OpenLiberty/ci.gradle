package net.wasdev.wlp.gradle.plugins.loose

import net.wasdev.wlp.gradle.plugins.extensions.LibertyExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task

import java.text.MessageFormat

import static net.wasdev.wlp.gradle.plugins.Liberty.TASK_CORE_EAR

/**
 * This installer is built to handle installing loose applications via the project
 * config method using gradle configurations
 *
 */
class LooseInstallerByLibertyBlock implements LooseInstaller {

  LooseInstallerByLibertyBlock(Project project, File appsDir, File serverDir){
    this.project = project
    this.appsDir = appsDir
    this.serverDir = serverDir

    libertyExt = project.extensions.findByType(LibertyExtension)
    server = libertyExt.getServer()

  }

  void installArchives() {
    if ((server.apps == null || server.apps.isEmpty()) && (server.dropins == null || server.dropins.isEmpty())) {
      if (project.plugins.hasPlugin('war')) {
        server.apps = [project.war]
      }
    }
    if (server.apps != null && !server.apps.isEmpty()) {
      Tuple appsLists = splitAppList(server.apps)

      installMultipleApps(appsLists[0] as List<Task>)
      installFileList(appsLists[1] as List<File>)
    }
//    if (server.dropins != null && !server.dropins.isEmpty()) {
//      Tuple dropinsLists = splitAppList(server.dropins)
//      installMultipleApps(dropinsLists[0], 'dropins')
//      installFileList(dropinsLists[1], 'dropins')
//    }
  }

  void installMultipleApps(List<Task> applications) {
    applications.each { Task task ->
      InstallDTO dto = getPackagingType(task, project)
      if (dto.installType != InstallType.NONE) {
        installLooseApplication(dto)
      } else {
        throw new GradleException(MessageFormat.format("Application {0} is not supported",
            task.archiveName.toString()))
      }
    }
  }
}
