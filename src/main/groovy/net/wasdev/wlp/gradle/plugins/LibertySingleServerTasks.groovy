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
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.bundling.War

class LibertySingleServerTasks extends LibertyTasks {

    LibertySingleServerTasks(Project project) {
        super(project)
    }

    void applyTasks() {
        overwriteTask('compileJSP', CompileJSPTask, {
            description 'Compile the JSP files in the src/main/webapp directory. '
            logging.level = LogLevel.INFO
            dependsOn 'installLiberty', 'compileJava'
            group 'Liberty'
        })

        overwriteTask('installLiberty', InstallLibertyTask, {
            description 'Installs Liberty from a repository'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        overwriteTask('libertyRun', RunTask, {
            description = "Runs a Websphere Liberty Profile server under the Gradle process."
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyCreate'

            if (dependsOnApps(project.liberty.server)) dependsOn 'installApps'
        })

        overwriteTask('libertyStatus', StatusTask, {
            description 'Checks if the Liberty server is running.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyCreate'
        })

        overwriteTask('libertyCreate', CreateTask, {
            description 'Creates a WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'installLiberty'

            // Run install features if configured
            if (dependsOnFeature(project.liberty.server)) finalizedBy 'installFeature'
        })

        overwriteTask('libertyStart', StartTask, {
            description 'Starts the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyCreate'

            if (dependsOnApps(project.liberty.server)) dependsOn 'installApps'
        })

        overwriteTask('libertyStop', StopTask, {
            description 'Stops the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        overwriteTask('libertyPackage', PackageTask, {
            description 'Generates a WebSphere Liberty Profile server archive.'
            logging.level = LogLevel.DEBUG
            group 'Liberty'

            dependsOn installDependsOn(project.liberty.server, 'libertyCreate')
        })

        overwriteTask('libertyDump', DumpTask, {
            description 'Dumps diagnostic information from the Liberty Profile server into an archive.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        overwriteTask('libertyJavaDump', JavaDumpTask, {
            description 'Dumps diagnostic information from the Liberty Profile server JVM.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        overwriteTask('libertyDebug', DebugTask, {
            description 'Runs the Liberty Profile server in the console foreground after a debugger connects to the debug port (default: 7777).'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        overwriteTask('deploy', DeployTask, {
            description 'Deploys a supported file to the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyStart'
        })

        overwriteTask('undeploy', UndeployTask, {
            description 'Removes an application from the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyStart'
        })

        overwriteTask('installFeature', InstallFeatureTask, {
            description 'Install a new feature to the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'

            if (dependsOnFeature(project.liberty.server)) {
                dependsOn 'libertyCreate'
            } else {
                dependsOn 'installLiberty'
            }
        })

        overwriteTask('uninstallFeature', UninstallFeatureTask, {
            description 'Uninstall a feature from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        overwriteTask('cleanDirs', CleanTask, {
            description 'Deletes files from some directories from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyStop'
        })

        overwriteTask('installApps', InstallAppsTask, {
            description "Copy applications generated by the Gradle project to a Liberty server's dropins or apps directory."
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn project.tasks.withType(War), 'libertyCreate'
        })

        overwriteTask('configureArquillian', ConfigureArquillianTask, {
            description "Automatically generates arquillian.xml for projects that use Arquillian Liberty Managed or Remote containers."
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyCreate', 'processTestResources'
            skipIfArquillianXmlExists = project.configureArquillian.skipIfArquillianXmlExists
            arquillianProperties = project.configureArquillian.arquillianProperties
        })

        if (!dependsOnApps(project.liberty.server)) {
            if (project.plugins.hasPlugin('war')) {
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
