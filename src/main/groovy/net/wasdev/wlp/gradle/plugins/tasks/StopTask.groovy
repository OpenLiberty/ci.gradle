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
import org.gradle.api.logging.LogLevel

import org.gradle.api.tasks.TaskAction

class StopTask extends AbstractServerTask {

    StopTask() {
        configure({
            description 'Stops the Liberty server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })
    }

    @TaskAction
    void stop() {
        if (isLibertyInstalled(project)) {
            if (getServerDir(project).exists()) {
                executeServerCommand(project, 'stop', buildLibertyMap(project))
        	} else {
        	   logger.error ('There is no server to stop. The server has not been created.')
        	}
        } else {
            logger.error ('There is no server to stop. The runtime has not been installed.')
        }
    }
}
