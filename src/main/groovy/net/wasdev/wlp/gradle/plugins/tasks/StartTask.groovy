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
import net.wasdev.wlp.ant.ServerTask
import net.wasdev.wlp.gradle.plugins.utils.*
import java.io.File

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
        serverTask.setOutputDir(getOutputDir(params))
        serverTask.initTask()

        if (server != null && server.verifyAppStartTimeout > 0) {
            def verifyAppStartTimeout = server.verifyAppStartTimeout

            long timeout = verifyAppStartTimeout * 1000
            long endTime = System.currentTimeMillis() + timeout;

            Set<String> appsToVerify = getAppNamesFromServerXml()
            ArrayList<Task> applicationBuildTasks = new ArrayList<Task>()

            if (server.dropins != null && !server.dropins.isEmpty()) {
                applicationBuildTasks += server.dropins
            }

            if (!applicationBuildTasks.empty) {
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

            def verifyAppStartedThreads = appsToVerify.collect { String archiveName ->
                Thread.start {
                    String verify = serverTask.waitForStringInLog(START_APP_MESSAGE_REGEXP + archiveName, timeout, serverTask.getLogFile())
                    if (!verify) {
                        executeServerCommand(project, 'stop', buildLibertyMap(project))
                        throw new GradleException("The server has been stopped. Unable to verify if the server was started after ${verifyAppStartTimeout} seconds.")
                    }
                }
            }
            verifyAppStartedThreads*.join()
        }
    }

    private Set<String> getAppNamesFromServerXml() {
        Set<String> appNames

        File serverConfigFile = new File(getServerDir(project), 'server.xml')
        if (serverConfigFile != null && serverConfigFile.exists()) {
            try {
                ServerConfigDocument scd = ServerConfigDocument.getInstance(serverConfigFile, server.configDirectory, server.bootstrapPropertiesFile, server.bootstrapProperties, server.serverEnv)
                if (scd != null) {
                    appNames = scd.getNames()
                    appNames += scd.getNamelessLocations().collect { String location ->
                            getNameFromLocation(location)
                        }
                }
            }
            catch (Exception e) {
                logger.warn(e.getLocalizedMessage())
                logger.debug(e.toString())
            }
        }
        return appNames
    }

    protected String getNameFromLocation(String location) {
        //gets file name from path
        String fileName = location.substring(location.lastIndexOf(File.separator) + 1)
        String appName = getBaseName(fileName)

        boolean foundName = false

        server.apps.each { task ->
            if (getArchiveName(task.archiveName).equals(fileName)) { //stripVersion?
                appName = task.baseName
                foundName = true
            }
        }
        //print debug statement if app is in server.xml but not in apps list
        if (!foundName) {
            logger.debug("The application at " + location + " was configured in the server.xml file but could not be found in the list of applications.")
        }
        return appName
    }

    protected String getArchiveName(String archiveName){
        if (server.installapps.stripVersion){
            StringBuilder sbArchiveName = new StringBuilder().append("-").append(project.version)
            return archiveName.replaceAll(sbArchiveName.toString(),"")
        }
        return archiveName;
    }

    protected String getBaseName(String fileName) {
        File file = new File(fileName)
        return file.name.take(getArchiveName(file.name).lastIndexOf('.'))
    }
}
