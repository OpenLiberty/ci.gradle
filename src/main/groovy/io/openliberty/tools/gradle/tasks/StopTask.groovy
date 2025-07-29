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
import io.openliberty.tools.ant.ServerTask
import io.openliberty.tools.gradle.utils.ServerUtils

class StopTask extends AbstractServerTask {

    StopTask() {
        configure({
            description = 'Stops the Liberty server.'
            group = 'Liberty'
        })
    }

    @TaskAction
    void stop() {
        File serverDir = getServerDir(project)

        // Clean up force-stopped marker file if it exists (since this is a normal stop)
        ServerUtils.cleanupForceStoppedMarker(getServerDir(project), logger)

        if (isLibertyInstalledAndValid(project)) {
            if (serverDir.exists()) {
                File serverXmlFile = new File(serverDir,"server.xml")
                boolean defaultServerTemplateUsed = copyDefaultServerTemplate(getInstallDir(project),serverDir)
                if (serverXmlFile.exists()) {
                    ServerTask serverTaskStop = createServerTask(project, "stop");
                    serverTaskStop.setUseEmbeddedServer(server.embedded)
                    serverTaskStop.execute()
                    
                    // Verify server is fully stopped and resources are released
                    if (!ServerUtils.verifyServerFullyStopped(serverDir, logger)) {
                        // If normal stop verification fails, try forced cleanup
                        ServerUtils.forceCleanupServerResources(serverDir, logger)
                    }
                } else {
        	        logger.error ('The server cannot be stopped. There is no server.xml file in the server.')
                }

                if (defaultServerTemplateUsed) {
        	        logger.warn ('The server.xml file was missing in the server during the stop task. Copied the defaultServer template server.xml file into the server temporarily so the stop task could be completed.')
                    if (!serverXmlFile.delete()) {
                        logger.error('Could not delete the server.xml file copied from the defaultServer template after stopping the server.')
                    }
                }
            } else {
        	    logger.error ('There is no server to stop. The server has not been created.')
            }
        } else {
            logger.error ('There is no server to stop. The runtime has not been installed.')
        }
    }
}
