/**
 * (C) Copyright IBM Corporation 2014, 2018.
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
import net.wasdev.wlp.gradle.plugins.tasks.extensions.arquillian.ArquillianExtension

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.GradleException

import java.util.Properties

class Liberty implements Plugin<Project> {

    final String JST_WEB_FACET_VERSION = '3.0'
    final String JST_EAR_FACET_VERSION = '6.0'

    void apply(Project project) {
        project.extensions.create('liberty', LibertyExtension)
        project.extensions.create('arquillianConfiguration', ArquillianExtension)

        project.liberty.servers = project.container(ServerExtension)

        project.configurations.create('libertyLicense')
        project.configurations.create('libertyRuntime')
        project.configurations.create('libertyFeature')
        if (project.configurations.find { it.name == 'compileOnly' }) {
            project.configurations.libertyFeature.extendsFrom(project.configurations.compileOnly)
        }

        //Used to set project facets in Eclipse
        project.pluginManager.apply('eclipse-wtp')
        project.tasks.getByName('eclipseWtpFacet').finalizedBy 'libertyCreate'

        new LibertyTaskFactory(project).createTasks()

        //Create expected server extension from liberty extension data
        project.afterEvaluate {
            setEclipseFacets(project)
            if (isSingleServerProject(project)) {
                new LibertySingleServerTasks(project).applyTasks()
            } else if (isMultiServerProject(project)) {
                new LibertyMultiServerTasks(project).applyTasks()
            } else if (project.liberty.server != null && !project.liberty.servers.isEmpty()){
                throw new GradleException('Both a \'server\' and \'servers\' extension were found in a build.gradle file that uses the liberty plugin. Please define multiple servers inside of the Liberty \'servers\' extension in your build.gradle file.')
            }
            if (project.liberty.server == null && project.liberty.servers.isEmpty()) {
                project.liberty.server = copyProperties(project.liberty)
                new LibertySingleServerTasks(project).applyTasks()
            }
            //Checking serverEnv files for server properties
            Liberty.checkEtcServerEnvProperties(project)
        }
    }

    private void setEclipseFacets(Project project) {
        //Uplift the jst.web facet version to 3.0 if less than 3.0 so WDT can deploy properly to Liberty.
        //There is a known bug in the wtp plugin that will add duplicate facets, the first of the duplicates is honored.
        if(project.plugins.hasPlugin('war')) {
            setFacetVersion(project, 'jst.web', JST_WEB_FACET_VERSION)
        }

        if (project.plugins.hasPlugin('ear')) {
            setFacetVersion(project, 'jst.ear', JST_EAR_FACET_VERSION)
            project.getGradle().getTaskGraph().whenReady {
                Dependency[] deps = project.configurations.deploy.getAllDependencies().toArray()
                deps.each { Dependency dep ->
                    if (dep instanceof ProjectDependency) {
                        def projectDep = dep.getDependencyProject()
                        if (projectDep.plugins.hasPlugin('war')) {
                            setFacetVersion(projectDep, 'jst.web', JST_WEB_FACET_VERSION)
                        }
                    }
                }
            }
        }
    }

    protected void setFacetVersion(Project project, String facetName, String version) {
        if(project.plugins.hasPlugin('eclipse-wtp')) {
            project.tasks.getByName('eclipseWtpFacet').facet.file.whenMerged {
                def jstFacet = facets.find { it.type.name() == 'installed' && it.name == facetName && Double.parseDouble(it.version) < Double.parseDouble(version) }
                if (jstFacet != null) {
                    jstFacet.version = version
                }
            }
        }
    }

    private ServerExtension copyProperties(LibertyExtension liberty) {
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
        serverMap.remove('outputDir')

        return ServerExtension.newInstance(serverMap)
    }

    public static void checkEtcServerEnvProperties(Project project) {
        if (project.liberty.outputDir == null) {
            Properties envProperties = new Properties()
            //check etc/server.env and set liberty.outputDir
            File serverEnvFile = new File(Liberty.getInstallDir(project), 'etc/server.env')
            if (serverEnvFile.exists()) {
                serverEnvFile.text = serverEnvFile.text.replace("\\", "/")
                envProperties.load(new FileInputStream(serverEnvFile))
                Liberty.setLibertyOutputDir(project, (String) envProperties.get("WLP_OUTPUT_DIR"))
            }
        }
    }

    private static void setLibertyOutputDir(Project project, String envOutputDir){
        if (envOutputDir != null) {
            project.liberty.outputDir = envOutputDir
        }
    }

    private static File getInstallDir(Project project) {
        if (project.liberty.installDir == null) {
           if (project.liberty.install.baseDir == null) {
               return new File(project.buildDir, 'wlp')
           } else {
               return new File(project.liberty.install.baseDir, 'wlp')
           }
        } else {
           return new File(project.liberty.installDir)
        }
    }

    boolean isSingleServerProject(Project project) {
        if (project.liberty.server != null && project.liberty.servers.isEmpty()) {
            return true
        }
        return false
    }

    boolean isMultiServerProject(Project project) {
        if (!project.liberty.servers.isEmpty() && project.liberty.server == null) {
            return true
        }
        return false
    }
}
