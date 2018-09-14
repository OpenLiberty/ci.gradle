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

import net.wasdev.wlp.gradle.plugins.tasks.InstallSpringBootApp
import org.gradle.api.Project

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
import net.wasdev.wlp.gradle.plugins.tasks.CompileJSPTask
import net.wasdev.wlp.gradle.plugins.tasks.extensions.arquillian.ConfigureArquillianTask

class LibertyTaskFactory {
    Project project
    LibertyTaskFactory(Project project) {
        this.project = project
    }

    void createTasks() {
        project.tasks.create('compileJSP', CompileJSPTask)
        project.tasks.create('installLiberty', InstallLibertyTask)
        project.tasks.create('libertyRun', RunTask)
        project.tasks.create('libertyStatus', StatusTask)
        project.tasks.create('libertyCreate', CreateTask)
        project.tasks.create('libertyStart', StartTask)
        project.tasks.create('libertyStop', StopTask)
        project.tasks.create('libertyPackage', PackageTask)
        project.tasks.create('libertyDump', DumpTask)
        project.tasks.create('libertyJavaDump', JavaDumpTask)
        project.tasks.create('libertyDebug', DebugTask)
        project.tasks.create('deploy', DeployTask)
        project.tasks.create('undeploy', UndeployTask)
        project.tasks.create('installFeature', InstallFeatureTask)
        project.tasks.create('uninstallFeature', UninstallFeatureTask)
        project.tasks.create('cleanDirs', CleanTask)
        project.tasks.create('installApps', InstallAppsTask)
        project.tasks.create('installSpringBootApp', InstallSpringBootApp)
        project.tasks.create('configureArquillian', ConfigureArquillianTask)
    }
}
