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

import net.wasdev.wlp.gradle.plugins.extension.LibertyExtension
import net.wasdev.wlp.gradle.plugins.tasks.*

import org.gradle.api.*
import org.gradle.api.logging.LogLevel

class Liberty implements Plugin<Project> {

    void apply(Project project) {

        project.plugins.apply 'war'

        project.extensions.create('liberty', LibertyExtension)

        project.task('deployWar', type: DeployWarTask){
            description 'Deploys a WAR file to the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
		}

        project.task('installFeature', type: InstallFeatureTask){
            description 'Installs a new feature to the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
		}

        project.task('libertyCreate', type: LibertyCreateTask){
            description 'Creates a WebSphere Liberty Profile server.'
            outputs.file { new File(getUserDir(project), "servers/${project.liberty.serverName}/server.xml") }
            logging.level = LogLevel.INFO
		}

        project.task('libertyPackage', type: LibertyPackageTask){
            description 'Generates a WebSphere Liberty Profile server archive.'
            logging.level = LogLevel.INFO
		}

        project.task('libertyRun', type: LibertyRunTask){
            description 'Runs a WebSphere Liberty Profile server under the Gradle process.'
            logging.level = LogLevel.INFO
		}

        project.task('libertyStart', type: LibertyStartTask){
            description 'Starts the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
		}

        project.task('libertyStatus', type: LibertyStatusTask){
            description 'Checks the current status of the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
		}

        project.task('libertyStop', type: LibertyStopTask){
            description 'Stops the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
		}

        project.task('undeployWar', type: UndeployWarTask){
            description 'Removes a WAR file from the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
		}
    }
}
