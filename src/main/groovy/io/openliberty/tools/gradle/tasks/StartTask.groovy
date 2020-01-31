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

import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel
import io.openliberty.tools.ant.ServerTask
import io.openliberty.tools.gradle.utils.*
import io.openliberty.tools.common.plugins.config.ServerConfigDocument
import io.openliberty.tools.gradle.utils.CommonLogger

import java.io.File

class StartTask extends AbstractServerTask {

    protected final String START_APP_MESSAGE_REGEXP = "CWWKZ0001I.*"

    StartTask() {
        configure({
            description 'Starts the Liberty server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })
    }

    @TaskAction
    void start() {
        ServerTask serverTaskStart = createServerTask(project, "start");
        serverTaskStart.setUseEmbeddedServer(server.embedded)
        serverTaskStart.setClean(server.clean)
        serverTaskStart.execute();

        if (server != null && server.verifyAppStartTimeout > 0) {
            ServerTask serverTaskStop = createServerTask(project, "stop")
            serverTaskStop.setUseEmbeddedServer(server.embedded)
            serverTaskStop.initTask()

            def verifyAppStartTimeout = server.verifyAppStartTimeout

            long timeout = verifyAppStartTimeout * 1000
            long endTime = System.currentTimeMillis() + timeout;

            Set<String> appsToVerify = getAppNamesFromServerXml()

            if (server.deploy.dropins != null && !server.deploy.dropins.isEmpty()) {
                server.deploy.dropins.each { Object dropinObj ->
                    if (dropinObj instanceof Task) {
                        appsToVerify += dropinObj.baseName
                    } else if (dropinObj instanceof File) {
                        appsToVerify += getBaseName(dropinObj.name)
                    }
                }
            }

            def verifyAppStartedThreads = appsToVerify.collect { String archiveName ->
                Thread.start {
                    String verify = serverTaskStop.waitForStringInLog(START_APP_MESSAGE_REGEXP + archiveName, timeout, serverTaskStop.getLogFile())
                    if (!verify) {
                        serverTaskStop.execute()
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
                ServerConfigDocument scd = ServerConfigDocument.getInstance(CommonLogger.getInstance(project), serverConfigFile, server.configDirectory, server.bootstrapPropertiesFile, convertPropertiesToMap(server.bootstrapProperties), server.serverEnvFile, false);
                if (scd != null) {
                    appNames = scd.getNames()
                    appNames += scd.getNamelessLocations().collect { String location ->
                            getNameFromLocation(location)
                        }
                }
            }
            catch (Exception e) {
                logger.warn(e.getLocalizedMessage())
            }
        }
        return appNames
    }

    protected String getNameFromLocation(String location) {
        //gets file name from path
        String fileName = location.substring(location.lastIndexOf(File.separator) + 1)
        String appName = getBaseName(fileName)

        boolean foundName = false

        server.deploy.apps.each { app ->
            if (app instanceof Task) {
                if (getArchiveName(app.archiveName).equals(fileName)) {
                    appName = app.baseName
                    foundName = true
                }
            } else if (app instanceof File) {
                if (getArchiveName(app.name).equals(fileName)) {
                    appName = getBaseName(app.name)
                    foundName = true
                }
            }
        }
        //print debug statement if app is in server.xml but not in apps list
        if (!foundName) {
            logger.debug("The application at " + location + " was configured in the server.xml file but could not be found in the list of applications.")
        }
        return appName
    }

    protected String getArchiveName(String archiveName){
        if (server.stripVersion){
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
