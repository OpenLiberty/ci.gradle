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
import net.wasdev.wlp.gradle.plugins.extensions.LibertyExtension
import net.wasdev.wlp.gradle.plugins.extensions.ServerExtension
import net.wasdev.wlp.gradle.plugins.tasks.AbstractServerTask
import net.wasdev.wlp.gradle.plugins.utils.LibertyIntstallController
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileTreeElement
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.internal.tasks.DefaultSourceSet
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.bundling.War
import org.gradle.plugins.ear.Ear

import javax.inject.Inject

class Liberty extends LibertyTrait implements Plugin<Project> {

  private final SourceDirectorySetFactory sourceDirectorySetFactory
  private final ModuleRegistry moduleRegistry
  Project project

  public static final String LIBERTY_DEPLOY_CONFIGURATION = "libertyDeploy"
  public static final String LIBERTY_DEPLOY_APP_CONFIGURATION = "libertyDeployApp"

  public static final String TASK_CORE_EAR = "ear"
  public static final String TASK_CORE_WAR = "war"


  @Inject
  Liberty(SourceDirectorySetFactory sourceDirectorySetFactory, ModuleRegistry moduleRegistry) {
    this.sourceDirectorySetFactory = sourceDirectorySetFactory
    this.moduleRegistry = moduleRegistry

    initTaskDefMap()
  }

  final String JST_WEB_FACET_VERSION = '3.0'
  final String JST_EAR_FACET_VERSION = '6.0'

  void apply(Project project) {
    this.project = project
    project.plugins.apply(JavaBasePlugin)
    configureSourceSetDefaults()

    project.extensions.create('liberty', LibertyExtension, project)
    project.configurations.create('libertyLicense')
    project.configurations.create('libertyRuntime')

    setEclipseFacets(project)

    project.configurations.create(LIBERTY_DEPLOY_CONFIGURATION) {
      description: "Configuration that allows for deploying projects to liberty dropins folder"
    }

    project.configurations.create(LIBERTY_DEPLOY_APP_CONFIGURATION) {
      description: "Configuration that allows for deploying projects to liberty apps folder"
    }

    //Create expected server extension from liberty extension data
    project.afterEvaluate {
      if (project.liberty.server == null) {
        project.liberty.server = copyProperties(project.liberty)
      }

      // set logging level for all tasks
      for (String sTask in taskDefMap.keySet()) {
        Task tTask = project.tasks.findByName(sTask)
        if (tTask != null) {
          tTask.configure {
            logging.level = LogLevel.INFO
          }
        }
      }

      //Checking serverEnv files for server properties
      checkEtcServerEnvProperties(project)
      checkServerEnvProperties(project.liberty.server)

      //Server objects need to be set per task after the project configuration phase
      setServersForTasks(project)
    }

    for (String sTask in taskDefMap.keySet()) {
      project.tasks.create(taskDefMap[sTask])
    }

    setTaskWorkflow()
    setTaskAfterEvalWorkflow()
  }

   void setTaskAfterEvalWorkflow() {
    project.afterEvaluate {
      ServerExtension server = project.liberty.server

      a_dependsOn_b(project, TASK_LIBERTY_RUN, installAppsDependsOn(server, TASK_LIBERTY_CREATE))
      a_dependsOn_b(project, TASK_LIBERTY_START, installAppsDependsOn(server, TASK_LIBERTY_CREATE))
      a_dependsOn_b(project, TASK_LIBERTY_PACKAGE, installAppsDependsOn(server, TASK_INSTALL_LIBERTY))

      if (dependsOnFeature(server)) {
        a_dependsOn_b(project, TASK_INSTALL_FEATURE, TASK_LIBERTY_CREATE)
      } else {
        a_dependsOn_b(project, TASK_INSTALL_FEATURE, TASK_INSTALL_LIBERTY)
      }

      if (!dependsOnApps(server)) {
        if (project.plugins.hasPlugin(WarPlugin)) {
          a_dependsOn_b(project, TASK_LIBERTY_RUN, TASK_INSTALL_APPS)
          a_dependsOn_b(project, TASK_LIBERTY_START, TASK_INSTALL_APPS)
          a_dependsOn_b(project, TASK_LIBERTY_PACKAGE, TASK_INSTALL_APPS)
        }
      }

      setOnlyIf(project, TASK_INSTALL_APPS_ARCHIVE, { !server.looseApplication })
      setOnlyIf(project, TASK_INSTALL_APPS_LOOSE, { server.looseApplication })

      setOnlyIf(project, TASK_INSTALL_APPS_AUTOCONFIG, { server.autoConfigure })

      setOnlyIf(project, TASK_LIBERTY_CREATE_SERVER_XML, { server.configFile.exists() })
      setOnlyIf(project, TASK_LIBERTY_CREATE_SERVER_DEFAULT_XML, { !server.configFile.exists() })
      setOnlyIf(project, TASK_LIBERTY_CREATE_SERVER_ENV, { server.serverEnv.exists() })
    }
  }

  void setTaskWorkflow() {
    a_dependsOn_b(project, TASK_COMPILE_JSP, TASK_INSTALL_LIBERTY)
    a_dependsOn_b(project, TASK_COMPILE_JSP, 'compileJava')

    a_dependsOn_b(project, TASK_LIBERTY_STATUS, TASK_LIBERTY_CREATE)

    a_dependsOn_b(project, TASK_LIBERTY_CREATE, TASK_LIBERTY_CREATE_ANT)
    a_dependsOn_b(project, TASK_LIBERTY_CREATE, TASK_LIBERTY_CREATE_CONFIG)

    a_dependsOn_b(project, TASK_LIBERTY_CREATE_ANT, TASK_INSTALL_LIBERTY)

    a_mustRunAfter_b(project, TASK_LIBERTY_CREATE_CONFIG, TASK_LIBERTY_CREATE_ANT)

    a_dependsOn_b(project, TASK_LIBERTY_CREATE_CONFIG, TASK_LIBERTY_CREATE_BOOTSTRAP)
    a_dependsOn_b(project, TASK_LIBERTY_CREATE_CONFIG, TASK_LIBERTY_CREATE_SERVER_DEFAULT_XML)
    a_dependsOn_b(project, TASK_LIBERTY_CREATE_CONFIG, TASK_LIBERTY_CREATE_SERVER_XML)
    a_dependsOn_b(project, TASK_LIBERTY_CREATE_CONFIG, TASK_LIBERTY_CREATE_JVM_OPTIONS)
    a_dependsOn_b(project, TASK_LIBERTY_CREATE_CONFIG, TASK_LIBERTY_CREATE_SERVER_ENV)

    a_dependsOn_b(project, TASK_LIBERTY_CREATE_BOOTSTRAP, TASK_INSTALL_LIBERTY)

    a_dependsOn_b(project, TASK_LIBERTY_CREATE_JVM_OPTIONS, TASK_INSTALL_LIBERTY)

    a_dependsOn_b(project, TASK_LIBERTY_CREATE_SERVER_XML, TASK_INSTALL_LIBERTY)

    a_dependsOn_b(project, TASK_LIBERTY_CREATE_SERVER_ENV, TASK_INSTALL_LIBERTY)

    setOnlyIf(project, TASK_LIBERTY_START, { !LibertyIntstallController.isServerRunning(project) })
    setOnlyIf(project, TASK_LIBERTY_STOP, { LibertyIntstallController.isServerRunning(project) })

    a_dependsOn_b(project, TASK_LIBERTY_PACKAGE, TASK_LIBERTY_CREATE_CONFIG)

    a_dependsOn_b(project, TASK_DEPLOY, TASK_LIBERTY_START)

    a_dependsOn_b(project, TASK_UNDEPLOY, TASK_LIBERTY_START)

    a_dependsOn_b(project, TASK_UNINSTALL_FEATURE, TASK_LIBERTY_CREATE)

    a_dependsOn_b(project, TASK_CLEAN_DIRS, TASK_LIBERTY_STOP)

    Task taskATask = project.tasks.findByName(TASK_INSTALL_APPS_ARCHIVE)
    taskATask.dependsOn(project.tasks.withType(War))
    taskATask.dependsOn(project.tasks.withType(Ear))

    Task taskBTask = project.tasks.findByName(TASK_INSTALL_APPS_LOOSE)
    taskBTask.dependsOn(project.tasks.withType(War))
    taskBTask.dependsOn(project.tasks.withType(Ear))

    a_dependsOn_b(project, TASK_INSTALL_APPS, TASK_CORE_WAR)
    a_dependsOn_b(project, TASK_INSTALL_APPS, TASK_LIBERTY_CREATE)
    a_dependsOn_b(project, TASK_INSTALL_APPS, TASK_INSTALL_APPS_AUTOCONFIG)

    a_mustRunAfter_b(project, TASK_INSTALL_APPS_AUTOCONFIG, TASK_INSTALL_APPS_SANITY)

    a_dependsOn_b(project, TASK_INSTALL_APPS_SANITY, TASK_INSTALL_APPS_ARCHIVE)
    a_dependsOn_b(project, TASK_INSTALL_APPS_SANITY, TASK_INSTALL_APPS_LOOSE)

    a_dependsOn_b(project, TASK_INSTALL_APPS, TASK_INSTALL_APPS_SANITY)

    a_mustRunAfter_b(project, TASK_INSTALL_APPS_ARCHIVE, TASK_LIBERTY_CREATE)
    a_mustRunAfter_b(project, TASK_INSTALL_APPS_LOOSE, TASK_LIBERTY_CREATE)

  }

  private void configureSourceSetDefaults() {
    configureLibertyBaseSourceset("libertyBase")
    configureLibertyConfigSourceset("libertyConfig")
  }

  private blankSourcesetLanguages(def newSrcSet) {
    newSrcSet.with {
      java.setSrcDirs([])
      resources.setSrcDirs([])
    }

    if (project.plugins.hasPlugin(GroovyPlugin)) {
      newSrcSet.groovy.setSrcDirs([])
    }

    if (project.plugins.hasPlugin(ScalaPlugin)) {
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

  private void setEclipseFacets(Project project) {
    //Used to set project facets in Eclipse
    project.pluginManager.apply('eclipse-wtp')
    project.tasks.getByName('eclipseWtpFacet').finalizedBy 'libertyCreate'

    //Uplift the jst.web facet version to 3.0 if less than 3.0 so WDT can deploy properly to Liberty.
    //There is a known bug in the wtp plugin that will add duplicate facets, the first of the duplicates is honored.
    project.tasks.getByName('eclipseWtpFacet').facet.file.whenMerged {
      if (project.plugins.hasPlugin('war')) {
        setFacetVersion(project, 'jst.web', JST_WEB_FACET_VERSION)
      } else if (project.plugins.hasPlugin('ear')) {
        setFacetVersion(project, 'jst.ear', JST_EAR_FACET_VERSION)
      }
    }

    if (project.plugins.hasPlugin('ear')) {
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
    if (project.plugins.hasPlugin('eclipse-wtp')) {
      project.tasks.getByName('eclipseWtpFacet').facet.file.whenMerged {
        def jstFacet = facets.find {
          it.type.name() == 'installed' && it.name == facetName && Double.parseDouble(it.version) < Double.parseDouble(version)
        }
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
      } else {
        serverMap.put(element, libertyMap.get(element))
      }
    }
    serverMap.remove('class')
    serverMap.remove('outputDir')

    return ServerExtension.newInstance(serverMap)
  }

  static void checkEtcServerEnvProperties(Project project) {
    if (project.liberty.outputDir == null) {
      Properties envProperties = new Properties()
      //check etc/server.env and set liberty.outputDir
      File serverEnvFile = new File(getInstallDir(project), 'etc/server.env')
      if (serverEnvFile.exists()) {
        envProperties.load(new FileInputStream(serverEnvFile))
        setLibertyOutputDir(project, (String) envProperties.get("WLP_OUTPUT_DIR"))
      }
    }
  }

  static void checkServerEnvProperties(ServerExtension server) {
    if (server.outputDir == null) {
      Properties envProperties = new Properties()
      //check server.env files and set liberty.server.outputDir
      if (server.configDirectory != null) {
        File serverEnvFile = new File(server.configDirectory, 'server.env')
        if (serverEnvFile.exists()) {
          envProperties.load(new FileInputStream(serverEnvFile))
          setServerOutputDir(server, (String) envProperties.get("WLP_OUTPUT_DIR"))
        }
      } else if (server.serverEnv.exists()) {
        envProperties.load(new FileInputStream(server.serverEnv))
        setServerOutputDir(server, (String) envProperties.get("WLP_OUTPUT_DIR"))
      }
    }
  }

  private static void setLibertyOutputDir(Project project, String envOutputDir) {
    if (envOutputDir != null) {
      project.liberty.outputDir = envOutputDir
    }
  }

  private static void setServerOutputDir(ServerExtension server, String envOutputDir) {
    if (envOutputDir != null) {
      server.outputDir = envOutputDir
    }
  }

  private void setServersForTasks(Project project) {
    project.tasks.withType(AbstractServerTask).each { task ->
      task.server = project.liberty.server
    }
  }

  static String installAppsDependsOn(ServerExtension server, String elseDepends) {
    if (server.apps != null || server.dropins != null) {
      return TASK_INSTALL_APPS
    } else {
      return elseDepends
    }
  }

  static boolean dependsOnApps(ServerExtension server) {
    return ((server.apps != null && !server.apps.isEmpty()) ||
        (server.dropins != null && !server.dropins.isEmpty()))
  }

  static boolean dependsOnFeature(ServerExtension server) {
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
