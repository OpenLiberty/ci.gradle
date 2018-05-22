/**
 * (C) Copyright IBM Corporation 2014, 2018.
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
import org.gradle.api.logging.LogLevel

class UndeployTask extends AbstractServerTask {

    UndeployTask() {
        configure({
            description 'Removes an application from the Liberty server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })
    }

    @TaskAction
    void undeploy() {
        def params = buildLibertyMap(project);

        project.ant.taskdef(name: 'undeploy',
                            classname: 'net.wasdev.wlp.ant.UndeployTask',
                            classpath: project.buildscript.configurations.classpath.asPath)

        def application = server.undeploy.application
        def include = server.undeploy.include
        def exclude = server.undeploy.exclude

        if (application != null) {
            params.put('file', application)
            project.ant.undeploy(params)
        } else if ((include != null && !include.isEmpty()) || exclude != null) {
            project.ant.undeploy(params) {
                patternset(includes: include, excludes: exclude)
            }
        } else {
            if (project.plugins.hasPlugin("war")) {
                params.put('file', project.war.archiveName)
            }

            if (project.plugins.hasPlugin("ear")) {
                params.put('file', project.ear.archiveName)
            }
            project.ant.undeploy(params)
        }
    }

}
