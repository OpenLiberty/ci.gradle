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

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.bundling.War

class LibertySingleServerTasks extends LibertyTasks {

    LibertySingleServerTasks(Project project) {
        super(project)
    }

    void applyTasks() {
        project.tasks.getByName('compileJSP'){
            type: CompileJSPTask
            description 'Compile the JSP files in the src/main/webapp directory. '
            logging.level = LogLevel.INFO
            dependsOn 'installLiberty', 'compileJava'
            group 'Liberty'
        }

        // project.task('compileJSP', type: CompileJSPTask, overwrite: true)
        // {
        //     type: CompileJSPTask
        //     description 'Compile the JSP files in the src/main/webapp directory. '
        //     logging.level = LogLevel.INFO
        //     dependsOn 'installLiberty', 'compileJava'
        //     group 'Liberty'
        // }
        project.tasks.getByName('installLiberty') {
            type: InstallLibertyTask
            description 'Installs Liberty from a repository'
            logging.level = LogLevel.INFO
            group 'Liberty'
        }
        // project.task('installLiberty', type: InstallLibertyTask, overwrite: true) {
        //     description 'Installs Liberty from a repository'
        //     logging.level = LogLevel.INFO
        //     group 'Liberty'
        // }
        project.tasks.getByName('libertyRun') {
            type: RunTask
            description = "Runs a Websphere Liberty Profile server under the Gradle process."
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyCreate'

            if (dependsOnApps(project.liberty.server)) dependsOn 'installApps'
        }
        // project.task('libertyRun', type: RunTask, overwrite: true) {
        //     description = "Runs a Websphere Liberty Profile server under the Gradle process."
        //     logging.level = LogLevel.INFO
        //     group 'Liberty'
        //     dependsOn 'libertyCreate'
        //
        //     if (dependsOnApps(project.liberty.server)) dependsOn 'installApps'
        // }
        project.tasks.getByName('libertyStatus') {
            type: StatusTask
            description 'Checks if the Liberty server is running.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyCreate'
        }
        // project.task('libertyStatus', type: StatusTask, overwrite: true) {
        //     description 'Checks if the Liberty server is running.'
        //     logging.level = LogLevel.INFO
        //     group 'Liberty'
        //     dependsOn 'libertyCreate'
        // }
        project.tasks.getByName('libertyCreate') {
            type: CreateTask
            description 'Creates a WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'installLiberty'

            // Run install features if configured
            if (dependsOnFeature(project.liberty.server)) finalizedBy 'installFeature'
        }
        // project.task('libertyCreate', type: CreateTask, overwrite: true) {
        //     description 'Creates a WebSphere Liberty Profile server.'
        //     logging.level = LogLevel.INFO
        //     group 'Liberty'
        //     dependsOn 'installLiberty'
        //
        //     // Run install features if configured
        //     if (dependsOnFeature(project.liberty.server)) finalizedBy 'installFeature'
        // }
        project.tasks.getByName('libertyStart') {
            type: StartTask
            description 'Starts the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyCreate'

            if (dependsOnApps(project.liberty.server)) dependsOn 'installApps'
        }
        // project.task('libertyStart', type: StartTask, overwrite: true) {
        //     description 'Starts the WebSphere Liberty Profile server.'
        //     logging.level = LogLevel.INFO
        //     group 'Liberty'
        //     dependsOn 'libertyCreate'
        //
        //     if (dependsOnApps(project.liberty.server)) dependsOn 'installApps'
        // }
        project.tasks.getByName('libertyStop') {
            type: StopTask
            description 'Stops the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        }
        // project.task('libertyStop', type: StopTask, overwrite: true) {
        //     description 'Stops the WebSphere Liberty Profile server.'
        //     logging.level = LogLevel.INFO
        //     group 'Liberty'
        // }
        project.tasks.getByName('libertyPackage') {
            type: PackageTask
            description 'Generates a WebSphere Liberty Profile server archive.'
            logging.level = LogLevel.DEBUG
            group 'Liberty'

            dependsOn installDependsOn(project.liberty.server, 'installLiberty')
        }
        // project.task('libertyPackage', type: PackageTask, overwrite: true) {
        //     description 'Generates a WebSphere Liberty Profile server archive.'
        //     logging.level = LogLevel.DEBUG
        //     group 'Liberty'
        //
        //     dependsOn installDependsOn(project.liberty.server, 'installLiberty')
        // }
        project.tasks.getByName('libertyDump') {
            type: DumpTask
            description 'Dumps diagnostic information from the Liberty Profile server into an archive.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        }
        // project.task('libertyDump', type: DumpTask, overwrite: true) {
        //     description 'Dumps diagnostic information from the Liberty Profile server into an archive.'
        //     logging.level = LogLevel.INFO
        //     group 'Liberty'
        // }
        project.tasks.getByName('libertyJavaDump') {
            type: JavaDumpTask
            description 'Dumps diagnostic information from the Liberty Profile server JVM.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        }
        // project.task('libertyJavaDump', type: JavaDumpTask, overwrite: true) {
        //     description 'Dumps diagnostic information from the Liberty Profile server JVM.'
        //     logging.level = LogLevel.INFO
        //     group 'Liberty'
        // }
        project.tasks.getByName('libertyDebug') {
            type: DebugTask
            description 'Runs the Liberty Profile server in the console foreground after a debugger connects to the debug port (default: 7777).'
            logging.level = LogLevel.INFO
            group 'Liberty'
        }
        // project.task('libertyDebug', type: DebugTask, overwrite: true) {
        //     description 'Runs the Liberty Profile server in the console foreground after a debugger connects to the debug port (default: 7777).'
        //     logging.level = LogLevel.INFO
        //     group 'Liberty'
        // }
        project.tasks.getByName('deploy') {
            type: DeployTask
            description 'Deploys a supported file to the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyStart'
        }
        // project.task('deploy', type: DeployTask, overwrite: true) {
        //     description 'Deploys a supported file to the WebSphere Liberty Profile server.'
        //     logging.level = LogLevel.INFO
        //     group 'Liberty'
        //     dependsOn 'libertyStart'
        // }
        project.tasks.getByName('undeploy') {
            type: UndeployTask
            description 'Removes an application from the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyStart'
        }
        // project.task('undeploy', type: UndeployTask, overwrite: true) {
        //      description 'Removes an application from the WebSphere Liberty Profile server.'
        //      logging.level = LogLevel.INFO
        //      group 'Liberty'
        //      dependsOn 'libertyStart'
        // }
        project.tasks.getByName('installFeature') {
            type: InstallFeatureTask
            description 'Install a new feature to the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'

            if (dependsOnFeature(project.liberty.server)) {
                dependsOn 'libertyCreate'
            } else {
                dependsOn 'installLiberty'
            }
        }
        // project.task('installFeature', type: InstallFeatureTask, overwrite: true) {
        //     description 'Install a new feature to the WebSphere Liberty Profile server'
        //     logging.level = LogLevel.INFO
        //     group 'Liberty'
        //
        //     if (dependsOnFeature(project.liberty.server)) {
        //         dependsOn 'libertyCreate'
        //     } else {
        //         dependsOn 'installLiberty'
        //     }
        // }
        project.tasks.getByName('uninstallFeature') {
            type: UninstallFeatureTask
            description 'Uninstall a feature from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'
        }
        // project.task('uninstallFeature', type: UninstallFeatureTask, overwrite: true) {
        //     description 'Uninstall a feature from the WebSphere Liberty Profile server'
        //     logging.level = LogLevel.INFO
        //     group 'Liberty'
        // }
        project.tasks.getByName('cleanDirs') {
            type: CleanTask
            description 'Deletes files from some directories from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyStop'
        }
        // project.task('cleanDirs', type: CleanTask, overwrite: true) {
        //     description 'Deletes files from some directories from the WebSphere Liberty Profile server'
        //     logging.level = LogLevel.INFO
        //     group 'Liberty'
        //     dependsOn 'libertyStop'
        // }
        project.tasks.getByName('installApps') {
            type: InstallAppsTask
            description "Copy applications generated by the Gradle project to a Liberty server's dropins or apps directory."
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn project.tasks.withType(War), 'libertyCreate'
        }

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
