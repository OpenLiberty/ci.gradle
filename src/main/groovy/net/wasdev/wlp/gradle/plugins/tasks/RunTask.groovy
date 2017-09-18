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

import org.gradle.api.tasks.TaskAction

class RunTask extends AbstractServerTask {

    @TaskAction
    void run() {
        List<String> command = buildCommand("run")
        if (project.liberty.clean) {
            command.add("--clean")
        }
        checkUserDir()

        def run_process = new ProcessBuilder(command).redirectErrorStream(true).start()
        addShutdownHook {
            run_process.waitFor()
        }
        run_process.inputStream.eachLine {
            println it
        }
    }

    void checkUserDir() {
        if (getUserDir(project) != new File(getInstallDir(project),'usr')) {
            File etcDir = new File(getInstallDir(project), 'etc')
            if (!etcDir.exists()) {
                etcDir.mkdir()
            }
            File etcServerEnv = new File(etcDir, 'server.env')
            if (!etcServerEnv.exists()) {
                etcServerEnv.write('WLP_USER_DIR=' + getUserDir(project).toString())
            } else if (!etcServerEnv.text.contains('WLP_USER_DIR')) {
                etcServerEnv.append('WLP_USER_DIR=' + getUserDir(project).toString())
            }
        }
    }
}
