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
package net.wasdev.wlp.gradle.plugins

import org.gradle.api.*

import net.wasdev.wlp.gradle.plugins.extensions.LibertyExtension
import net.wasdev.wlp.gradle.plugins.tasks.StartTask
import net.wasdev.wlp.gradle.plugins.tasks.StopTask
import net.wasdev.wlp.gradle.plugins.tasks.StatusTask
import net.wasdev.wlp.gradle.plugins.tasks.CreateTask
import net.wasdev.wlp.gradle.plugins.tasks.RunTask
import net.wasdev.wlp.gradle.plugins.tasks.PackageTask
import net.wasdev.wlp.gradle.plugins.tasks.DumpTask
import net.wasdev.wlp.gradle.plugins.tasks.JavaDumpTask
import net.wasdev.wlp.gradle.plugins.tasks.DebugTask
import net.wasdev.wlp.gradle.plugins.tasks.DeployTask
import net.wasdev.wlp.gradle.plugins.tasks.UndeployTask
import net.wasdev.wlp.gradle.plugins.tasks.InstallFeatureTask
import net.wasdev.wlp.gradle.plugins.tasks.InstallLibertyTask
import net.wasdev.wlp.gradle.plugins.tasks.UninstallFeatureTask
import net.wasdev.wlp.gradle.plugins.tasks.CleanTask
import net.wasdev.wlp.gradle.plugins.tasks.InstallAppsTask

import org.gradle.api.logging.LogLevel

class Liberty implements Plugin<Project> {

    void apply(Project project) {
        
        project.extensions.create('liberty', LibertyExtension)
        project.configurations.create('libertyLicense')

        project.task('installLiberty', type: InstallLibertyTask) {
            description 'Installs Liberty from a repository'
            logging.level = LogLevel.INFO
        }

        project.task('libertyRun', type: RunTask, dependsOn: 'libertyCreate') {
            description = "Runs a Websphere Liberty Profile server under the Gradle process."
            logging.level = LogLevel.INFO
        }

        project.task('libertyStatus', type: StatusTask, dependsOn: 'libertyCreate') {
            description 'Checks if the Liberty server is running.'
            logging.level = LogLevel.INFO
        }

        project.task('libertyCreate', type: CreateTask, dependsOn: 'installLiberty') {
            description 'Creates a WebSphere Liberty Profile server.'
            outputs.file { new File(getUserDir(project), "servers/${project.liberty.serverName}/server.xml") }
            logging.level = LogLevel.INFO
        }

        project.task('libertyStart', type: StartTask, dependsOn: 'libertyCreate') {
            description 'Starts the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
        }

        project.task('libertyStop', type: StopTask) {
            description 'Stops the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
        }

        project.task('libertyPackage', type: PackageTask, dependsOn: 'libertyCreate') {
            description 'Generates a WebSphere Liberty Profile server archive.'
            logging.level = LogLevel.DEBUG
        }

        project.task('libertyDump', type: DumpTask) {
            description 'Dumps diagnostic information from the Liberty Profile server into an archive.'
            logging.level = LogLevel.INFO
        }

        project.task('libertyJavaDump', type: JavaDumpTask) {
            description 'Dumps diagnostic information from the Liberty Profile server JVM.'
            logging.level = LogLevel.INFO
        }

        project.task('libertyDebug', type: DebugTask, dependsOn: 'libertyCreate') {
            description 'Runs the Liberty Profile server in the console foreground after a debugger connects to the debug port (default: 7777).'
            logging.level = LogLevel.INFO
        }

        project.task('deploy', type: DeployTask) {
            description 'Deploys a supported file to the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
        }

        project.task('undeploy', type: UndeployTask) {
            description 'Removes an application from the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
        }

        project.task('installFeature', type: InstallFeatureTask, dependsOn: 'installLiberty') {
            description 'Install a new feature to the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
        }
        project.task('uninstallFeature', type: UninstallFeatureTask, dependsOn: 'installLiberty') {
            description 'Uninstall a feature from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
        }

        project.task('cleanDirs', type: CleanTask) {
            description 'Deletes files from some directories from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
        }
        
        project.task('installApps', type: InstallAppsTask, dependsOn: 'libertyCreate') {
            description "Copy applications generated by the Gradle project to a Liberty server's dropins or apps directory."
            logging.level = LogLevel.INFO
        }
    }

}
