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

import net.wasdev.wlp.gradle.plugins.Liberty

import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class CreateTask extends AbstractServerTask {

    String defaultPath = project.projectDir.toString() + '/src/main/liberty/config/'

    CreateTask() {
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
    File getConfigFile() {
        if(!server.configFile.toString().equals('default') && server.configFile.exists()) {
            return server.configFile
        } else if (server.configDirectory != null && new File(server.configDirectory, 'server.xml').exists()) {
            return new File(server.configDirectory, 'server.xml')
        } else if (new File(defaultPath + 'server.xml').exists()) {
            return new File(project.projectDir.toString() + '/src/main/liberty/config/server.xml')
        }
    }

    @InputFile @Optional
    File getBootstrapPropertiesFile() {
        if (!server.bootstrapPropertiesFile.toString().equals('default') && server.bootstrapPropertiesFile.exists()) {
            return server.bootstrapPropertiesFile
        } else if (server.configDirectory != null && new File(server.configDirectory, 'bootstrap.properties').exists()) {
            return new File(server.configDirectory, 'bootstrap.properties')
        } else if (new File(defaultPath + 'bootstrap.properties').exists()) {
            return new File(defaultPath + 'bootstrap.properties')
        }
    }

    @InputFile @Optional
    File getJvmOptionsFile() {
        if (!server.jvmOptionsFile.toString().equals('default') && server.jvmOptionsFile.exists()) {
            return server.jvmOptionsFile
        } else if (server.configDirectory != null && new File(server.configDirectory, 'jvm.options').exists()) {
            return new File(server.configDirectory, 'jvm.options')
        } else if (new File(defaultPath + 'jvm.options').exists()) {
            return new File(defaultPath + 'jvm.options')
        }
    }

    @InputFile @Optional
    File getServerEnvFile() {
        if (!server.serverEnv.toString().equals('default') && server.serverEnv.exists()) {
            return server.serverEnv
        } else if (server.configDirectory != null && new File(server.configDirectory, 'server.env').exists()) {
            return new File(server.configDirectory, 'server.env')
        } else if (new File(defaultPath + 'server.env').exists()) {
            return new File(defaultPath + 'server.env')
        }
    }

    @OutputFile
    File getPluginConfigXml() {
        return new File(project.buildDir, 'liberty-plugin-config.xml')
    }

    @TaskAction
    void create() {
        //Checking etc/server.env for outputDirs
        Liberty.checkEtcServerEnvProperties(project)
        if(!getServerDir(project).exists()){
            def params = buildLibertyMap(project);
            if (project.liberty.server.template != null && project.liberty.server.template.length() != 0) {
                params.put('template', project.liberty.server.template)
            }
            executeServerCommand(project, 'create', params)
        }
        copyConfigFiles()
        writeServerPropertiesToXml(project)
    }

}
