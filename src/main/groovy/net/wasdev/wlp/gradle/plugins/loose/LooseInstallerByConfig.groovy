package net.wasdev.wlp.gradle.plugins.loose

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency

/**
 * This installer is built to handle installing loose applications via the project
 * config method using gradle configurations
 *
 */
class LooseInstallerByConfig implements LooseInstaller{

  DependencySet depSet

  LooseInstallerByConfig(Project project, DependencySet depSet, File appsDir, File serverDir){
    this.depSet = depSet
    this.project = project
    this.appsDir = appsDir
    this.serverDir = serverDir
  }

  void installArchives() {
    for (Dependency d in depSet) {

      if (d instanceof DefaultProjectDependency) {
        project.logger.debug(d.dependencyProject.name)

        InstallDTO installType = getPackagingType(d.dependencyProject)
        installLooseApplication(installType, appsDir)
      }
    }
  }
}
