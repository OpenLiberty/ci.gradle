package net.wasdev.wlp.gradle.plugins

import org.gradle.api.Project

class LibertyTaskFactory {
    Project project
    LibertyTaskFactory(Project project) {
        this.project = project
    }

    void createTasks() {
        project.task('compileJSP')
        project.task('installLiberty')
        project.task('libertyRun')
        project.task('libertyStatus')
        project.task('libertyCreate')
        project.task('libertyStart')
        project.task('libertyStop')
        project.task('libertyPackage')
        project.task('libertyDump')
        project.task('libertyJavaDump')
        project.task('libertyDebug')
        project.task('deploy')
        project.task('undeploy')
        project.task('installFeature')
        project.task('uninstallFeature')
        project.task('cleanDirs')
        project.task('installApps')
        project.task('configureArquillian')
    }
}
