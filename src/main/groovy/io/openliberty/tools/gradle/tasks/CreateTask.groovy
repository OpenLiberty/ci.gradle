/**
 * (C) Copyright IBM Corporation 2014, 2020.
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

import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.logging.LogLevel

class CreateTask extends AbstractServerTask {

    private final String DEFAULT_PATH = project.projectDir.toString() + '/src/main/liberty/config/'

    CreateTask() {
        configure({
            description 'Creates a Liberty server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })
        outputs.upToDateWhen {
            getServerDir(project).exists() && new File(getServerDir(project), 'server.xml')
        }
    }

    @InputDirectory @Optional
    File getConfigDir() {
        if(server.configDirectory != null && server.configDirectory.exists()) {
            return server.configDirectory
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
        return new File(project.buildDir, 'liberty-plugin-config.xml')
    }

    @TaskAction
    void create() {
        //Checking etc/server.env for outputDirs
        Liberty.checkEtcServerEnvProperties(project)
        if(!getServerDir(project).exists()){
            def params = buildLibertyMap(project);
            if (server.template != null && server.template.length() != 0) {
                params.put('template', server.template)
            }
            if (server.noPassword) {
                params.put('noPassword', server.noPassword)
            }
            executeServerCommand(project, 'create', params)
        }
        copyConfigFiles()
        writeServerPropertiesToXml(project)
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
