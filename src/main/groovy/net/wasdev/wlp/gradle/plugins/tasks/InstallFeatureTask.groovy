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

class InstallFeatureTask extends AbstractTask {

    @TaskAction
    void installFeature() {
        def params = buildLibertyMap(project);
        params.put('name', project.liberty.features.name.join(","))
        params.put('acceptLicense', project.liberty.features.acceptLicense)
        if (project.liberty.features.to != null) {
            params.put('to', project.liberty.features.to)
        }
        if (project.liberty.features.from != null) {
            params.put('from', project.liberty.features.from)
        }
        params.remove('timeout')
        project.ant.taskdef(name: 'installFeature', 
                            classname: 'net.wasdev.wlp.ant.InstallFeatureTask', 
                            classpath: project.buildscript.configurations.classpath.asPath)
        project.ant.installFeature(params)
    }

}
