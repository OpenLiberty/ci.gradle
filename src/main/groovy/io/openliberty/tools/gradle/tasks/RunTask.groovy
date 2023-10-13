/**
 * (C) Copyright IBM Corporation 2014, 2023.
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
import io.openliberty.tools.ant.ServerTask

class RunTask extends AbstractServerTask {

    RunTask() {
        configure({
            description "Runs a Liberty server under the Gradle process."
            group 'Liberty'
        })
    }

    @TaskAction
    void run() {
        addShutdownHook {
            if (isLibertyInstalledAndValid(project)) {
                if (getServerDir(project).exists()) {
                    File serverXmlFile = new File(getServerDir(project),"server.xml")
                    if (serverXmlFile.exists()) {
                        ServerTask serverTaskStop = createServerTask(project, "stop");
                        serverTaskStop.setUseEmbeddedServer(server.embedded)
                        serverTaskStop.execute()
                    }
                }
            }
        }

        if (isLibertyInstalledAndValid(project)) {
            File serverDir = getServerDir(project)
            if (serverDir.exists()) {
                File serverXmlFile = new File(serverDir,"server.xml")
                if (serverXmlFile.exists()) {
                    if (server.embedded) {
                        ServerTask serverTaskRun = createServerTask(project, "run");
                        serverTaskRun.setUseEmbeddedServer(server.embedded)
                        serverTaskRun.setClean(server.clean)
                        serverTaskRun.execute();
                    } else {
                        List<String> command = buildCommand("run")
                        if (server.clean) {
                            command.add("--clean")
                        }
                        def pb = new ProcessBuilder(command)
                        pb.environment().put('WLP_USER_DIR', getUserDir(project).getCanonicalPath())

                        def run_process = pb.redirectErrorStream(true).start()

                        run_process.inputStream.eachLine {
                            println it
                        }
                    }                
                } else {
        	        logger.error ('There is no server.xml file in the server. The server cannot be run.')
                }
            } else {
        	    logger.error ('There is no server to run. The server has not been created.')
            }
        } else {
            logger.error ('There is no server to run. The runtime has not been installed.')
        }
    }
}
