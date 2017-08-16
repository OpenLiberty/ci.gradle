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

import net.wasdev.wlp.gradle.plugins.utils.*;

class InstallAppsTask extends AbstractTask {

    protected ApplicationXmlDocument applicationXml = new ApplicationXmlDocument();

    @TaskAction
    void installApps() {

        boolean installDependencies = false
        boolean installProject = false

        switch (getInstallAppPackages()) {
            case "all":
                installDependencies = true
                installProject = true
                break;
            case "dependencies":
                installDependencies = true
                break
            case "project":
                installProject = true
                break
            default:
                return
        }

        if (installProject) {
            installProjectArchive()
        }
        /**if(installDependencies){
            installDependencies()
        }*/

        // create application configuration in configDropins if application is not configured
        if (applicationXml.hasChildElements()) {
            logger.warn("The application is not defined in the server configuration but the build file indicates it should be installed in the apps folder. Application configuration is being added to the target server configuration dropins folder by the plug-in.");
            applicationXml.writeApplicationXmlDocument(getServerDir(project));
        } else {
            if (ApplicationXmlDocument.getApplicationXmlFile(getServerDir(project)).exists()) {
                ApplicationXmlDocument.getApplicationXmlFile(getServerDir(project)).delete();
            }
        }
    }

    private void installProjectArchive() throws Exception {
        File archive = new File(archivePath())
        if(!archive.exists()) {
            throw new GradleException("The project archive was not found and cannot be installed.")
        }
        Files.copy(archive.toPath(), new File(getServerDir(project), "/" + project.liberty.installapps.appsDirectory + "/" + getArchiveName(archive.getName())).toPath(), StandardCopyOption.REPLACE_EXISTING)

        validateAppConfig(getArchiveName(archive.getName()), getBaseName(archive))
    }

    private void validateAppConfig(String fileName, String artifactId) throws Exception {
        String appsDir = project.liberty.installapps.appsDirectory

        if(appsDir.equalsIgnoreCase('apps') && !isAppConfiguredInSourceServerXml(fileName)){
            applicationXml.createApplicationElement(fileName, artifactId)
        }
        else if(appsDir.equalsIgnoreCase('dropins') && isAppConfiguredInSourceServerXml(fileName)){
            throw new GradleException("The application is configured in the server.xml and the plug-in is configured to install the application in the dropins folder. A configured application must be installed to the apps folder.")
        }
    }

    protected boolean isAppConfiguredInSourceServerXml(String fileName) {
        boolean configured = false;
        File serverConfigFile = new File(getServerDir(project), 'server.xml')
        if (serverConfigFile != null && serverConfigFile.exists()) {
            try {
                ServerConfigDocument scd = ServerConfigDocument.getInstance(serverConfigFile, project.liberty.configDirectory, project.liberty.bootstrapPropertiesFile, project.liberty.bootstrapProperties, project.liberty.serverEnv)
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

    private String getArchiveName(String archiveName){
        if(project.liberty.installapps.stripVersion){
            StringBuilder sbArchiveName = new StringBuilder().append("-").append(project.version)
            return archiveName.replaceAll(sbArchiveName.toString(),"")
        }
        return archiveName;
    }

    private String getInstallAppPackages() {
        if (project.plugins.hasPlugin("ear")) {
            project.liberty.installapps.installAppPackages = "project"
        }
        return project.liberty.installapps.installAppPackages
    }

    //Removes extension
    private String getBaseName(File file){
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
    //Loose Config Start
    private void installProject() throws Exception{
      if(isSupportedType(getPackagingType())){
        if(project.liberty.installapps.looseApplication){
          String looseConfigFileName = getLooseConfigFileName(project.liberty.applications)
          String application = looseConfigFileName.substring(0, looseConfigFileName.length()-4)
          File destDir = new File(getServerDir(project), "apps")
          File looseConfigFile = new File(destDir, looseConfigFileName)
          LooseConfigData config = new LooseConfigData()
          switch(getPackagingType()){
            case "war":
                  installLooseConfigWar(config)
                  //validateAppConfig(getArchiveName(archive.getName()), getBaseName(archive))
                  validateAppConfig(application, application.take(getArchiveName(application).lastIndexOf('.')))
                  logger.debug("Ask matt what to put here again")
                  /*logger.info(MessageFormat.format(("Installing application into the {0} folder."), looseConfigFileName));*/
                  deleteApplication(new File(getServerDir(project), "apps"), looseConfigFile)
                  deleteApplication(new File(getServerDir(project), "dropins"), looseConfigFile)
                  config.toXmlFile(looseConfigFile)
                  println("\n\n\n\n:::::::::::::::::::::::::::::::::;\n\n\n\n")
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
        else{
          installProjectArchive()
        }
      }
      else{
        throw new GradleException("Application is not supported")
      }
    }

    //Start of methods that need to be implemented
    // install war project artifact using loose application configuration file
    protected void installLooseConfigWar(LooseConfigData config) throws Exception {
        File dir = new File(project.liberty.outputDir)
        if (!dir.exists() && containsJavaSource()) {
          throw new GradleException("Ask Matt what to put here")
        }
        LooseWarApplication looseWar = new LooseWarApplication(project, config)
        looseWar.addSourceDir()
        looseWar.addOutputDir(looseWar.getDocumentRoot(), project, "/WEB-INF/classes");

        // retrieves dependent library jar files
        addWarEmbeddedLib(looseWar.getDocumentRoot(), looseWar);
    }

    private void addWarEmbeddedLib(Element parent, LooseApplication looseApp) throws Exception {
        DependencySet deps = project.configuration.dependencies
        /*for (dep in deps) {
            MavenProject dependProject = getMavenProject(dep.getGroupId(), dep.getArtifactId(),
                    dep.getVersion());
            if (dependProject.getBasedir() != null && dependProject.getBasedir().exists()) {
                Element archive = looseApp.addArchive(parent, "/WEB-INF/lib/" + dependProject.getBuild().getFinalName() + ".jar");
                looseApp.addOutputDir(archive, dependProject, "/");
                looseApp.addManifestFile(archive, dependProject, "maven-jar-plugin");
            } else {
                looseApp.getConfig().addFile(parent,
                        resolveArtifact(dependProject.getArtifact()).getFile().getAbsolutePath(),
                        "/WEB-INF/lib/" + resolveArtifact(dependProject.getArtifact()).getFile().getName());
            }
        }
    }*/
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
      File[] files = dir.listFiles()
      for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".java")) {
                return true;
            } else if (file.isDirectory()) {
                return containsJavaSource(file);
            }
        }
        return false;
    }



//End of methods that need to be implemented
    //Need to modify to emulate what it's doing in InstallAppsMojo
    private boolean isSupportedType(){
      switch (type) {
        case "ear":
        case "war":
        case "eba":
        case "esa":
            return true;
        default:
            return false;
        }
    }
    //According to installAppsMojoSupp, only worried about war??
    private String getLooseConfigFileName(){
      return getArchiveName(project.war.archiveName) + ".xml"
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
          // application can be installed with expanded format
          FileUtils.deleteDirectory(application);
      } else {
          application.delete();
      }
  }
}
