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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory

class CreateTask extends AbstractServerTask {

    @InputFiles
    def configCollection

    @Input
    String serverName

    @OutputDirectory
    def serverDir

    @TaskAction
    void create() {
        if(!getServerDir(project).exists()){
            println ("Making the server for the first time.")
            def params = buildLibertyMap(project);
            if (project.liberty.server.template != null && project.liberty.server.template.length() != 0) {
                params.put('template', project.liberty.server.template)
            }
            executeServerCommand(project, 'create', params)
        } else {
            println ("The server already exists.")
        }
        copyConfigFiles();
    }



}
