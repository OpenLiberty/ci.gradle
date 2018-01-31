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

import net.wasdev.wlp.gradle.plugins.extensions.LibertyExtension
import net.wasdev.wlp.gradle.plugins.utils.LibertyIntstallController

import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import groovy.xml.MarkupBuilder

class InstallLibertyTask extends AbstractTask {

    @TaskAction
    void install() {
        if (!isLibertyInstalled(project)) {
            def params = buildInstallLibertyMap(project)

            project.ant.taskdef(name: 'installLiberty',
                                classname: net.wasdev.wlp.ant.install.InstallLibertyTask.name,
                                classpath: project.rootProject.buildscript.configurations.classpath.asPath)
            project.ant.installLiberty(params)

            String licenseFilePath = project.configurations.getByName('libertyLicense').getAsPath()
            if (licenseFilePath) {
                def command = "java -jar " + licenseFilePath + " --acceptLicense " + project.buildDir
                def process = command.execute()
                process.waitFor()
            }
        } else {
            logger.info ("Liberty is already installed at: ${LibertyIntstallController.getInstallDir(project)}")
        }

        createPluginXmlFile(project)
    }

    private Map<String, String> buildInstallLibertyMap(Project project) {

        LibertyExtension liberty = project.getExtensions().getByType(LibertyExtension)
        Map<String, String> result = new HashMap();
        if (project.liberty.install.licenseCode != null) {
           result.put('licenseCode', liberty.install.licenseCode)
        }

        if (project.liberty.install.version != null) {
            result.put('version', liberty.install.version)
        }

        if (project.liberty.install.type != null) {
            result.put('type', liberty.install.type)
        }

        String runtimeFilePath = project.configurations.getByName('libertyRuntime').getAsPath()
        if (runtimeFilePath) {
            logger.debug 'Liberty archive file Path to the local Gradle repository  : ' + runtimeFilePath

            File localFile = new File(runtimeFilePath)

            if (localFile.exists()) {
                logger.debug 'Getting WebSphere Liberty archive file from the local Gradle repository.'
                result.put('runtimeUrl', localFile.toURI().toURL().toString())
            }
        } else if (project.liberty.install.runtimeUrl != null) {
            result.put('runtimeUrl', liberty.install.runtimeUrl)
        }

        if (project.liberty.install.baseDir == null) {
           result.put('baseDir', project.buildDir.absolutePath)
        } else {
           result.put('baseDir', liberty.install.baseDir)
        }

        if (project.liberty.install.cacheDir != null) {
            result.put('cacheDir', liberty.install.cacheDir)
        }

        if (project.liberty.install.username != null) {
            result.put('username', liberty.install.username)
            result.put('password', liberty.install.password)
        }

        result.put('maxDownloadTime', liberty.install.maxDownloadTime)

        result.put('offline', project.gradle.startParameter.offline.toString())

        return result
    }

    protected void outputLibertyPropertiesToXml(MarkupBuilder xmlDoc) {
        xmlDoc.installDirectory (LibertyIntstallController.getInstallDir(project).toString())

        if (project.configurations.libertyRuntime != null) {
            project.configurations.libertyRuntime.dependencies.each { libertyArtifact ->
                xmlDoc.assemblyArtifact {
                    groupId (libertyArtifact.group)
                    artifactId (libertyArtifact.name)
                    version (libertyArtifact.version)
                    type ('zip')
                }
                xmlDoc.assemblyArchive (project.configurations.libertyRuntime.resolvedConfiguration.resolvedArtifacts.getAt(0).file.toString())
            }
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
}
