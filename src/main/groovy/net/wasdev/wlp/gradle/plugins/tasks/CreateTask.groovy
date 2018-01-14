/**
 * (C) Copyright IBM Corporation 2014, 2018
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

import java.io.File;

import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction

import groovy.lang.Closure;

class CreateTask extends AbstractServerTask {

	@InputDirectory @Optional
	File getConfigDir() {
		if(project.liberty.server.configDirectory != null && project.liberty.server.configDirectory)
			return project.liberty.server.configDirectory;
	}

	@OutputDirectory
	File getOutputConfigDir() {
		return getServerDir(project);
	}

	@InputFile
	File getConfigFile() {
		File configFile = project.liberty.server.configFile
		File configDir = project.liberty.server.configDirectory

		if(!server.configFile.toString().equals('default')) {
			return configFile;
		} else if(configDir != null && new File(configDir, "server.xml").exists()) {
			return new File(configDir, "server.xml");
		} else {
			// Return the default value wether or not it exists, if nothing else is configured
			return new File(project.projectDir.toString() + '/src/main/liberty/config/server.xml')
		}
	}

	@OutputFile
	File getOutputConfigFile() {
		return new File(getServerDir(project), "server.xml")
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
