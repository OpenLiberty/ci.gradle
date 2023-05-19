/**
 * (C) Copyright IBM Corporation 2015, 2023.
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

import org.gradle.api.tasks.TaskAction
import org.gradle.api.logging.LogLevel

class CleanTask extends AbstractServerTask {
    CleanTask() {
        configure({
            description 'Deletes files from some directories from the Liberty server'
            group 'Liberty'
        })
    }


    @TaskAction
    void cleanDirectories() {
        // first make sure there is a Liberty installation that needs cleaning as a previous clean may have occurred
        File installDir = getInstallDir(project)
        if (isLibertyInstalled(installDir)) {
            def params = buildLibertyMap(project);
            params.put('logs', server.cleanDir.logs)
            params.put('workarea', server.cleanDir.workarea)
            params.put('dropins', server.cleanDir.dropins)
            params.put('apps', server.cleanDir.apps)
            params.remove('timeout')
            project.ant.taskdef(name: 'cleanDir',
                                classname: 'io.openliberty.tools.ant.CleanTask',
                                classpath: project.buildscript.configurations.classpath.asPath)
            project.ant.cleanDir(params)
        } else {
            logger.info("There is no Liberty server to clean. The runtime has not been installed.")
        }
    }

    private boolean isLibertyInstalled(File installDir) {
        boolean installationExists = installDir.exists() && new File(installDir,"lib/ws-launch.jar").exists()
        return installationExists
    }
}
