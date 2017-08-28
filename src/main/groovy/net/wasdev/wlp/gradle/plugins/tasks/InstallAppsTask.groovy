/**
 * (C) Copyright IBM Corporation 2014, 2017.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.wasdev.wlp.gradle.plugins.tasks

import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.gradle.api.GradleException
import groovy.util.XmlParser
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.w3c.dom.Element;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.gradle.api.Task
import org.gradle.api.tasks.bundling.War

import net.wasdev.wlp.gradle.plugins.utils.*;

class InstallAppsTask extends AbstractServerTask {

    protected ApplicationXmlDocument applicationXml = new ApplicationXmlDocument();

    @TaskAction
    void installApps() {

        boolean installDependencies = false
        boolean installApp = false

        switch (getInstallAppPackages()) {
            case "all":
                installDependencies = true
                installApp = true
                break;
            case "dependencies":
                installDependencies = true
                break
            case "project":
                installApp = true
                break
            default:
                return
        }

        if (installProject) {
            if ((server.apps == null || server.apps.isEmpty()) && (server.dropins == null || server.dropins.isEmpty())) {
                if (project.plugins.hasPlugin('war')) {
                    server.apps = [project.war]
                }
            }
            if (server.apps != null && !server.apps.isEmpty()) {
                installMultipleApps(server.apps, 'apps')
            }
            if (server.dropins != null && !server.dropins.isEmpty()) {
                installMultipleApps(server.dropins, 'dropins')
            }
        }
        //TODO
        /**if(installDependencies){
            installDependencies()
        }*/

        if (applicationXml.hasChildElements()) {
            logger.warn("The application is not defined in the server configuration but the build file indicates it should be installed in the apps folder. Application configuration is being added to the target server configuration dropins folder by the plug-in.");
            applicationXml.writeApplicationXmlDocument(getServerDir(project));
        } else {
            if (ApplicationXmlDocument.getApplicationXmlFile(getServerDir(project)).exists()) {
                ApplicationXmlDocument.getApplicationXmlFile(getServerDir(project)).delete();
            }
        }
    }

    private void installMultipleApps(List<Task> applications, String appsDir) {
        applications.each{ Task task ->
            Files.copy(task.archivePath.toPath(), new File(getServerDir(project), "/" + appsDir + "/" + getArchiveName(task.archiveName)).toPath(), StandardCopyOption.REPLACE_EXISTING)
            validateAppConfig(getArchiveName(task.archiveName), task.baseName, appsDir)
        }
    }

    protected void validateAppConfig(String fileName, String artifactId) throws Exception {
        validateAppConfig(fileName, artifactId, server.installapps.appsDirectory)
    }

    protected void validateAppConfig(String fileName, String artifactId, String dir) throws Exception {
        String appsDir = dir

        if (appsDir.equalsIgnoreCase('apps') && !isAppConfiguredInSourceServerXml(fileName)) {
            applicationXml.createApplicationElement(fileName, artifactId)
        }
        else if (appsDir.equalsIgnoreCase('dropins') && isAppConfiguredInSourceServerXml(fileName)) {
            throw new GradleException("The application, " + artifactId + ", is configured in the server.xml and the plug-in is configured to install the application in the dropins folder. A configured application must be installed to the apps folder.")
        }
    }

    protected boolean isAppConfiguredInSourceServerXml(String fileName) {
        boolean configured = false;
        File serverConfigFile = new File(getServerDir(project), 'server.xml')
        if (serverConfigFile != null && serverConfigFile.exists()) {
            try {
                ServerConfigDocument scd = ServerConfigDocument.getInstance(serverConfigFile, server.configDirectory, server.bootstrapPropertiesFile, server.bootstrapProperties, server.serverEnv)
                if (scd != null && scd.getLocations().contains(fileName)) {
                    logger.debug("Application configuration is found in server.xml : " + fileName)
                    configured = true
                }
            }
            catch (Exception e) {
                logger.warn(e.getLocalizedMessage())
                logger.debug(e)
            }
        }
        return configured
    }

    protected String getArchiveName(String archiveName){
        if (server.installapps.stripVersion){
            StringBuilder sbArchiveName = new StringBuilder().append("-").append(project.version)
            return archiveName.replaceAll(sbArchiveName.toString(),"")
        }
        return archiveName;
    }

    private String getInstallAppPackages() {
        if (project.plugins.hasPlugin("ear")) {
            server.installapps.installAppPackages = "project"
        }
        return server.installapps.installAppPackages
    }

    //Removes extension
    private String getBaseName(File file) {
        return file.name.take(getArchiveName(file.name).lastIndexOf('.'))
    }

    private String archivePath() throws Exception {
        if (project.plugins.hasPlugin("ear")) {
            return project.ear.archivePath
        }
        else if (project.plugins.hasPlugin("war")) {
            return project.war.archivePath
        }
        else if (project.plugins.hasPlugin("java")){
            return project.jar.archivePath
        }
        else {
            throw new GradleException("Archive path not found. Supported formats are jar, war, and ear.")
        }
    }

    private void installProject() throws Exception {
      if(isSupportedType()) {
        if(server.installapps.looseApplication){
          installLooseApplication()
        } else {
          installProjectArchive()
        }
      } else {
        throw new GradleException("Application is not supported")
      }
    }

    private void installLooseApplication() throws Exception {
      String looseConfigFileName = getLooseConfigFileName()
      String application = looseConfigFileName.substring(0, looseConfigFileName.length()-4)
      File destDir = new File(getServerDir(project), "apps")
      File looseConfigFile = new File(destDir, looseConfigFileName)
      LooseConfigData config = new LooseConfigData()
      switch(getPackagingType()){
        case "war":
            validateAppConfig(application, application.take(checkForStripVersion(application).lastIndexOf('.')))
            logger.debug("Installing application into the {0} folder.", looseConfigFile.getAbsolutePath())
            installLooseConfigWar(config)
            deleteApplication(new File(getServerDir(project), "apps"), looseConfigFile)
            deleteApplication(new File(getServerDir(project), "dropins"), looseConfigFile)
            config.toXmlFile(looseConfigFile)
            break
        case "ear":
            break
        default:
            logger.info(MessageFormat.format(("Loose application configuration is not supported for packaging type {0}. The project artifact will be installed as is."),
                    project.getPackaging()));
            installProjectArchive()
            break
        }
    }

    protected void installLooseConfigWar(LooseConfigData config) throws Exception {
        File dir = getServerDir(project)
        if (!dir.exists() && containsJavaSource()) {
          throw new GradleException("Failed to install loose application from project {0}. The project has not been compiled.", project.name)
        }
        LooseWarApplication looseWar = new LooseWarApplication(project, config)
        looseWar.addSourceDir()
        looseWar.addOutputDir(looseWar.getDocumentRoot(), project, "/WEB-INF/classes");

        //retrieves dependent library jar files
        addWarEmbeddedLib(looseWar.getDocumentRoot(), looseWar);

        //add Manifest file
        File manifestFile = new File(project.sourceSets.main.getOutput().getResourcesDir().getParentFile().getAbsolutePath() + "/META-INF/MANIFEST.MF")
        looseWar.addManifestFile(manifestFile, "gradle-war-plugin")
    }

    private void addWarEmbeddedLib(Element parent, LooseApplication looseApp) throws Exception {
      ArrayList<File> deps = new ArrayList<File>();
      project.configurations.runtime.each { deps.add(it)}
      project.configurations.providedRuntime.each { deps.remove(it) }
      File parentProjectDir = new File(project.getRootProject().rootDir.getAbsolutePath())
      for (File dep: deps) {
        String projectPath = getProjectPath(parentProjectDir, dep)
        if(!projectPath.isEmpty() && project.getRootProject().findProject(projectPath) != null){
            Element archive = looseApp.addArchive(parent, "/WEB-INF/lib/"+ dep.getName());
            looseApp.addOutputDirectory(archive, project.getRootProject().findProject(projectPath), "/");
            looseApp.addManifestFile(archive, project.getRootProject().findProject(projectPath), "gradle-jar-plugin");
        } else{
            looseApp.getConfig().addFile(parent, dep.getAbsolutePath() , "/WEB-INF/lib/" + dep.getName());
        }
      }
    }

    private String getProjectPath(File parentProjectDir, File dep){
      String projectJunk = dep.getAbsolutePath().replace(parentProjectDir.getAbsolutePath()+"/","")
      String projectPath = dep.getAbsolutePath().replace(projectJunk,"")
      Pattern pattern = Pattern.compile("/build/.*")
      Matcher matcher = pattern.matcher(projectJunk)
      projectPath = matcher.replaceAll("")
      return projectPath;
    }

    private boolean containsJavaSource(){
      Set<File> srcDirs = project.sourceSets.allJava.getSrcDirs();
      for(srcDir in srcDirs){
        File javaSourceDir = new File (srcDir)
        if(javaSourceDir.exists() && javaSourceDir.isDirectory() && containsJavaSource(javaSourceDir)){
          return true;
        }
      }
      return false;
    }

    private boolean containsJavaSource(File f){
      File[] files = f.listFiles()
      for (file in Files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".java")) {
                return true;
            } else if (file.isDirectory()) {
                return containsJavaSource(file);
            }
        }
        return false;
    }

    private boolean isSupportedType(){
      switch (getPackagingType()) {
        case "ear":
        case "war":
        case "eba":
        case "esa":
            return true;
        default:
            return false;
        }
    }
    private String getLooseConfigFileName(){
      return checkForStripVersion(project.war.archiveName) + ".xml"
    }

    private String getPackagingType() throws Exception{
      if (project.plugins.hasPlugin("ear")) {
          return "ear"
      }
      else if (project.plugins.hasPlugin("war")) {
          return "war"
      }
      else if (project.plugins.hasPlugin("java")){
          return "java"
      }
      else {
          throw new GradleException("Archive path not found. Supported formats are jar, war, and ear.")
      }
  }

  protected void deleteApplication(File parent, File artifactFile) throws IOException {
      deleteApplication(parent, artifactFile.getName());
      if (artifactFile.getName().endsWith(".xml")) {
          deleteApplication(parent, artifactFile.getName().substring(0, artifactFile.getName().length() - 4));
      } else {
          deleteApplication(parent, artifactFile.getName() + ".xml");
      }
  }

  protected void deleteApplication(File parent, String filename) throws IOException {
      File application = new File(parent, filename);
      if (application.isDirectory()) {
          FileUtils.deleteDirectory(application);
      } else {
          application.delete();
      }
  }
}
