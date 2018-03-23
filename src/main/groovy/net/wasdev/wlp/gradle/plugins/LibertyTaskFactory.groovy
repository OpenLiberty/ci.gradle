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
