/*
 * (C) Copyright IBM Corporation 2018.
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
package net.wasdev.wlp.gradle.plugins

import net.wasdev.wlp.gradle.plugins.extensions.ServerExtension
import net.wasdev.wlp.gradle.plugins.tasks.AbstractServerTask

import org.gradle.api.Project
import org.gradle.api.tasks.bundling.War

class LibertySingleServerTasks extends LibertyTasks {

    LibertySingleServerTasks(Project project) {
        super(project)
    }

    void applyTasks() {
        project.compileJSP {
            dependsOn 'installLiberty', 'compileJava'
        }

        project.libertyRun {
            dependsOn 'libertyCreate'

            if (dependsOnApps(project.liberty.server)) dependsOn 'installApps'
        }

        project.libertyStatus {
            dependsOn 'libertyCreate'
        }

        project.libertyCreate {
            dependsOn 'installLiberty'
            // Run install features if configured
            if (dependsOnFeature(project.liberty.server)) finalizedBy 'installFeature'
        }

        project.libertyStart {
            dependsOn 'libertyCreate'

            if (dependsOnApps(project.liberty.server)) dependsOn 'installApps'
        }

        project.libertyPackage {
            dependsOn installDependsOn(project.liberty.server, 'libertyCreate')
        }

        project.deploy {
            dependsOn 'libertyStart'
        }

        project.undeploy {
            dependsOn 'libertyStart'
        }

        project.installFeature {
            if (dependsOnFeature(project.liberty.server)) {
                dependsOn 'libertyCreate'
            } else {
                dependsOn 'installLiberty'
            }
        }

        project.cleanDirs {
            dependsOn 'libertyStop'
        }

        project.installApps {
            dependsOn project.tasks.withType(War), 'libertyCreate'
        }

        project.configureArquillian {
            dependsOn 'installApps', 'processTestResources'
            skipIfArquillianXmlExists = project.arquillianConfiguration.skipIfArquillianXmlExists
            arquillianProperties = project.arquillianConfiguration.arquillianProperties
        }

        if (!dependsOnApps(project.liberty.server)) {
            if (project.plugins.hasPlugin('war') || project.plugins.hasPlugin('ear')) {
                def tasks = project.tasks
                tasks.getByName('libertyRun').dependsOn 'installApps'
                tasks.getByName('libertyStart').dependsOn 'installApps'
                tasks.getByName('libertyPackage').dependsOn 'installApps'
            }
        }

        checkServerEnvProperties(project.liberty.server)
        //Server objects need to be set per task after the project configuration phase
        setServersForTasks()
    }

    private void setServersForTasks(){
        project.tasks.withType(AbstractServerTask).each { task ->
            task.server = project.liberty.server
        }
    }
}
