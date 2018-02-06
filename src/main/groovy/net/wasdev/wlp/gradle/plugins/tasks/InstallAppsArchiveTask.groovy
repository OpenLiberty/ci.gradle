package net.wasdev.wlp.gradle.plugins.tasks

import net.wasdev.wlp.gradle.plugins.ILibertyDefinitions
import net.wasdev.wlp.gradle.plugins.loose.ApplicationInstaller
import net.wasdev.wlp.gradle.plugins.loose.ArchiveInstallerByLibertyBlock
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

import static net.wasdev.wlp.gradle.plugins.Liberty.LIBERTY_DEPLOY_CONFIGURATION
import static net.wasdev.wlp.gradle.plugins.Liberty.LIBERTY_DEPLOY_APP_CONFIGURATION

class InstallAppsArchiveTask extends InstallAppsTask implements ILibertyDefinitions {

  @Input
  @Optional
  def deployDropinConfig() {
    resolveArtifact(LIBERTY_DEPLOY_CONFIGURATION)
  }

  @Input
  @Optional
  def deployAppConfig() {
    resolveArtifact(LIBERTY_DEPLOY_APP_CONFIGURATION)
  }

  List<ResolvedArtifact> resolveArtifact(String name){
    def deployConf = project.configurations.findByName(name)
    if (deployConf != null) {

      //def deployArtifacts = deployConf.incoming.resolutionResult.allDependencies as List
//      return deployConf.resolvedConfiguration.resolvedArtifacts as List
      return deployConf.resolve() as List<ResolvedArtifact>
    }
    return null
  }

  @TaskAction
  void installApps() {
    // install archive by Liberty Block
    ApplicationInstaller configInstaller = new ArchiveInstallerByLibertyBlock(project, appsDir(), getServerDir(project))
    configInstaller.searchLocalAppsWar()

    println(server.apps)
    configInstaller.installServerAppsConfig()

    // install archive by configuration
    deployArtifact(deployDropinConfig(), dropinsDir())
    deployArtifact(deployAppConfig(), appsDir())
  }

  def deployArtifact(def archiveFrom, def archiveTo) {
    for (ResolvedArtifact archive in archiveFrom){
      project.copy {
        from archive.file
        into archiveTo
      }
    }
  }

}
