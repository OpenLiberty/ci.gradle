/**
 * (C) Copyright IBM Corporation 2014, 2018.
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

import javax.xml.parsers.*

import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.OutputFile
import org.gradle.api.artifacts.Dependency
import groovy.xml.MarkupBuilder
import org.gradle.api.GradleException
import org.apache.commons.io.FileUtils;


class InstallLibertyTask extends AbstractTask {

    InstallLibertyTask() {
        outputs.upToDateWhen {
            getInstallDir(project).exists() && isInstalled()
        }
    }

    /*
    @OutputFile
    File getPluginConfigXml() {
        return new File(project.buildDir, 'liberty-plugin-config.xml')
    }*/

    @TaskAction
    void install() {
        File wlpDirectory = new File(project.buildDir,"wlp")
        if (wlpDirectory.exists()) {
            FileUtils.deleteDirectory(wlpDirectory);
        }

        def params = buildInstallLibertyMap(project)

        project.ant.taskdef(name: 'installLiberty',
                            classname: 'net.wasdev.wlp.ant.install.InstallLibertyTask',
                            classpath: project.buildscript.configurations.classpath.asPath)
        project.ant.installLiberty(params)

        String licenseFilePath = project.configurations.getByName('libertyLicense').getAsPath()
        if (licenseFilePath) {
            def command = "java -jar " + licenseFilePath + " --acceptLicense " + project.buildDir
            def process = command.execute()
            process.waitFor()
        }

        createPluginXmlFile(project)
    }

    private Map<String, String> buildInstallLibertyMap(Project project) {

        Map<String, String> result = new HashMap();
        if (project.liberty.install.licenseCode != null) {
           result.put('licenseCode', project.liberty.install.licenseCode)
        }

        if (project.liberty.install.version != null) {
            result.put('version', project.liberty.install.version)
        }

        if (project.liberty.install.type != null) {
            result.put('type', project.liberty.install.type)
        }

        if(project.configurations.libertyRuntime.getFiles().size() > 1) {
            throw new GradleException("The liberty gradle plugin only supports one libertyRuntime, but more than one was specified. Please set only one libertyRuntime.")
        }
        String runtimeFilePath = project.configurations.getByName('libertyRuntime').getAsPath()
        if (runtimeFilePath) {
            logger.debug 'Liberty archive file Path to the local Gradle repository  : ' + runtimeFilePath

            File localFile = new File(runtimeFilePath)

            if (localFile.exists()) {
                logger.debug 'Getting WebSphere Liberty archive file from the local Gradle repository.'
                result.put('runtimeUrl', localFile.toURI().toURL())
            }
        } else if (project.liberty.install.runtimeUrl != null) {
            result.put('runtimeUrl', project.liberty.install.runtimeUrl)
        }

        if (project.liberty.install.baseDir == null) {
           result.put('baseDir', project.buildDir)
        } else {
           result.put('baseDir', project.liberty.install.baseDir)
        }

        if (project.liberty.install.cacheDir != null) {
            result.put('cacheDir', project.liberty.install.cacheDir)
        }

        if (project.liberty.install.username != null) {
            result.put('username', project.liberty.install.username)
            result.put('password', project.liberty.install.password)
        }

        result.put('maxDownloadTime', project.liberty.install.maxDownloadTime)

        result.put('offline', project.gradle.startParameter.offline)

        return result
    }

    protected void outputLibertyPropertiesToXml(MarkupBuilder xmlDoc) {
        xmlDoc.installDirectory (getInstallDir(project).toString())
        if (project.configurations.libertyRuntime != null && !project.configurations.libertyRuntime.dependencies.isEmpty()) {
            Dependency libertyArtifact = project.configurations.libertyRuntime.dependencies.toArray()[0]
            xmlDoc.assemblyArtifact {
                groupId (libertyArtifact.group)
                artifactId (libertyArtifact.name)
                version (libertyArtifact.version)
                type ('zip')
            }
            xmlDoc.assemblyArchive (project.configurations.libertyRuntime.resolvedConfiguration.resolvedArtifacts.getAt(0).file.toString())
        } else if (project.liberty.install.runtimeUrl != null) {
            xmlDoc.runtimeUrl (project.liberty.install.runtimeUrl)
        }
    }

    protected void createPluginXmlFile(Project project) {
        new File(project.buildDir, 'liberty-plugin-config.xml').withWriter { writer ->
            def xmlDoc = new MarkupBuilder(writer)
            xmlDoc.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")
            xmlDoc.'liberty-plugin-config'('version':'2.0') {
                outputLibertyPropertiesToXml(xmlDoc)
            }
        }
    }
    
    
    protected boolean isInstalled() {
        boolean isUpToDate = false;

        File f = new File(project.buildDir, 'liberty-plugin-config.xml')
        if (f == null || !f.exists()) {
            return isUpToDate;
        }

        XmlParser pluginXmlParser = new XmlParser()
        Node libertyPluginConfig = pluginXmlParser.parse(f)

        if(project.configurations.libertyRuntime != null && !project.configurations.libertyRuntime.dependencies.isEmpty()) {
            if (!libertyPluginConfig.getAt('assemblyArtifact').isEmpty()) {
                Dependency libertyArtifact = project.configurations.libertyRuntime.dependencies.toArray()[0]
                if(libertyPluginConfig.getAt('assemblyArtifact').getAt('artifactId').text().equals(libertyArtifact.name)
                    && libertyPluginConfig.getAt('assemblyArtifact').getAt('version').text().equals(libertyArtifact.version)
                    && libertyPluginConfig.getAt('assemblyArtifact').getAt('groupId').text().equals(libertyArtifact.group)) {
                        isUpToDate = true;
                }
            }
        }
        else if (project.liberty.install.runtimeUrl != null && !libertyPluginConfig.getAt('runtimeUrl').isEmpty()
            && project.liberty.install.runtimeUrl.equals(libertyPluginConfig.getAt('runtimeUrl').text())) {
                isUpToDate = true;
        }
        return isUpToDate;
    }
}
