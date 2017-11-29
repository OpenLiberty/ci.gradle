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

import org.gradle.api.*

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

    public static final String LIBERTY_DEPLOY_CONFIGURATION = "libertyDeploy"

    public static final String TASK_LIBERTY_START = "libertyStart"
    public static final String TASK_LIBERTY_STOP = "libertyStop"
    public static final String TASK_LIBERTY_CREATE = "libertyCreate"
    public static final String TASK_LIBERTY_CREATE_CONFIG = "libertyCreateConfig"
    public static final String TASK_LIBERTY_CREATE_ANT = "libertyCreateAnt"
    public static final String TASK_LIBERTY_CREATE_BOOTSTRAP = "libertyCreateBoostrap"
    public static final String TASK_LIBERTY_CREATE_JVM_OPTIONS = "libertyCreateJvmOptions"
    public static final String TASK_LIBERTY_CREATE_SERVER_ENV = "libertyCreateServerEnv"
    public static final String TASK_LIBERTY_CREATE_SERVER_XML = "libertyCreateServerXml"
    public static final String TASK_DEPLOY = "deploy"
    public static final String TASK_INSTALL_LIBERTY = "installLiberty"

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

        //Used to set project facets in Eclipse
        project.pluginManager.apply('eclipse-wtp')
        project.tasks.getByName('eclipseWtpFacet').finalizedBy 'libertyCreate'

        project.configurations.create(LIBERTY_DEPLOY_CONFIGURATION) {
            description: "Configuration that allows for deploying projects to liberty via dependency"
        }

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

            if (!dependsOnApps(project.liberty.server)) {
                if (project.plugins.hasPlugin('war')) {
                    def tasks = project.tasks
                    tasks.getByName('libertyRun').dependsOn 'installApps'
                    tasks.getByName('libertyStart').dependsOn 'installApps'
                    tasks.getByName('libertyPackage').dependsOn 'installApps'
                }
            }
        }

        project.task('compileJSP', type: CompileJSPTask) {
            description 'Compile the JSP files in the src/main/webapp directory. '
            logging.level = LogLevel.INFO
            dependsOn TASK_INSTALL_LIBERTY, 'compileJava'
            group groupName
        }

        project.task(TASK_INSTALL_LIBERTY, type: InstallLibertyTask) {
            description "Installs Liberty from a repository"
            logging.level = LogLevel.INFO
            group groupName
        }

        project.task('libertyRun', type: RunTask) {
            description = "Runs a Websphere Liberty Profile server under the Gradle process."
            logging.level = LogLevel.INFO
            group groupName

            project.afterEvaluate {
                dependsOn installAppsDependsOn(server, TASK_LIBERTY_CREATE)
            }
        }

        project.task('libertyStatus', type: StatusTask) {
            description 'Checks if the Liberty server is running.'
            logging.level = LogLevel.INFO
            group groupName
            dependsOn TASK_LIBERTY_CREATE
        }

        project.task(TASK_LIBERTY_CREATE) {
            description 'Creates a WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group groupName
            dependsOn TASK_LIBERTY_CREATE_ANT, TASK_LIBERTY_CREATE_CONFIG
//            project.afterEvaluate{
//                outputs.file { new File(getUserDir(project), "servers/${project.liberty.server.name}/server.xml") }
//            }
        }
        project.task(TASK_LIBERTY_CREATE_ANT, type: CreateTask) {
            group groupName
            dependsOn TASK_INSTALL_LIBERTY
        }

        project.tasks.create([name: TASK_LIBERTY_CREATE_CONFIG,
                              description: "Creates the configration files for the system",
                              group: groupName,
                              type: CreateConfigTask]) {
            logging.level = LogLevel.INFO
            dependsOn TASK_LIBERTY_CREATE_BOOTSTRAP, TASK_LIBERTY_CREATE_SERVER_XML,
                TASK_LIBERTY_CREATE_JVM_OPTIONS, TASK_LIBERTY_CREATE_SERVER_ENV
            mustRunAfter TASK_LIBERTY_CREATE_ANT
        }

        project.task(TASK_LIBERTY_CREATE_BOOTSTRAP, type: CreateBootstrapTask) {
            description 'Creates the server bootstrap.properties file'
            logging.level = LogLevel.INFO
            group groupName
            dependsOn TASK_INSTALL_LIBERTY
        }

        project.task(TASK_LIBERTY_CREATE_JVM_OPTIONS, type: CreateJvmOptionsTask) {
            description 'Creates the server jvm.options file'
            logging.level = LogLevel.INFO
            group groupName
            dependsOn TASK_INSTALL_LIBERTY
        }

        project.task(TASK_LIBERTY_CREATE_SERVER_XML, type: CreateServerXmlTask) {
            description 'Creates the server.xml file'
            logging.level = LogLevel.INFO
            group groupName
            dependsOn TASK_INSTALL_LIBERTY
        }

        project.task(TASK_LIBERTY_CREATE_SERVER_ENV, type: CreateServerEnvTask) {
            description 'Creates the server.evn file'
            logging.level = LogLevel.INFO
            group groupName
            dependsOn TASK_INSTALL_LIBERTY
        }


        project.tasks.create([name: TASK_LIBERTY_START,
                              type: StartTask,
                              description: 'Starts the WebSphere Liberty Profile server.',
                              group: groupName]) {

            logging.level = LogLevel.INFO

            project.afterEvaluate {
                dependsOn installAppsDependsOn(server, TASK_LIBERTY_CREATE)
            }
        }.onlyIf {
            !LibertyIntstallController.isServerRunning(project)
        }

        project.tasks.create([name: TASK_LIBERTY_STOP,
                              type: StopTask,
                              description: 'Stops the WebSphere Liberty Profile server.',
                              group: groupName]) {

            logging.level = LogLevel.INFO

        }.onlyIf {
            LibertyIntstallController.isServerRunning(project)
        }


        project.task('libertyPackage', type: PackageTask) {
            description 'Generates a WebSphere Liberty Profile server archive.'
            logging.level = LogLevel.DEBUG
            group groupName
            dependsOn TASK_LIBERTY_CREATE_CONFIG

            project.afterEvaluate {
                dependsOn installAppsDependsOn(server, TASK_INSTALL_LIBERTY)
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

        project.task(TASK_DEPLOY, type: DeployTask) {
            description 'Deploys a supported file to the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group groupName
            dependsOn TASK_LIBERTY_START
        }

        project.task('undeploy', type: UndeployTask) {
             description 'Removes an application from the WebSphere Liberty Profile server.'
             logging.level = LogLevel.INFO
             group groupName
             dependsOn TASK_LIBERTY_START
        }

        project.task('installFeature', type: InstallFeatureTask) {
            description 'Install a new feature to the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group groupName

            project.afterEvaluate {
                if (dependsOnFeature(server)) {
                  dependsOn TASK_LIBERTY_CREATE
                } else {
                    dependsOn TASK_INSTALL_LIBERTY
                }
            }
        }

        project.task('uninstallFeature', type: UninstallFeatureTask) {
            description 'Uninstall a feature from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group groupName
            dependsOn TASK_LIBERTY_CREATE
        }

        project.task('cleanDirs', type: CleanTask) {
            description 'Deletes files from some directories from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group groupName
            dependsOn TASK_LIBERTY_STOP
        }

        project.task('installApps', type: InstallAppsTask) {
            description "Copy applications generated by the Gradle project to a Liberty server's dropins or apps directory."
            logging.level = LogLevel.INFO
            group groupName
            dependsOn project.tasks.withType(War), TASK_LIBERTY_CREATE
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

        libertyConfigSourceSet.getLibertyConfig().srcDir("/src/main/libertyConfig/")

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

        libertyBaseSourceSet.getLibertyBase().srcDir("/src/main/libertyBase/")

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
        project.tasks.withType(AbstractServerTask).each { task ->
            task.server = project.liberty.server
        }
    }

    private List<String> installDependsOn(ServerExtension server, String elseDepends) {
        List<String> tasks = new ArrayList<String>()
        boolean apps = dependsOnApps(server)
        boolean feature = dependsOnFeature(server)

        if (apps) tasks.add('installApps')
        if (feature) tasks.add('installFeature')
        if (!apps && !feature) tasks.add(elseDepends)
        return tasks
    }

    private boolean dependsOnApps(ServerExtension server) {
        return ((server.apps != null && !server.apps.isEmpty()) ||
                (server.dropins != null && !server.dropins.isEmpty()))
    }

    private boolean dependsOnFeature(ServerExtension server) {
        return (server.features.name != null && !server.features.name.isEmpty())
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
