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
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.gradle.api.logging.LogLevel

import io.openliberty.tools.ant.ServerTask
import io.openliberty.tools.common.plugins.config.ServerConfigDocument

class UndeployTask extends AbstractServerTask {

    private static final String STOP_APP_MESSAGE_CODE_REG = "CWWKZ0009I.*"
    private static final long APP_STOP_TIMEOUT_DEFAULT = 30 * 1000

    protected ServerConfigDocument scd

    protected List<File> appFiles = new ArrayList<File>()

    UndeployTask() {
        configure({
            description 'Removes an application from the Liberty server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })
    }

    @TaskAction
    void undeploy() {
        if (server.undeploy != null) {
            if (server.undeploy.apps != null && !server.undeploy.apps.isEmpty()) {
                def apps = getAppFiles(server.undeploy.apps, 'apps')
                apps.each { undeployApp(it, 'apps') }
            }
            if (server.undeploy.dropins != null && !server.undeploy.dropins.isEmpty()) {
                def dropins = getAppFiles(server.undeploy.dropins, 'dropins')
                dropins.each { undeployApp(it, 'dropins') }
            }
        }
    }

    private void getAppFiles(List<Object> allApps, String appsDir) {
        File installDir = new File(getServerDir(project), appsDir)
        allApps.each { Object appObj ->
            if (appObj instanceof org.gradle.api.tasks.bundling.AbstractArchiveTask) { //War or ear task
                if (server.looseApplication) {
                    appFiles.add(new File(installDir, getLooseConfigFileName((Task)appObj)))
                } else {
                    appFiles.add(new File(installDir, getArchiveName((Task)appObj)))
                }
            } else if (appObj instanceof File) {
                appFiles.add(new File(installDir, ((File)appObj).getName()))
            } else {
                logger.warn('Application ' + appObj.getClass.name + ' is expressed as ' + appObj.toString() + ' which is not a supported input type. Define applications using Task or File objects.')
            }
        }
    }

    protected void undeployApp(File file, String appsDir) throws GradleException {
        String appName = file.getName().substring(0, file.getName().lastIndexOf('.'))

        if (appsDir.equals("apps")) {
            scd = null

            File serverXML = new File(getServerDir(project).getCanonicalPath(), "server.xml")

            try {
                scd = ServerConfigDocument.getInstance(CommonLogger.getInstance(project), serverXML, server.configDirectory,
                        server.bootstrapPropertiesFile, server.bootstrapProperties, server.serverEnvFile, false)

                //appName will be set to a name derived from appFile if no name can be found.
                appName = scd.findNameForLocation(file)
            } catch (Exception e) {
                logger.warn(e.getLocalizedMessage())
            } 
        }

        FileUtils.delete(file)

        //check stop message code
        String stopMessage = STOP_APP_MESSAGE_CODE_REG + appName
        ServerTask serverTask = createServerTask(project, null) //Using a server task without an opertation to check logs for app undeploy message
        if (serverTask.waitForStringInLog(stopMessage, appStopTimeout, new File(new File(getOutputDir(project), server.name), "logs/messages.log")) == null) {
            throw new GradleException("CWWKM2022E: Failed to undeploy application " + file.getPath() + ". The Stop application message cannot be found in console.log.")
        }
    }

}
