/**
 * (C) Copyright IBM Corporation 2014, 2024.
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
package io.openliberty.tools.gradle.tasks

import io.openliberty.tools.gradle.Liberty
import org.gradle.api.Project
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import groovy.xml.XmlParser

class CreateTask extends AbstractServerTask {

    private final String DEFAULT_PATH = project.projectDir.toString() + '/src/main/liberty/config/'

    CreateTask() {
        configure({
            description 'Creates a Liberty server.'
            group 'Liberty'
        })
        outputs.upToDateWhen {
            getServerDir(project).exists() && (new File(getServerDir(project), 'server.xml')).exists() && !isServerDirChanged(project)
        }
    }

    @InputDirectory @Optional
    File getConfigDir() {
        File defaultConfigDir = new File(DEFAULT_PATH)
        if(server.configDirectory != null && server.configDirectory.exists()) {
            return server.configDirectory
        } else if (defaultConfigDir.exists()) {
            return defaultConfigDir
        }
    }

    @InputFile @Optional
    File getServerXmlFile() {
        return getLibertyPropertyFile(server.serverXmlFile, 'server.xml')
    }

    @InputFile @Optional
    File getBootstrapPropertiesFile() {
        return getLibertyPropertyFile(server.bootstrapPropertiesFile, 'bootstrap.properties')
    }

    @InputFile @Optional
    File getJvmOptionsFile() {
        return getLibertyPropertyFile(server.jvmOptionsFile, 'jvm.options')
    }

    @InputFile @Optional
    File getServerEnvFile() {
        return getLibertyPropertyFile(server.serverEnvFile, 'server.env')
    }

    @InputFile
    File getPluginConfigXml() {
        return new File(project.getLayout().getBuildDirectory().getAsFile().get(), 'liberty-plugin-config.xml')
    }

    @TaskAction
    void create() {
        //Checking etc/server.env for outputDirs
        Liberty.checkEtcServerEnvProperties(project)
        File serverDir = getServerDir(project)
        File serverXmlFile = new File(serverDir, "server.xml")
        if(!serverDir.exists()){
            def params = buildLibertyMap(project);
            if (server.template != null && server.template.length() != 0) {
                params.put('template', server.template)
            }
            if (server.noPassword) {
                params.put('noPassword', server.noPassword)
            }
            executeServerCommand(project, 'create', params)
        } else if (copyDefaultServerTemplate(getInstallDir(project), serverDir)) {
            // copied defaultServer template server.xml over for rare case that the server.xml disappears from an existing Liberty server (issue 850)
            // if the project contains its own server.xml file, it will get copied over in copyConfigFiles() next
            logger.warn("The " + serverXmlFile.getAbsolutePath() + " does not exist. Copying over the defaultServer template server.xml file.")
        }
        copyConfigFiles()
    }

    protected boolean isServerDirChanged(Project project) {
        if (!project.getLayout().getBuildDirectory().getAsFile().get().exists() || !(new File(project.getLayout().getBuildDirectory().getAsFile().get(), 'liberty-plugin-config.xml')).exists()) {
            return false
        }

        XmlParser pluginXmlParser = new XmlParser()
        Node libertyPluginConfig = pluginXmlParser.parse(new File(project.getLayout().getBuildDirectory().getAsFile().get(), 'liberty-plugin-config.xml'))
        if (!libertyPluginConfig.getAt('serverDirectory').isEmpty()) {
            File currentDir = getServerDir(project)
            File previousDir = new File(libertyPluginConfig.getAt('serverDirectory')[0].value)
            if (previousDir.exists() && previousDir.equals(currentDir)) {
                return false
            }
            return true
        }
        // if serverDirectory did not exist in the xml file, do not consider this as a change 
        return false
    }

    File getLibertyPropertyFile(File libertyPropertyFile, String fileName) {
        if (libertyPropertyFile != null && libertyPropertyFile.exists()) {
            return libertyPropertyFile
        } else if (server.configDirectory != null && new File(server.configDirectory, fileName).exists()) {
            return new File(server.configDirectory, fileName)
        } else if (new File(DEFAULT_PATH + fileName).exists()) {
            return new File(DEFAULT_PATH + fileName)
        }
    }
}
