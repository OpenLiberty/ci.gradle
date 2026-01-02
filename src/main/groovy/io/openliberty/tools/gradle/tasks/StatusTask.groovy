/**
 * (C) Copyright IBM Corporation 2014, 2025.
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
import org.gradle.api.logging.LogLevel

import org.gradle.api.tasks.TaskAction

class StatusTask extends AbstractServerTask {

    StatusTask() {
        configure({
            description = 'Checks if the Liberty server is running.'
            group = 'Liberty'
        })
    }

    @TaskAction
    void status() {
        if (isLibertyInstalledAndValid(project)) {
            File serverDir = getServerDir(project)
            if (serverDir.exists()) {
                File serverXmlFile = new File(serverDir,"server.xml")
                if (serverXmlFile.exists()) {
                    def pb = new ProcessBuilder(buildCommand("status"))
                    Map<String, String> envVars = getToolchainEnvVar();
                    if(!envVars.isEmpty()){
                        pb.environment().putAll(envVars);
                    }
                    def status_process= pb.redirectErrorStream(true).start()
                    status_process.inputStream.eachLine {
                        println it
                    }
                } else {
        	        logger.error ('The server status cannot be checked. There is no server.xml file in the server.')
                }
            } else {
        	    logger.error ('The server status cannot be checked. The server has not been created.')
            }
        } else {
            logger.error ('The server status cannot be checked. The runtime has not been installed.')
        }
    }

}
