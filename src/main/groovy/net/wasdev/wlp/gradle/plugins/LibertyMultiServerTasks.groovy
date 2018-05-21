
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
import net.wasdev.wlp.gradle.plugins.tasks.CompileJSPTask
import net.wasdev.wlp.gradle.plugins.tasks.extensions.arquillian.ConfigureArquillianTask

import org.gradle.api.Project
import org.gradle.api.DefaultTask
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.bundling.War

class LibertyMultiServerTasks extends LibertyTasks {
    LibertyMultiServerTasks(Project project) {
        super(project)
    }

    void applyTasks() {
        addTaskRules()

        overwriteTask('compileJSP', DefaultTask, {
            description 'Compile the JSP files in the src/main/webapp directory. '
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('compileJSP')
        })

        overwriteTask('libertyRun', DefaultTask, {
            description = "Runs a Liberty server under the Gradle process."
            logging.level = LogLevel.INFO
            group 'Liberty'
            doLast {
                logger.warn('Please specify a server to run. Use the command \'libertyRun-<Server Name>\'.')
            }
        })

        overwriteTask('libertyStatus', DefaultTask, {
            description 'Checks if the Liberty server is running.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('libertyStatus')
        })

        overwriteTask('libertyCreate', DefaultTask, {
            description 'Creates a Liberty server.'
            logging.level = LogLevel.INFO
            group 'Liberty'

            List<String> libertyCreateTasks = getTaskList('libertyCreate')
            dependsOn 'installLiberty', libertyCreateTasks

            outputs.upToDateWhen {
                tasksUpToDate(libertyCreateTasks)
            }
        })

        overwriteTask('libertyStart', DefaultTask, {
            description 'Starts the Liberty server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('libertyStart')
        })

        overwriteTask('libertyStop', DefaultTask, {
            description 'Stops the Liberty server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('libertyStop')
        })

        overwriteTask('libertyPackage', DefaultTask, {
            description 'Generates a Liberty server archive.'
            logging.level = LogLevel.DEBUG
            group 'Liberty'
            dependsOn getTaskList('libertyPackage')
        })

        overwriteTask('libertyDump', DefaultTask, {
            description 'Dumps diagnostic information from the Liberty server into an archive.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('libertyDump')
        })

        overwriteTask('libertyJavaDump', DefaultTask, {
            description 'Dumps diagnostic information from the Liberty server JVM.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('libertyJavaDump')
        })

        overwriteTask('libertyDebug', DefaultTask, {
            description 'Runs the Liberty server in the console foreground after a debugger connects to the debug port (default: 7777).'
            logging.level = LogLevel.INFO
            group 'Liberty'
            doLast {
                logger.warn('Please specify a server to debug. Use the command \'libertyDebug-<Server Name>\'.')
            }
        })

        overwriteTask('deploy', DefaultTask, {
            description 'Deploys a supported file to the Liberty server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('deploy')
        })

        overwriteTask('undeploy', DefaultTask, {
            description 'Removes an application from the Liberty server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('undeploy')
        })

        overwriteTask('installFeature', DefaultTask, {
            description 'Install a new feature to the Liberty server'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('installFeature')
        })

        overwriteTask('uninstallFeature', DefaultTask, {
            description 'Uninstall a feature from the Liberty server'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('uninstallFeature')
        })

        overwriteTask('cleanDirs', DefaultTask, {
            description 'Deletes files from some directories from the Liberty server'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('cleanDirs')
        })

        overwriteTask('installApps', DefaultTask, {
            description "Copy applications generated by the Gradle project to a Liberty server's dropins or apps directory."
            logging.level = LogLevel.INFO
            group 'Liberty'
            List<String> installAppsTasks = getTaskList('installApps')
            dependsOn project.tasks.withType(War), installAppsTasks
            outputs.upToDateWhen {
                tasksUpToDate(installAppsTasks)
            }
        })

        project.liberty.servers.each { checkServerEnvProperties(it) }
    }

    void addTaskRules() {
        addTaskRule('Pattern: libertyCreate-<Server Name>', 'libertyCreate', CreateTask, {
            dependsOn 'installLiberty'

            if (dependsOnFeature(server)) finalizedBy 'installFeature-' + server.name
        })

        addTaskRule('Pattern: libertyStop-<Server Name>', 'libertyStop', StopTask, {})

        addTaskRule('Pattern: libertyStart-<Server Name>', 'libertyStart', StartTask, {
            dependsOn 'libertyCreate-' + server.name

            if (dependsOnApps(server)) dependsOn 'installApps-' + server.name
        })

        addTaskRule('Pattern: libertyRun-<Server Name>', 'libertyRun', RunTask, {
            dependsOn 'libertyCreate-' + server.name

            if (dependsOnApps(server)) dependsOn 'installApps-' + server.name
        })

        addTaskRule('Pattern: installApps-<Server Name>', 'installApps', InstallAppsTask, {
            dependsOn 'libertyCreate-' + server.name, project.tasks.withType(War)
        })

        addTaskRule('Pattern: installFeature-<Server Name>', 'installFeature', InstallFeatureTask, {
            if (dependsOnFeature(server)) {
                dependsOn 'libertyCreate-' + server.name
            } else {
                dependsOn 'installLiberty'
            }
        })

        addTaskRule('Pattern: uninstallFeature-<Server Name>', 'uninstallFeature', UninstallFeatureTask, {})

        addTaskRule('Pattern: compileJSP-<Server Name>', 'compileJSP', CompileJSPTask, {
            dependsOn 'installLiberty', 'compileJava'
        })

        addTaskRule('Pattern: libertyRun-<Server Name>', 'libertyRun', RunTask, {
            dependsOn 'libertyCreate-' + server.name
            if (dependsOnApps(server)) dependsOn 'installApps-' + server.name
        })

        addTaskRule('Pattern: libertyStatus-<Server Name>', 'libertyStatus', StatusTask, {})

        addTaskRule('Pattern: libertyDebug-<Server Name>', 'libertyDebug', DebugTask, {
            dependsOn 'libertyCreate-' + server.name
        })

        addTaskRule('Pattern: libertyPackage-<Server Name>', 'libertyPackage', PackageTask, {
            dependsOn installDependsOn(server, 'libertyCreate')
        })

        addTaskRule('Pattern: libertyDump-<Server Name>', 'libertyDump', DumpTask, {})

        addTaskRule('Pattern: libertyJavaDump-<Server Name>', 'libertyJavaDump', JavaDumpTask, {})

        addTaskRule('Pattern: deploy-<Server Name>', 'deploy', DeployTask, {
            dependsOn 'libertyStart-' + server.name
        })

        addTaskRule('Pattern: undeploy-<Server Name>', 'undeploy', UndeployTask, {
            dependsOn 'libertyStart-' + server.name
        })

        addTaskRule('Pattern: cleanDirs-<Server Name>', 'cleanDirs', CleanTask, {})

        addTaskRule('Pattern: configureArquillian-<Server Name>', 'configureArquillian', ConfigureArquillianTask, {
            dependsOn 'installApps-' + server.name, 'processTestResources'
            skipIfArquillianXmlExists = project.arquillianConfiguration.skipIfArquillianXmlExists
            arquillianProperties = project.arquillianConfiguration.arquillianProperties
        })
    }

    void addTaskRule (String pattern, String name, Class taskType, Closure configureClosure) {
        project.tasks.addRule(pattern) { String taskName ->
            if (taskName.startsWith(name)) {
                project.task(taskName, type: taskType, overwrite:true) {
                    server = project.liberty.servers.getByName(taskName - "${name}-")
                }.configure(configureClosure)
            }
        }
    }

    boolean tasksUpToDate (List<String> taskList) {
        boolean allUpToDate = true
        taskList.each {
            if(!project.tasks.getByName(it).getUpToDate()){
                allUpToDate = false
            }
        }
        return allUpToDate
    }

    private List<String> getTaskList (String taskName) {
        List<String> tasks = new ArrayList<String>()
        project.liberty.servers.each {
            tasks.add(taskName + '-' + it.name)
        }
        return tasks
    }
}
