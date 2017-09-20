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
import net.wasdev.wlp.gradle.plugins.extensions.ServerExtension
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
import net.wasdev.wlp.gradle.plugins.tasks.AbstractServerTask
import org.gradle.api.tasks.bundling.War

import org.gradle.api.logging.LogLevel

class Liberty implements Plugin<Project> {

    void apply(Project project) {

        project.extensions.create('liberty', LibertyExtension)
        project.configurations.create('libertyLicense')
        project.configurations.create('libertyRuntime')

        //Create expected server extension from liberty extension data
        project.afterEvaluate{
            project.liberty.server = project.liberty.server ?: copyProperties(project.liberty)

            //Server objects need to be set per task after the project configuration phase
            setServersForTasks(project)
        }

        project.task('installLiberty', type: InstallLibertyTask) {
            description 'Installs Liberty from a repository'
            logging.level = LogLevel.INFO

            project.afterEvaluate {
                outputs.dir getInstallDir(project)
            }
        }

        project.task('libertyRun', type: RunTask) {
            description = "Runs a Websphere Liberty Profile server under the Gradle process."
            logging.level = LogLevel.INFO
            dependsOn 'installApps'
        }

        project.task('libertyStatus', type: StatusTask) {
            description 'Checks if the Liberty server is running.'
            logging.level = LogLevel.INFO
            dependsOn 'libertyCreate'
        }

        project.task('libertyCreate', type: CreateTask) {
            description 'Creates a WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            dependsOn 'installLiberty'

            project.afterEvaluate{
                outputs.dir getServerDir(project)
                outputs.file new File(getServerDir(project), "server.xml")

                // @Input
                serverName = server.name

                // @Input configs
                configCollection = [project.liberty.configFile,
                                    project.liberty.bootstrapPropertiesFile,
                                    project.liberty.jvmOptionsFile,
                                    project.liberty.serverEnv]
                configCollection + (project.liberty.configDirectory ?: null)
            }
        }

        project.task('libertyStart', type: StartTask) {
            description 'Starts the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            dependsOn 'installApps'
        }

        project.task('libertyStop', type: StopTask) {
            description 'Stops the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
        }

        project.task('libertyPackage', type: PackageTask) {
            description 'Generates a WebSphere Liberty Profile server archive.'
            logging.level = LogLevel.DEBUG
            dependsOn 'installApps'

            project.afterEvaluate {
                outputs.file server.packageLiberty.archive ?: new File("null")
            }
        }

        project.task('libertyDump', type: DumpTask) {
            description 'Dumps diagnostic information from the Liberty Profile server into an archive.'
            logging.level = LogLevel.INFO
        }

        project.task('libertyJavaDump', type: JavaDumpTask) {
            description 'Dumps diagnostic information from the Liberty Profile server JVM.'
            logging.level = LogLevel.INFO
        }

        project.task('libertyDebug', type: DebugTask) {
            description 'Runs the Liberty Profile server in the console foreground after a debugger connects to the debug port (default: 7777).'
            logging.level = LogLevel.INFO
        }

        project.task('deploy', type: DeployTask) {
            description 'Deploys a supported file to the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            dependsOn 'libertyStart'
        }

        project.task('undeploy', type: UndeployTask) {
            description 'Removes an application from the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            dependsOn 'libertyStart'
        }

        project.task('installFeature', type: InstallFeatureTask) {
            description 'Install a new feature to the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO

            project.afterEvaluate {
                if (server.features.name != null && !server.features.name.empty) {
                    dependsOn 'libertyCreate'
                } else {
                    dependsOn 'installLiberty'
                }
            }
        }
        project.task('uninstallFeature', type: UninstallFeatureTask) {
            description 'Uninstall a feature from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            dependsOn 'libertyCreate'
        }

        project.task('cleanDirs', type: CleanTask) {
            description 'Deletes files from some directories from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO

            project.afterEvaluate {
                if (project.liberty.server.cleanDir.workarea || project.liberty.server.cleanDir.dropins) {
                    logger.info ('Stopping the server to clean workarea or dropins')
                    dependsOn 'libertyStop'
                }
            }
        }

        project.task('installApps', type: InstallAppsTask) {
            description "Copy applications generated by the Gradle project to a Liberty server's dropins or apps directory."
            logging.level = LogLevel.INFO
            dependsOn project.tasks.withType(War)

            project.afterEvaluate {
                inputs.property "apps", server.apps
                inputs.property "dropins", server.dropins
                outputs.dir new File(getServerDir(project), "apps")
                outputs.dir new File(getServerDir(project), "dropins")

                if (server.features.name != null && !server.features.name.empty) {
                    dependsOn 'installFeature'
                } else {
                    dependsOn 'libertyCreate'
                }
           }
        }
    }

    ServerExtension copyProperties(LibertyExtension liberty) {
        def serverMap = new ServerExtension().getProperties()
        def libertyMap = liberty.getProperties()

        serverMap.keySet().each { String element ->
            if (element.equals("name")) {
                serverMap.put(element, libertyMap.get("serverName"))
            }
            else {
                serverMap.put(element, libertyMap.get(element))
            }
        }
        serverMap.remove('class')

        return ServerExtension.newInstance(serverMap)
    }

    void setServersForTasks(Project project){
        project.tasks.withType(AbstractServerTask).each {task ->
            task.server = project.liberty.server
        }
    }

    String installAppsDependsOn(ServerExtension server, String elseDepends) {
        if (server.apps != null || server.dropins != null) {
            return 'installApps'
        } else {
            return elseDepends
        }
    }
}
