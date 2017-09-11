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

import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.Task
import net.wasdev.wlp.ant.ServerTask;

class StartTask extends AbstractServerTask {

    protected final String START_APP_MESSAGE_REGEXP = "CWWKZ0001I.*"

    @TaskAction
    void start() {

        def params = buildLibertyMap(project);
        params.put('clean', server.clean)
        if (server.timeout != null && server.timeout.length() != 0) {
            params.put('timeout', server.timeout)
        }
        executeServerCommand(project, 'start', params)

        ServerTask serverTask = new ServerTask()
        serverTask.setInstallDir(params.get('installDir'))
        serverTask.setServerName(params.get('serverName'))
        serverTask.setUserDir(params.get('userDir'))
        if(params.get('outputDir') == null ) {
            serverTask.setOutputDir(params.get('outputDir'))
        }else {
            serverTask.setOutputDir(new File(params.get('outputDir')))
        }
        serverTask.initTask()

        if (server != null && server.verifyAppStartTimeout > 0) {
            def verifyAppStartTimeout = server.verifyAppStartTimeout

            long timeout = verifyAppStartTimeout * 1000
            long endTime = System.currentTimeMillis() + timeout;

            ArrayList<String> appsToVerify = new ArrayList<String>()
            ArrayList<Task> applicationBuildTasks = new ArrayList<Task>()

            if (server.apps != null && !server.apps.isEmpty()) {
                applicationBuildTasks += server.apps
            }
            if (server.dropins != null && !server.dropins.isEmpty()) {
                applicationBuildTasks += server.dropins
            }

            if (!applicationBuildTasks.isEmpty()) {
                applicationBuildTasks.each{ Task task ->
                    appsToVerify.add(task.baseName)
                }
            }
            else {
                //Do we need to do a stripVersion check here?
                if (project.plugins.hasPlugin('war')) {
                    appsToVerify.add(project.war.baseName)
                }
            }

            for (String archiveName : appsToVerify) {
                String verify = serverTask.waitForStringInLog(START_APP_MESSAGE_REGEXP + archiveName, timeout, serverTask.getLogFile())
                if (!verify) {
                    executeServerCommand(project, 'stop', buildLibertyMap(project))
                    throw new GradleException("The server has been stopped. Unable to verify if the server was started after ${verifyAppStartTimeout} seconds.")
                }
                timeout = endTime - System.currentTimeMillis();
            }
        }
    }
}
