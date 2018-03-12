package net.wasdev.wlp.gradle.plugins.loose

import net.wasdev.wlp.gradle.plugins.extensions.LibertyExtension
import net.wasdev.wlp.gradle.plugins.extensions.ServerExtension
import net.wasdev.wlp.gradle.plugins.utils.LooseConfigData
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.bundling.War
import org.gradle.plugins.ear.Ear
import org.gradle.plugins.ear.EarPlugin

import java.nio.file.Paths
import java.text.MessageFormat

import static net.wasdev.wlp.gradle.plugins.Liberty.TASK_CORE_EAR
import static net.wasdev.wlp.gradle.plugins.Liberty.TASK_CORE_WAR

trait ApplicationInstaller {
  abstract void installArchives()

  Project project
  File appsDir
  File serverDir

  LibertyExtension libertyExt
  ServerExtension server

  /**
   * Called if no deployment is configured.  This searches the apps and dropins config and if not found, it will
   * search the local project for a war plugin.  if it finds one, it will deploy that
   * @return
   */
  def searchLocalAppsWar() {
    if ((server.apps?.size() > 0) && (server.dropins?.size() > 0)) {
      if (project.plugins.hasPlugin('war')) {
        if (server.apps == null) {
          server.apps = []
        }
        server.apps << project.war
      }
    }
  }

  def installServerDropinsConfig(){
    if (server.dropins?.size() > 0) {
      Tuple appsLists = splitAppList(server.dropins)

      installMultipleApps(appsLists[0] as List<Task>)
      installFileList(appsLists[1] as List<File>)
    }
  }

  /**
   * Attempt to install applications based on the config
   * @return
   */
  def installServerAppsConfig(){
    if (server.apps?.size() > 0) {
      Tuple appsLists = splitAppList(server.apps)

      installMultipleApps(appsLists[0] as List<Task>)
      installFileList(appsLists[1] as List<File>)
    }
  }

  void installMultipleApps(List<Task> applications) {
    applications.each { Task task ->
      InstallDTO dto = getPackagingType(task, project)

      println(dto.toString())

      if (dto.installType != InstallType.NONE) {
        if(server.looseApplication) {
          installLooseApplication(dto)
        } else {
          installProjectArchive(dto.task, appsDir)
        }
      } else {
        throw new GradleException(MessageFormat.format("Application {0} is not supported",
            task.archiveName.toString()))
      }
    }
  }

  private void installProjectArchive(Task task, File appsDir) {

    project.copy {
      from task
      into appsDir
      rename { String fileName ->
        getArchiveName(task)
      }
    }

//    validateAppConfig(getArchiveName(task), task.baseName, appsDir)
  }


  void installLooseApplication(InstallDTO installDTO) throws Exception {

    println("appsDir: ${appsDir}")
    println("serverDir: ${serverDir}")

    String looseConfigFileName = getLooseConfigFileName(project, installDTO)
    println("Loose file name: ${looseConfigFileName}")

    String application = looseConfigFileName.substring(0, looseConfigFileName.length() - 4)
    File looseConfigFile = new File(appsDir, looseConfigFileName)
    LooseConfigData config = new LooseConfigData()

    switch (installDTO.installType) {
      case InstallType.WAR:
        project.logger.lifecycle(MessageFormat.format(("Installing application into the {0} folder."), looseConfigFile.getAbsolutePath()))

        LooseWarApplication warApplication = new LooseWarApplication(installDTO, config, appsDir)
        warApplication.installLooseConfigWar(config, installDTO)
        config.toXmlFile(looseConfigFile)

        break
      case InstallType.EAR:
//        validateAppConfig(application, task.baseName, appsDir)
        project.logger.info(MessageFormat.format(("Installing application into the {0} folder."), looseConfigFile.getAbsolutePath()))
//        installLooseConfigEar(config, installDTO)
        config.toXmlFile(looseConfigFile)
        break

    }
  }

  InstallDTO getPackagingType(Project proj) {
    if (proj.plugins.hasPlugin(WarPlugin)) {
      return new InstallDTO(InstallType.WAR, proj.tasks.findByName(TASK_CORE_WAR), proj)
    } else if (proj.plugins.hasPlugin(EarPlugin)) {
      return new InstallDTO(InstallType.EAR, proj.tasks.findByName(TASK_CORE_EAR), proj)
    }
    return new InstallDTO(InstallType.NONE, null, proj)
  }

  InstallDTO getPackagingType(Task task, Project project) {
    if (task instanceof War) {
      return new InstallDTO(InstallType.WAR, task, project)
    } else if (task instanceof Ear) {
      return new InstallDTO(InstallType.EAR, task, project)
    }
    return new InstallDTO(InstallType.NONE, null, project)
  }

  String getLooseConfigFileName(Project proj, InstallDTO installDTO){
    Project intProj = proj as Project
    Task task

    switch (installDTO.installType) {
      case InstallType.WAR:
        task = installDTO.task
        break
      case InstallType.EAR:
        task = installDTO.task
        break
    }
    assert task != null : "Could not find appropriate deployable task type"
    return getArchiveName(task) + ".xml"
  }

  String getArchiveName(Task task) {
    println("Task ${task.name} Basename: ${task.baseName}: archiveName: ${task.archiveName}")

    if (server.stripVersion){
      return task.baseName + "." + task.extension
    }

    return task.archiveName
  }

  void installFromFile(File file) {
    project.copy {
      from file
      into appsDir
    }
  }

  void installFileList(List<File> appFiles) {
    appFiles.each { File appFile ->
      installFromFile(appFile)
    }
  }

  Tuple splitAppList(List<Object> allApps) {
    List<File> appFiles = new ArrayList<File>()
    List<Task> appTasks = new ArrayList<Task>()

    allApps.each { Object appObj ->
      if (appObj instanceof Task) {
        appTasks.add((Task) appObj)
      } else if (appObj instanceof File) {
        appFiles.add((File) appObj)
      } else {
        project.logger.warn('Application ' + appObj.getClass.name + ' is expressed as ' + appObj.toString() + ' which is not a supported input type. Define applications using Task or File objects.')
      }
    }

    return new Tuple(appTasks, appFiles)
  }
}
