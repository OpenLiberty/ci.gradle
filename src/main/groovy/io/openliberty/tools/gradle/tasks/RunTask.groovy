/**
 * (C) Copyright IBM Corporation 2014, 2026.
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
import io.openliberty.tools.ant.ServerTask

class RunTask extends AbstractServerTask {

    RunTask() {
        configure({
            description = "Runs a Liberty server under the Gradle process."
            group = 'Liberty'
        })
    }

    @TaskAction
    void run() {
        addShutdownHook {
            if (isLibertyInstalledAndValid(project)) {
                File serverDir = getServerDir(project)
                if (serverDir.exists()) {
                    // copy default server template server.xml file over if the server.xml file does not exist so that the server can be stopped
                    boolean defaultServerTemplateUsed = copyDefaultServerTemplate(getInstallDir(project), serverDir)                    
                    File serverXmlFile = new File(getServerDir(project),"server.xml")
                    if (serverXmlFile.exists()) {
                        ServerTask serverTaskStop = createServerTask(project, "stop");
                        serverTaskStop.setUseEmbeddedServer(server.embedded)
                        serverTaskStop.execute()
                    }
                    if (defaultServerTemplateUsed) {
                        serverXmlFile.delete() // delete the temporary copy of the server.xml file
                    }                
                }
            }
        }

        if (server.embedded) {
            ServerTask serverTaskRun = createServerTask(project, "run");
            serverTaskRun.setUseEmbeddedServer(server.embedded)
            serverTaskRun.setClean(server.clean)
            
            // When using embedded mode with Gradle daemon, the shutdown hook may not execute on Ctrl+C
            // because the daemon JVM continues running. Warn the user about this limitation.
            logger.warn("Starting Liberty server in embedded mode. Note: When using the Gradle daemon, " +
                       "the server may continue running after pressing Ctrl+C. " +
                       "Use 'gradle libertyStop' to stop the server or 'gradle --stop' to stop the daemon.")
            
            serverTaskRun.execute();
        } else {
            List<String> command = buildCommand("run")
            if (server.clean) {
                command.add("--clean")
            }
            def pb = new ProcessBuilder(command)
            pb.environment().put('WLP_USER_DIR', getUserDir(project).getCanonicalPath())
            addToolchainEnvToProcessBuilder(pb)
            def run_process = pb.redirectErrorStream(true).start()

            run_process.inputStream.eachLine {
                println it
            }
        }                
    }
}
