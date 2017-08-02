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
        else if(installDependencies){
            installDependencies()
        }
        
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
        if (project.liberty.configFile != null && project.liberty.configFile.exists()) {
            try {
                ServerConfigDocument scd = ServerConfigDocument.getInstance(project.liberty.configFile, project.liberty.configDirectory, project.liberty.bootstrapPropertiesFile, project.liberty.bootstrapProperties, project.liberty.serverEnv)
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
    
}
