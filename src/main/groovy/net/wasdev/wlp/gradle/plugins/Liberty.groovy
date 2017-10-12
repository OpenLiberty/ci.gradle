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

import net.wasdev.wlp.gradle.plugins.definition.DefaultLibertyBaseSourceSet
import net.wasdev.wlp.gradle.plugins.definition.DefaultLibertyConfigSourceSet
import net.wasdev.wlp.gradle.plugins.definition.LibertyBaseSourceSet
import net.wasdev.wlp.gradle.plugins.definition.LibertyConfigSourceSet
import net.wasdev.wlp.gradle.plugins.tasks.CreateBootstrapTask
import net.wasdev.wlp.gradle.plugins.tasks.CreateConfigTask
import net.wasdev.wlp.gradle.plugins.tasks.CreateJvmOptionsTask
import net.wasdev.wlp.gradle.plugins.tasks.CreateServerEnvTask
import net.wasdev.wlp.gradle.plugins.tasks.CreateServerXmlTask
import org.gradle.api.*
import org.gradle.api.file.FileTreeElement
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.internal.classpath.ModuleRegistry

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
import net.wasdev.wlp.gradle.plugins.tasks.CompileJSPTask
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.internal.tasks.DefaultSourceSet
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.bundling.War
import org.gradle.api.logging.LogLevel

import javax.inject.Inject

class Liberty implements Plugin<Project> {

    private final SourceDirectorySetFactory sourceDirectorySetFactory
    private final ModuleRegistry moduleRegistry
    Project project

    @Inject
    Liberty(SourceDirectorySetFactory sourceDirectorySetFactory, ModuleRegistry moduleRegistry) {
        this.sourceDirectorySetFactory = sourceDirectorySetFactory
        this.moduleRegistry = moduleRegistry
    }

    void apply(Project project) {
        this.project = project
        project.plugins.apply(JavaBasePlugin)

        configureSourceSetDefaults()

        project.extensions.create('liberty', LibertyExtension, project)
        project.configurations.create('libertyLicense')
        project.configurations.create('libertyRuntime')

        String groupName = "Liberty"


        //Create expected server extension from liberty extension data
        project.afterEvaluate{
            if (project.liberty.server == null) {
                project.liberty.server = copyProperties(project.liberty)
            }
            //Checking serverEnv files for server properties
            Liberty.checkEtcServerEnvProperties(project)
            Liberty.checkServerEnvProperties(project.liberty.server)
            //Server objects need to be set per task after the project configuration phase
            setServersForTasks(project)
        }

        project.task('compileJSP', type: CompileJSPTask) {
            description 'Compile the JSP files in the src/main/webapp directory. '
            logging.level = LogLevel.INFO
            dependsOn 'installLiberty', 'compileJava'
            group groupName
        }

        project.task('installLiberty', type: InstallLibertyTask) {
            description "Installs Liberty from a repository"
            logging.level = LogLevel.INFO
            group groupName
        }

        project.task('libertyRun', type: RunTask) {
            description = "Runs a Websphere Liberty Profile server under the Gradle process."
            logging.level = LogLevel.INFO
            group groupName

            project.afterEvaluate {
                dependsOn installAppsDependsOn(server, 'libertyCreate')
            }
        }

        project.task('libertyStatus', type: StatusTask) {
            description 'Checks if the Liberty server is running.'
            logging.level = LogLevel.INFO
            group groupName
            dependsOn 'libertyCreate'
        }

        project.task('libertyCreate') {
            description 'Creates a WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group groupName
            dependsOn "libertyCreateAnt", "libertyCreateConfig"
//            project.afterEvaluate{
//                outputs.file { new File(getUserDir(project), "servers/${project.liberty.server.name}/server.xml") }
//            }
        }
        project.task('libertyCreateAnt', type: CreateTask) {
            group groupName
            dependsOn 'installLiberty'
        }

        project.tasks.create([name: "libertyCreateConfig",
                              description: "Creates the configration files for the system",
                              group: groupName,
                              type: CreateConfigTask]) {
            logging.level = LogLevel.INFO
            dependsOn "libertyCreateBoostrap", "libertyCreateServerXml", "libertyCreateJvmOptions", "libertyCreateServerEnv"
            mustRunAfter "libertyCreateAnt"
        }

        project.task('libertyCreateBoostrap', type: CreateBootstrapTask) {
            description 'Creates the server bootstrap.properties file'
            logging.level = LogLevel.INFO
            group groupName
            dependsOn 'installLiberty'
        }

        project.task('libertyCreateJvmOptions', type: CreateJvmOptionsTask) {
            description 'Creates the server jvm.options file'
            logging.level = LogLevel.INFO
            group groupName
            dependsOn 'installLiberty'
        }

        project.task('libertyCreateServerXml', type: CreateServerXmlTask) {
            description 'Creates the server.xml file'
            logging.level = LogLevel.INFO
            group groupName
            dependsOn 'installLiberty'
        }

        project.task('libertyCreateServerEnv', type: CreateServerEnvTask) {
            description 'Creates the server.evn file'
            logging.level = LogLevel.INFO
            group groupName
            dependsOn 'installLiberty'
        }

        project.task('libertyStart', type: StartTask) {
            description 'Starts the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group groupName

            project.afterEvaluate {
                dependsOn installAppsDependsOn(server, 'libertyCreate')
            }
        }

        project.task('libertyStop', type: StopTask) {
            description 'Stops the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group groupName
        }

        project.task('libertyPackage', type: PackageTask) {
            description 'Generates a WebSphere Liberty Profile server archive.'
            logging.level = LogLevel.DEBUG
            group groupName
            dependsOn "libertyCreateConfig"

            project.afterEvaluate {
                dependsOn installAppsDependsOn(server, 'installLiberty')
            }
        }

        project.task('libertyDump', type: DumpTask) {
            description 'Dumps diagnostic information from the Liberty Profile server into an archive.'
            logging.level = LogLevel.INFO
            group groupName
        }

        project.task('libertyJavaDump', type: JavaDumpTask) {
            description 'Dumps diagnostic information from the Liberty Profile server JVM.'
            logging.level = LogLevel.INFO
            group groupName
        }

        project.task('libertyDebug', type: DebugTask) {
            description 'Runs the Liberty Profile server in the console foreground after a debugger connects to the debug port (default: 7777).'
            logging.level = LogLevel.INFO
            group groupName
        }

        project.task('deploy', type: DeployTask) {
            description 'Deploys a supported file to the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group groupName
            dependsOn 'libertyStart'
        }

        project.task('undeploy', type: UndeployTask) {
             description 'Removes an application from the WebSphere Liberty Profile server.'
             logging.level = LogLevel.INFO
             group groupName
             dependsOn 'libertyStart'
        }

        project.task('installFeature', type: InstallFeatureTask) {
            description 'Install a new feature to the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group groupName

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
            group groupName
            dependsOn 'libertyCreate'
        }

        project.task('cleanDirs', type: CleanTask) {
            description 'Deletes files from some directories from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group groupName
            dependsOn 'libertyStop'
        }

        project.task('installApps', type: InstallAppsTask) {
            description "Copy applications generated by the Gradle project to a Liberty server's dropins or apps directory."
            logging.level = LogLevel.INFO
            group groupName
            dependsOn project.tasks.withType(War)

            project.afterEvaluate {
                if (server.features.name != null && !server.features.name.empty) {
                    dependsOn 'installFeature'
                } else {
                    dependsOn 'libertyCreate'
                }
           }
        }
    }

    private void configureSourceSetDefaults() {
        configureLibertyBaseSourceset("libertyBase")
        configureLibertyConfigSourceset("libertyConfig")
    }

    private blankSourcesetLanguages(def newSrcSet){
        newSrcSet.with{
            java.setSrcDirs([])
            resources.setSrcDirs([])
        }

        if (project.plugins.hasPlugin(GroovyPlugin)){
            newSrcSet.groovy.setSrcDirs([])
        }

        if (project.plugins.hasPlugin(ScalaPlugin)){
            newSrcSet.scala.setSrcDirs([])
        }
    }

    private void configureLibertyConfigSourceset(String sourceSetName) {
        def newSrcSet = project.getConvention().getPlugin(JavaPluginConvention).getSourceSets().create(sourceSetName)
        blankSourcesetLanguages(newSrcSet)

        final LibertyConfigSourceSet libertyConfigSourceSet = new DefaultLibertyConfigSourceSet(((DefaultSourceSet) newSrcSet)
            .getDisplayName(), sourceDirectorySetFactory)

        new DslObject(newSrcSet).getConvention().getPlugins().put(sourceSetName, libertyConfigSourceSet)

        libertyConfigSourceSet.getLibertyConfig().srcDir("/src/main/liberty/config/")

        newSrcSet.getResources().getFilter().exclude(new Spec<FileTreeElement>() {
            boolean isSatisfiedBy(FileTreeElement element) {
                return libertyConfigSourceSet.getLibertyConfig().contains(element.getFile())
            }
        })

        newSrcSet.getAllJava().source(libertyConfigSourceSet.libertyConfig)
        newSrcSet.getAllSource().source(libertyConfigSourceSet.libertyConfig)

    }

    private void configureLibertyBaseSourceset(String sourceSetName) {
        def newSrcSet = project.getConvention().getPlugin(JavaPluginConvention).getSourceSets().create(sourceSetName)
        blankSourcesetLanguages(newSrcSet)

        final LibertyBaseSourceSet libertyBaseSourceSet = new DefaultLibertyBaseSourceSet(((DefaultSourceSet) newSrcSet)
            .getDisplayName(), sourceDirectorySetFactory)

        new DslObject(newSrcSet).getConvention().getPlugins().put(sourceSetName, libertyBaseSourceSet)

        libertyBaseSourceSet.getLibertyBase().srcDir("/src/main/liberty/")

        newSrcSet.getResources().getFilter().exclude(new Spec<FileTreeElement>() {
            boolean isSatisfiedBy(FileTreeElement element) {
                return libertyBaseSourceSet.getLibertyBase().contains(element.getFile())
            }
        })

        newSrcSet.getAllJava().source(libertyBaseSourceSet.libertyBase)
        newSrcSet.getAllSource().source(libertyBaseSourceSet.libertyBase)
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
                envProperties.load(new FileInputStream(serverEnvFile))
                Liberty.setLibertyOutputDir(project, (String) envProperties.get("WLP_OUTPUT_DIR"))
            }
        }
    }

    public static void checkServerEnvProperties(ServerExtension server) {
        if (server.outputDir == null) {
            Properties envProperties = new Properties()
            //check server.env files and set liberty.server.outputDir
            if (server.configDirectory != null) {
                File serverEnvFile = new File(server.configDirectory, 'server.env')
                if (serverEnvFile.exists()) {
                    envProperties.load(new FileInputStream(serverEnvFile))
                    Liberty.setServerOutputDir(server, (String) envProperties.get("WLP_OUTPUT_DIR"))
                }
            } else if (server.serverEnv.exists()) {
                envProperties.load(new FileInputStream(server.serverEnv))
                Liberty.setServerOutputDir(server, (String) envProperties.get("WLP_OUTPUT_DIR"))
            }
        }
    }

    private static void setLibertyOutputDir(Project project, String envOutputDir){
        if (envOutputDir != null) {
            project.liberty.outputDir = envOutputDir
        }
    }

    private static void setServerOutputDir(ServerExtension server, String envOutputDir){
        if (envOutputDir != null) {
            server.outputDir = envOutputDir
        }
    }

    private void setServersForTasks(Project project){
        project.tasks.withType(AbstractServerTask).each {task ->
            task.server = project.liberty.server
        }
    }

    private String installAppsDependsOn(ServerExtension server, String elseDepends) {
        if (server.apps != null || server.dropins != null) {
            return 'installApps'
        } else {
            return elseDepends
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
}
