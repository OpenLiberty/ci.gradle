package net.wasdev.wlp.gradle.plugins.tasks

import groovy.transform.TupleConstructor
import net.wasdev.wlp.gradle.plugins.utils.LooseConfigData
import net.wasdev.wlp.gradle.plugins.utils.LooseWarApplication
import org.apache.commons.io.FilenameUtils
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.TaskAction
import org.gradle.plugins.ear.EarPlugin
import org.w3c.dom.Element

import java.nio.file.Paths
import java.text.MessageFormat
import java.util.regex.Matcher
import java.util.regex.Pattern

import static net.wasdev.wlp.gradle.plugins.Liberty.*

class InstallAppsLooseTask extends InstallAppsTask {

//  @Input
//  @Optional
  def deployDropinConfig() {
    resolveArtifact(LIBERTY_DEPLOY_CONFIGURATION)
  }

//  @Input
//  @Optional
  def deployAppConfig() {
    resolveArtifact(LIBERTY_DEPLOY_APP_CONFIGURATION)
  }

  @TaskAction
  void installApps() {
//    deployDropinConfig()
    for (Dependency d in deployAppConfig()) {
      println d
      ILooseInstaller installer
      if (d instanceof DefaultProjectDependency) {
        println d.dependencyProject.name
        installer = new InstallLooseByProject(project, getServerDir(project))
        installer.routeInstall(d.dependencyProject, appsDir())
      }
    }
  }

  def resolveArtifact(String name){
    def deployConf = project.configurations.findByName(name)
//    println deployConf
//    println deployConf.getAllDependencies()
    if (deployConf != null) {
//      deployConf.resolve()
      return deployConf.getAllDependencies()
    }
    return []
  }
}

class InstallLooseByProject extends LooseInstaller implements ILooseInstaller {
  InstallLooseByProject(Project project, File serverDir){
    super(project, serverDir)
  }

  void routeInstall(def proj, File destDir) throws Exception {

    InstallDTO installType = getPackagingType(proj)
    installLooseApplication(installType, destDir)
  }

//  String getLooseConfigFileName(def proj, InstallType installType){
//    Project intProj = proj as Project
//    Task task
//
//    switch (installType) {
//      case InstallType.WAR:
//        task = intProj.tasks.findByName(TASK_CORE_WAR)
//        break
//      case InstallType.EAR:
//        task = intProj.tasks.findByName(TASK_CORE_EAR)
//        break
//    }
//    assert task != null : "Could not find appropriate deployable task type"
//    return getArchiveName(task) + ".xml"
//  }
}

interface ILooseInstaller {
  void routeInstall(def proj, File appsDir) throws Exception
//  String getLooseConfigFileName(def source, InstallType installType)
}

abstract class LooseInstaller {

  Project project
  File serverDir
  LooseInstaller(Project project, File serverDir){
    this.project = project
    this.serverDir = serverDir
  }

  InstallDTO getPackagingType(Project proj) {
    if (proj.plugins.hasPlugin(WarPlugin)) {
      return  new InstallDTO(InstallType.WAR, proj.tasks.findByName(TASK_CORE_WAR), proj)
    } else if (proj.plugins.hasPlugin(EarPlugin)) {
      return new InstallDTO(InstallType.EAR, proj.tasks.findByName(TASK_CORE_EAR), proj)
    }
    return new InstallDTO(InstallType.NONE, null, proj)
  }

  String getArchiveName(Task task){
//    if (server.stripVersion){
//      return task.baseName + "." + task.extension
//    }
    return task.archiveName
  }

  void installLooseApplication(InstallDTO installDTO, File destDir) throws Exception {

    String looseConfigFileName = getLooseConfigFileName(installDTO.task)
    println looseConfigFileName
    String application = looseConfigFileName.substring(0, looseConfigFileName.length() - 4)
    File looseConfigFile = new File(destDir, looseConfigFileName)
    LooseConfigData config = new LooseConfigData()

    switch (installDTO.installType) {
      case InstallType.WAR:
//        logger.lifecycle(MessageFormat.format(("Installing application into the {0} folder."), looseConfigFile.getAbsolutePath()))
        installLooseConfigWar(config, installDTO)
//        deleteApplication(new File(getServerDir(project), "apps"), looseConfigFile)
//        deleteApplication(new File(getServerDir(project), "dropins"), looseConfigFile)
        config.toXmlFile(looseConfigFile)
        break
//      case InstallType.EAR:
//        break
//      default:
//        logger.info(MessageFormat.format(("Loose application configuration is not supported for packaging type {0}. The project artifact will be installed as an archive file."),
//            project.getPackaging()))
////            installProjectArchive(task, appsDir)
//        break
    }
  }

  private String getLooseConfigFileName(Task task){
    return getArchiveName(task) + ".xml"
  }

  protected void installLooseConfigWar(LooseConfigData config, InstallDTO installDTO) throws Exception {
        if (!serverDir.exists() && !installDTO.task.sourceSets.allJava.getSrcDirs().isEmpty()) {
      throw new GradleException(MessageFormat.format("Failed to install loose application from project {0}. The project has not been compiled.", project.name))
    }
    LooseWarApplication looseWar = new LooseWarApplication(installDTO.task, config)
    looseWar.addSourceDir()
    looseWar.addOutputDir(looseWar.getDocumentRoot() , installDTO.task, "/WEB-INF/classes/")

    //retrieves dependent library jar files
    addWarEmbeddedLib(looseWar.getDocumentRoot(), looseWar, installDTO.task)

    //add Manifest file
    File manifestFile = Paths.get(installDTO.parentProject.sourceSets.main.getOutput().getResourcesDir()
        .getParentFile().getAbsolutePath(), "/META-INF/MANIFEST.MF").toFile()

    looseWar.addManifestFile(manifestFile, "gradle-war-plugin")
  }

  private void addWarEmbeddedLib(Element parent, LooseWarApplication looseApp, Task task) throws Exception {
    ArrayList<File> deps = new ArrayList<File>()
    task.classpath.each {deps.add(it)}
    //Removes WEB-INF/lib/main directory since it is not rquired in the xml
    if(deps != null && !deps.isEmpty()){
      deps.remove(0)
    }
    File parentProjectDir = new File(task.getProject().getRootProject().rootDir.getAbsolutePath())
    for (File dep: deps) {
      String dependentProjectName = "project ':${getProjectPath(parentProjectDir, dep)}'"
      Project siblingProject = project.getRootProject().findProject(dependentProjectName)
      boolean isCurrentProject = ((task.getProject().toString()).equals(dependentProjectName))
      if (!isCurrentProject && siblingProject != null){
        Element archive = looseApp.addArchive(parent, "/WEB-INF/lib/"+ dep.getName());
        looseApp.addOutputDirectory(archive, siblingProject, "/")
        Task resourceTask = siblingProject.getTasks().findByPath(":"+dependentProjectName+":processResources")
        if (resourceTask.getDestinationDir() != null){
          looseApp.addOutputDir(archive, resourceTask.getDestinationDir(), "/")
        }
        looseApp.addManifestFile(archive, siblingProject, "gradle-jar-plugin");
      } else if(FilenameUtils.getExtension(dep.getAbsolutePath()).equalsIgnoreCase("jar")){
        looseApp.getConfig().addFile(parent, dep.getAbsolutePath() , "/WEB-INF/lib/" + dep.getName())
      } else {
        looseApp.addOutputDir(looseApp.getDocumentRoot(), dep.getAbsolutePath() , "/WEB-INF/classes/")
      }
    }
  }

  private String getProjectPath(File parentProjectDir, File dep){
    String dependencyPathPortion = dep.getAbsolutePath().replace(parentProjectDir.getAbsolutePath()+"/","")
    String projectPath = dep.getAbsolutePath().replace(dependencyPathPortion,"")
    Pattern pattern = Pattern.compile("/build/.*")
    Matcher matcher = pattern.matcher(dependencyPathPortion)
    projectPath = matcher.replaceAll("")
    return projectPath
  }

}

enum InstallType {
  EAR, WAR, NONE
}

@TupleConstructor
class InstallDTO {
  InstallType installType
  Task task
  Project parentProject
}
