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
package net.wasdev.wlp.gradle.plugins.tasks

import net.wasdev.wlp.ant.DeployTask
import net.wasdev.wlp.gradle.plugins.extensions.DeployExtension
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import static net.wasdev.wlp.gradle.plugins.Liberty.LIBERTY_DEPLOY_CONFIGURATION

/**
 * InstallAppsArchiveConfigTask
 * InstallAppsArchiveLibertyBlockTask
 * InstallAppsArchiveLocalTask
 * InstallAppsLooseConfigTask
 * InstallAppsLooseLibertyBlockTask
 * InstallAppsLooseLocalTask
 */
trait DeployBase {
    def initAntTask(Project project){

        project.ant.taskdef(name: 'deploy',
            classname: DeployTask.name,
            classpath: project.rootProject.buildscript.configurations.classpath.asPath)
    }
}

class DeployConfigTask extends AbstractInstallAppsTask implements DeployBase {
    @TaskAction
    void deploy() {
        initAntTask(project)

        // Deploys from the subproject configuration
        def deployConf = project.configurations.findByName(LIBERTY_DEPLOY_CONFIGURATION)
        def deployArtifacts = deployConf.incoming.resolutionResult.allDependencies as List
        def artifacts = deployConf.resolvedConfiguration.resolvedArtifacts as List
        artifacts.each {
            def params = buildLibertyMap(project)
            params.put('file', it.file.absolutePath)
            project.ant.deploy(params)
        }
    }
}

class DeployLibertyBlockTask extends AbstractInstallAppsTask implements DeployBase {
    @TaskAction
    void deploy() {
        initAntTask(project)

        // deploys the list of deploy closures
        for (DeployExtension deployable :  server.deploys) {
            def params = buildLibertyMap(project)

            def fileToDeploy = deployable.file
            println ("Deploying file: ${fileToDeploy}")

            if (fileToDeploy != null) {
                params.put('file', fileToDeploy)
                project.ant.deploy(params)
            } else {
                println("2")
                def deployDir = deployable.dir
                def include = deployable.include
                def exclude = deployable.exclude

                if (deployDir != null) {
                    println("3")
                    project.ant.deploy(params) {
                        fileset(dir:deployDir, includes: include, excludes: exclude)
                    }
                }
            }
        }
    }
}

class DeployLocalTask extends AbstractInstallAppsTask implements DeployBase {

    @TaskAction
    void deploy() {
        initAntTask(project)

        // Deploys war or ear from current project
        if (project.plugins.hasPlugin("war")) {
            def params = buildLibertyMap(project)
            def warFile = project.war.archivePath
            if (warFile.exists()) {
                params.put('file', warFile)
                project.ant.deploy(params)
            }
        }

        if (project.plugins.hasPlugin("ear")) {
            def params = buildLibertyMap(project)
            def earFile = project.ear.archivePath
            if (earFile.exists()) {
                params.put('file', earFile)
                project.ant.deploy(params)
            }
        }
    }
}
