package net.wasdev.wlp.gradle.plugins.loose

import net.wasdev.wlp.gradle.plugins.extensions.LibertyExtension
import org.gradle.api.Project

class ArchiveInstallerByLibertyBlock implements ApplicationInstaller {

  ArchiveInstallerByLibertyBlock(Project project, File appsDir, File serverDir){
    this.project = project
    this.appsDir = appsDir
    this.serverDir = serverDir

    libertyExt = project.extensions.findByType(LibertyExtension)
    server = libertyExt.getServer()

  }

  void installArchives() {}
}
