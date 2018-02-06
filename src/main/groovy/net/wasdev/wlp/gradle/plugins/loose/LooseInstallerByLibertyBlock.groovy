package net.wasdev.wlp.gradle.plugins.loose

import net.wasdev.wlp.gradle.plugins.extensions.LibertyExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task

import java.text.MessageFormat

/**
 * This installer is built to handle installing loose applications via the project
 * config method using gradle configurations
 *
 */
class LooseInstallerByLibertyBlock implements ApplicationInstaller {

  LooseInstallerByLibertyBlock(Project project, File appsDir, File serverDir){
    this.project = project
    this.appsDir = appsDir
    this.serverDir = serverDir

    libertyExt = project.extensions.findByType(LibertyExtension)
    server = libertyExt.getServer()

  }

  void installArchives() {
    searchLocalAppsWar()
    installServerAppsConfig()

//    if (server.dropins != null && !server.dropins.isEmpty()) {
//      Tuple dropinsLists = splitAppList(server.dropins)
//      installMultipleApps(dropinsLists[0], 'dropins')
//      installFileList(dropinsLists[1], 'dropins')
//    }
  }
}
