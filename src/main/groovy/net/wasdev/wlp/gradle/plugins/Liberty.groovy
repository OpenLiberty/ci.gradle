/**
 * (C) Copyright IBM Corporation 2014, 2015.
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

import org.gradle.api.*

import net.wasdev.wlp.gradle.plugins.extensions.LibertyExtension
import net.wasdev.wlp.gradle.plugins.tasks.StartTask
import net.wasdev.wlp.gradle.plugins.tasks.StopTask
import net.wasdev.wlp.gradle.plugins.tasks.StatusTask
import net.wasdev.wlp.gradle.plugins.tasks.CreateTask
import net.wasdev.wlp.gradle.plugins.tasks.RunTask
import net.wasdev.wlp.gradle.plugins.tasks.PackageTask
import net.wasdev.wlp.gradle.plugins.tasks.DeployTask
import net.wasdev.wlp.gradle.plugins.tasks.UndeployTask
import net.wasdev.wlp.gradle.plugins.tasks.InstallFeatureTask
import net.wasdev.wlp.gradle.plugins.tasks.InstallLibertyTask

import org.gradle.api.logging.LogLevel

class Liberty implements Plugin<Project> {

    void apply(Project project) {

        project.plugins.apply 'war'

        project.extensions.create('liberty', LibertyExtension)

        project.task('installLiberty', type: InstallLibertyTask) {
            description 'Installs Liberty from a repository'
            logging.level = LogLevel.INFO
        }

       try {
            project.task('libertyRun', type: RunTask) {
                description = "Runs a WebSphere Liberty Profile server under the Gradle process."
                logging.level = LogLevel.INFO
            }
        } catch (Exception e) {
            project.task('libertyRun') {
                description = "Runs a WebSphere Liberty Profile server under the Gradle process."
                logging.level = LogLevel.INFO
                doLast {
                    println ("This task can't be executed because some dependencies are missing")
                }
            }
        }

        project.task('libertyStatus', type: StatusTask) {
            description 'Checks the WebSphere Liberty Profile server is running.'
            logging.level = LogLevel.INFO
        }

        project.task('libertyCreate', type: CreateTask) {
            description 'Creates a WebSphere Liberty Profile server.'
            outputs.file { new File(getUserDir(project), "servers/${project.liberty.serverName}/server.xml") }
            logging.level = LogLevel.INFO
        }

        project.task('libertyStart', type: StartTask) {
            description 'Starts the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
        }

        project.task('libertyStop', type: StopTask) {
            description 'Stops the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
        }
        project.tasks.clean.dependsOn project.tasks.libertyStop

        project.task('libertyPackage', type: PackageTask) {
            description 'Generates a WebSphere Liberty Profile server archive.'
            logging.level = LogLevel.INFO
        }

        project.task('deployWar', type: DeployTask) {
            description 'Deploys a WAR file to the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
        }

        project.task('undeployWar', type: UndeployTask) {
            description 'Removes a WAR file from the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
        }
        
        project.task('installFeature', type: InstallFeatureTask) {
            description 'Install a new feature to the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
        }
    }

}
