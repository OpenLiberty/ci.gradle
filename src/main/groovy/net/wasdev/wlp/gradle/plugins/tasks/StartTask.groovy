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
import net.wasdev.wlp.ant.ServerTask;

class StartTask extends AbstractTask {

    protected final String START_APP_MESSAGE_REGEXP = "CWWKZ0001I.*"

    @TaskAction
    void start() {
        def params = buildLibertyMap(project);
        params.put('clean', project.liberty.clean)
        if (project.liberty.timeout != null && project.liberty.timeout.length() != 0) {
            params.put('timeout', project.liberty.timeout)
        }
        executeServerCommand(project, 'start', params)
        
        ServerTask serverTask = new ServerTask()
        serverTask.setInstallDir(params.get('installDir'))
        serverTask.setServerName(params.get('serverName'))
        serverTask.setUserDir(params.get('userDir'))
        serverTask.setOutputDir(params.get('outputDir'))
        serverTask.initTask()
        
        def verifyTimeout = project.liberty.verifyTimeout
        if(project.liberty.verifyTimeout < 0) {
            verifyTimeout = 30
        }
        long timeout = verifyTimeout * 1000
        long endTime = System.currentTimeMillis() + timeout;
        if(project.liberty.applications) {
            String[] apps = project.liberty.applications.split("[,\\s]+")
            for(String archiveName : apps) {
                String verify = serverTask.waitForStringInLog(START_APP_MESSAGE_REGEXP + archiveName, timeout, serverTask.getLogFile())
                if (!verify) { 
                    executeServerCommand(project, 'stop', buildLibertyMap(project))
                    throw new GradleException("The server has been stopped. Unable to verify if the server was started after ${verifyTimeout} seconds.")
                }
                timeout = endTime - System.currentTimeMillis();
            }
        }
    }
}