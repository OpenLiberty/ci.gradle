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
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.bundling.War

class LibertyMultiServerTasks extends LibertyTasks {
    LibertyMultiServerTasks(Project project) {
        super(project)
    }

    void applyTasks() {
        addTaskRules()

        project.tasks.getByName('compileJSP') {
            description 'Compile the JSP files in the src/main/webapp directory. '
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('compileJSP')
        }

        overwriteTask('installLiberty', InstallLibertyTask, {
            description 'Installs Liberty from a repository'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        project.tasks.getByName('libertyRun') {
            description = "Runs a Websphere Liberty Profile server under the Gradle process."
            logging.level = LogLevel.INFO
            group 'Liberty'
            doLast {
                logger.warn('Please specify a server to run. Use the command \'libertyRun-<Server Name>\'.')
            }
        }

        project.tasks.getByName('libertyStatus') {
            description 'Checks if the Liberty server is running.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('libertyStatus')
        }

        project.tasks.getByName('libertyCreate') {
            description 'Creates a WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'

            List<String> libertyCreateTasks = getTaskList('libertyCreate')
            dependsOn 'installLiberty', libertyCreateTasks

            outputs.upToDateWhen {
                tasksUpToDate(libertyCreateTasks)
            }
        }

        project.tasks.getByName('libertyStart') {
            description 'Starts the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('libertyStart')
        }

        project.tasks.getByName('libertyStop') {
            description 'Stops the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('libertyStop')
        }

        project.tasks.getByName('libertyPackage') {
            description 'Generates a WebSphere Liberty Profile server archive.'
            logging.level = LogLevel.DEBUG
            group 'Liberty'
            dependsOn getTaskList('libertyPackage')
        }

        project.tasks.getByName('libertyDump') {
            description 'Dumps diagnostic information from the Liberty Profile server into an archive.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('libertyDump')
        }

        project.tasks.getByName('libertyJavaDump') {
            description 'Dumps diagnostic information from the Liberty Profile server JVM.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('libertyJavaDump')
        }

        project.tasks.getByName('libertyDebug') {
            description 'Runs the Liberty Profile server in the console foreground after a debugger connects to the debug port (default: 7777).'
            logging.level = LogLevel.INFO
            group 'Liberty'
            doLast {
                logger.warn('Please specify a server to debug. Use the command \'libertyDebug-<Server Name>\'.')
            }
        }

        project.tasks.getByName('deploy') {
            description 'Deploys a supported file to the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('deploy')
        }

        project.tasks.getByName('undeploy') {
            description 'Removes an application from the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('undeploy')
        }

        project.tasks.getByName('installFeature') {
            description 'Install a new feature to the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('installFeature')
        }

        project.tasks.getByName('uninstallFeature') {
            description 'Uninstall a feature from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('uninstallFeature')
        }

        project.tasks.getByName('cleanDirs') {
            description 'Deletes files from some directories from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList('cleanDirs')
        }

        project.tasks.getByName('installApps') {
            description "Copy applications generated by the Gradle project to a Liberty server's dropins or apps directory."
            logging.level = LogLevel.INFO
            group 'Liberty'
            List<String> installAppsTasks = getTaskList('installApps')
            dependsOn project.tasks.withType(War), installAppsTasks
            outputs.upToDateWhen {
                tasksUpToDate(installAppsTasks)
            }
        }

        project.liberty.servers.each { checkServerEnvProperties(it) }
    }

    void addTaskRules() {
        addTaskRule('Pattern: libertyCreate-<Server Name>', 'libertyCreate', CreateTask, {
            description 'Creates a WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'installLiberty'

            if (dependsOnFeature(server)) finalizedBy 'installFeature-' + server.name
        })

        addTaskRule('Pattern: libertyStop-<Server Name>', 'libertyStop', StopTask, {
            description 'Stops the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        addTaskRule('Pattern: libertyStart-<Server Name>', 'libertyStart', StartTask, {
            description 'Starts the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyCreate-' + server.name

            if (dependsOnApps(server)) dependsOn 'installApps-' + server.name
        })

        addTaskRule('Pattern: libertyRun-<Server Name>', 'libertyRun', RunTask, {
            description 'Runs a Websphere Liberty Profile server under the Gradle process.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyCreate-' + server.name

            if (dependsOnApps(server)) dependsOn 'installApps-' + server.name
        })

        addTaskRule('Pattern: installApps-<Server Name>', 'installApps', InstallAppsTask, {
            dependsOn 'libertyCreate-' + server.name, project.tasks.withType(War)

            description "Copy applications generated by the Gradle project to a Liberty server's dropins or apps directory."
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        addTaskRule('Pattern: installFeature-<Server Name>', 'installFeature', InstallFeatureTask, {
            description 'Install a new feature to the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'

            if (dependsOnFeature(server)) {
                dependsOn 'libertyCreate-' + server.name
            } else {
                dependsOn 'installLiberty'
            }
        })

        addTaskRule('Pattern: uninstallFeature-<Server Name>', 'uninstallFeature', UninstallFeatureTask, {
            description 'Uninstall a feature from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        addTaskRule('Pattern: compileJSP-<Server Name>', 'compileJSP', CompileJSPTask, {
            description 'Compile the JSP files in the src/main/webapp directory. '
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'installLiberty', 'compileJava'
        })

        addTaskRule('Pattern: libertyRun-<Server Name>', 'libertyRun', RunTask, {
            description = "Runs a Websphere Liberty Profile server under the Gradle process."
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyCreate-' + server.name
            if (dependsOnApps(server)) dependsOn 'installApps-' + server.name
        })

        addTaskRule('Pattern: libertyStatus-<Server Name>', 'libertyStatus', StatusTask, {
            description 'Checks if the Liberty server is running.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        addTaskRule('Pattern: libertyDebug-<Server Name>', 'libertyDebug', DebugTask, {
            description 'Runs the Liberty Profile server in the console foreground after a debugger connects to the debug port (default: 7777).'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyCreate-' + server.name
        })

        addTaskRule('Pattern: libertyPackage-<Server Name>', 'libertyPackage', PackageTask, {
            description 'Generates a WebSphere Liberty Profile server archive.'
            logging.level = LogLevel.DEBUG
            group 'Liberty'
            dependsOn installDependsOn(server, 'libertyCreate')
        })

        addTaskRule('Pattern: libertyDump-<Server Name>', 'libertyDump', DumpTask, {
            description 'Dumps diagnostic information from the Liberty Profile server into an archive.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        addTaskRule('Pattern: libertyJavaDump-<Server Name>', 'libertyJavaDump', JavaDumpTask, {
            description 'Dumps diagnostic information from the Liberty Profile server JVM.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        addTaskRule('Pattern: deploy-<Server Name>', 'deploy', DeployTask, {
            description 'Deploys a supported file to the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyStart-' + server.name
        })

        addTaskRule('Pattern: undeploy-<Server Name>', 'undeploy', UndeployTask, {
            description 'Removes an application from the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyStart-' + server.name
        })

        addTaskRule('Pattern: cleanDirs-<Server Name>', 'cleanDirs', CleanTask, {
            description 'Deletes files from some directories from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        addTaskRule('configureArquillian', ConfigureArquillianTask, {
            description "Automatically generates arquillian.xml for projects that use Arquillian Liberty Managed or Remote containers."
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyCreate', 'processTestResources'
            skipIfArquillianXmlExists = project.configureArquillian.skipIfArquillianXmlExists
            arquillianProperties = project.configureArquillian.arquillianProperties
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
        taskList.each {
            if(!project.tasks.getByName(it).getUpToDate()){
                return false
            }
        }
        return true
    }

    private List<String> getTaskList (String taskName) {
        List<String> tasks = new ArrayList<String>()
        project.liberty.servers.each {
            tasks.add(taskName + '-' + it.name)
        }
        return tasks
    }
}
