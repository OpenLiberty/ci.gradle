/**
 * (C) Copyright IBM Corporation 2014, 2019.
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
            logging.level = LogLevel.INFO
            group 'Liberty'
        })
    }

    @TaskAction
    void run() {
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
            addShutdownHook {
                run_process.waitFor()
            }
            run_process.inputStream.eachLine {
                println it
            }
        }
    }
}
