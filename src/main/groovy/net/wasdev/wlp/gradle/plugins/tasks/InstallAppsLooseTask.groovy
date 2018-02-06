package net.wasdev.wlp.gradle.plugins.tasks


import net.wasdev.wlp.gradle.plugins.loose.LooseInstaller
import net.wasdev.wlp.gradle.plugins.loose.LooseInstallerByConfig
import net.wasdev.wlp.gradle.plugins.loose.LooseInstallerByLibertyBlock

import org.gradle.api.artifacts.DependencySet
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.file.Paths

import static net.wasdev.wlp.gradle.plugins.Liberty.*

class InstallAppsLooseTask extends InstallAppsTask {

  @OutputDirectory
  File appsDir() {
    Paths.get(getServerDir(project).absolutePath, DEPLOY_FOLDER_APPS).toFile()
  }

  @OutputDirectory
  File dropinsDir() {
    Paths.get(getServerDir(project).absolutePath, DEPLOY_FOLDER_DROPINS).toFile()
  }

  def deployDropinConfig() {
    resolveArtifact(LIBERTY_DEPLOY_CONFIGURATION)
  }

  DependencySet deployAppConfig() {
    resolveArtifact(LIBERTY_DEPLOY_APP_CONFIGURATION)
  }

  @TaskAction
  void installApps() {
    DependencySet depSet = deployAppConfig()

    if (depSet?.size() > 0){
      LooseInstaller configInstaller = new LooseInstallerByConfig(project, depSet, appsDir(), getServerDir(project))
      configInstaller.installArchives()
    }

    // install any apps that are defined in the liberty config block
    if (server.apps?.size() > 0){
      LooseInstaller configInstaller = new LooseInstallerByLibertyBlock(project, appsDir(), getServerDir(project))
      configInstaller.installArchives()
    }
  }

  def resolveArtifact(String name){
    def deployConf = project.configurations.findByName(name)
    if (deployConf != null) {
      return deployConf.getAllDependencies()
    }
    return []
  }

}
