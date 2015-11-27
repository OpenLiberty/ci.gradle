/**
 * (C) Copyright IBM Corporation 2015.
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

class CleanTask extends AbstractTask {

    @TaskAction
    void installFeature() {
        def params = buildLibertyMap(project);
        params.put('logs', project.liberty.cleanDir.logs)
        params.put('workarea', project.liberty.cleanDir.workarea)
        params.put('dropins', project.liberty.cleanDir.dropins)
        params.put('apps', project.liberty.cleanDir.apps)
        params.remove('timeout')
        project.ant.taskdef(name: 'cleanDir', 
                            classname: 'net.wasdev.wlp.ant.CleanTask', 
                            classpath: project.buildscript.configurations.classpath.asPath)
        project.ant.cleanDir(params)
    }
}