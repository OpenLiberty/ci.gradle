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

import org.gradle.api.Task
import org.gradle.api.tasks.bundling.War

import net.wasdev.wlp.gradle.plugins.utils.*;

class InstallAppsTask extends AbstractServerTask {

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
            if ((server.apps == null || server.apps.isEmpty()) && (server.dropins == null || server.dropins.isEmpty())){
                if(project.plugins.hasPlugin('war')) {
                    server.apps = [project.war]
                    installMultipleApps(server.apps, 'apps')
                }
            }
            else {
                if (server.apps != null && !server.apps.isEmpty()) {
                    installMultipleApps(server.apps, 'apps')
                }
                if (server.dropins != null && !server.dropins.isEmpty()) {
                    installMultipleApps(server.dropins, 'dropins')
                }
            }
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
        Files.copy(archive.toPath(), new File(getServerDir(project), "/" + server.installapps.appsDirectory + "/" + getArchiveName(archive.getName())).toPath(), StandardCopyOption.REPLACE_EXISTING)

        validateAppConfig(getArchiveName(archive.getName()), getBaseName(archive))
    }

    private void installMultipleApps(List<Task> applications, String appsDir){
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

        if(appsDir.equalsIgnoreCase('apps') && !isAppConfiguredInSourceServerXml(fileName)){
            applicationXml.createApplicationElement(fileName, artifactId)
        }
        else if(appsDir.equalsIgnoreCase('dropins') && isAppConfiguredInSourceServerXml(fileName)){
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
        if(server.installapps.stripVersion){
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
