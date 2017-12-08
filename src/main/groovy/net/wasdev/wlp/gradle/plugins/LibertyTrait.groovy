package net.wasdev.wlp.gradle.plugins

import net.wasdev.wlp.gradle.plugins.tasks.*
import org.gradle.api.Project
import org.gradle.api.Task

abstract class LibertyTrait implements ILibertyDefinitions {

  static def initTaskDefMap() {
    taskDefMap[TASK_COMPILE_JSP] = [name       : TASK_COMPILE_JSP,
                                    description: 'Compile the JSP files in the src/main/webapp directory. ',
                                    group      : GROUP_NAME,
                                    type       : CompileJSPTask]

    taskDefMap[TASK_INSTALL_LIBERTY] = [name       : TASK_INSTALL_LIBERTY,
                                        description: "Installs Liberty from a repository",
                                        group      : GROUP_NAME,
                                        type       : InstallLibertyTask]

    taskDefMap[TASK_LIBERTY_RUN] = [name       : TASK_LIBERTY_RUN,
                                    type       : RunTask,
                                    description: "Runs a Websphere Liberty Profile server under the Gradle process.",
                                    group      : GROUP_NAME]

    taskDefMap[TASK_LIBERTY_STATUS] = [name       : TASK_LIBERTY_STATUS,
                                       type       : StatusTask,
                                       description: 'Checks if the Liberty server is running.',
                                       group      : GROUP_NAME]

    taskDefMap[TASK_LIBERTY_CREATE] = [name       : TASK_LIBERTY_CREATE,
                                       type       : StatusTask,
                                       description: 'Creates a WebSphere Liberty Profile server.',
                                       group      : GROUP_NAME]

    taskDefMap[TASK_LIBERTY_CREATE_ANT] = [name       : TASK_LIBERTY_CREATE_ANT,
                                           type       : CreateTask,
                                           description: 'Creates a WebSphere Liberty Profile server.',
                                           group      : GROUP_NAME]

    taskDefMap[TASK_LIBERTY_CREATE_CONFIG] = [name       : TASK_LIBERTY_CREATE_CONFIG,
                                              description: "Creates the configuration files for the system",
                                              group      : GROUP_NAME,
                                              type       : CreateConfigTask]

    taskDefMap[TASK_LIBERTY_CREATE_BOOTSTRAP] = [name       : TASK_LIBERTY_CREATE_BOOTSTRAP,
                                                 description: 'Creates the server bootstrap.properties file',
                                                 group      : GROUP_NAME,
                                                 type       : CreateBootstrapTask]

    taskDefMap[TASK_LIBERTY_CREATE_JVM_OPTIONS] = [name       : TASK_LIBERTY_CREATE_JVM_OPTIONS,
                                                   description: 'Creates the server jvm.options file',
                                                   group      : GROUP_NAME,
                                                   type       : CreateJvmOptionsTask]

    taskDefMap[TASK_LIBERTY_CREATE_SERVER_XML] = [name       : TASK_LIBERTY_CREATE_SERVER_XML,
                                                  description: 'Creates the server.xml file',
                                                  group      : GROUP_NAME,
                                                  type       : CreateServerXmlTask]

    taskDefMap[TASK_LIBERTY_CREATE_SERVER_ENV] = [name       : TASK_LIBERTY_CREATE_SERVER_ENV,
                                                  description: 'Creates the server.evn file',
                                                  group      : GROUP_NAME,
                                                  type       : CreateServerEnvTask]

    taskDefMap[TASK_LIBERTY_START] = [name       : TASK_LIBERTY_START,
                                      type       : StartTask,
                                      description: 'Starts the WebSphere Liberty Profile server.',
                                      group      : GROUP_NAME]

    taskDefMap[TASK_LIBERTY_STOP] = [name       : TASK_LIBERTY_STOP,
                                     type       : StopTask,
                                     description: 'Stops the WebSphere Liberty Profile server.',
                                     group      : GROUP_NAME]

    taskDefMap[TASK_LIBERTY_PACKAGE] = [name       : TASK_LIBERTY_PACKAGE,
                                        type       : PackageTask,
                                        description: 'Generates a WebSphere Liberty Profile server archive.',
                                        group      : GROUP_NAME]

    taskDefMap[TASK_LIBERTY_DUMP] = [name       : TASK_LIBERTY_DUMP,
                                     type       : DumpTask,
                                     description: 'Dumps diagnostic information from the Liberty Profile server into an archive.',
                                     group      : GROUP_NAME]

    taskDefMap[TASK_LIBERTY_JAVA_DUMP] = [name       : TASK_LIBERTY_JAVA_DUMP,
                                          type       : JavaDumpTask,
                                          description: 'Dumps diagnostic information from the Liberty Profile server JVM.',
                                          group      : GROUP_NAME]

    taskDefMap[TASK_LIBERTY_DEBUG] = [name       : TASK_LIBERTY_DEBUG,
                                      type       : DebugTask,
                                      description: 'Runs the Liberty Profile server in the console foreground after a debugger connects to the debug port (default: 7777).',
                                      group      : GROUP_NAME]

    taskDefMap[TASK_DEPLOY] = [name       : TASK_DEPLOY,
                               type       : DeployTask,
                               description: 'Deploys a supported file to the WebSphere Liberty Profile server.',
                               group      : GROUP_NAME]

    taskDefMap[TASK_UNDEPLOY] = [name       : TASK_UNDEPLOY,
                                 type       : UndeployTask,
                                 description: 'Removes an application from the WebSphere Liberty Profile server.',
                                 group      : GROUP_NAME]

    taskDefMap[TASK_INSTALL_FEATURE] = [name       : TASK_INSTALL_FEATURE,
                                        type       : InstallFeatureTask,
                                        description: 'Install a new feature to the WebSphere Liberty Profile server',
                                        group      : GROUP_NAME]

    taskDefMap[TASK_UNINSTALL_FEATURE] = [name       : TASK_UNINSTALL_FEATURE,
                                          type       : UninstallFeatureTask,
                                          description: 'Uninstall a feature from the WebSphere Liberty Profile server',
                                          group      : GROUP_NAME]

    taskDefMap[TASK_CLEAN_DIRS] = [name       : TASK_CLEAN_DIRS,
                                   type       : CleanTask,
                                   description: 'Deletes files from some directories from the WebSphere Liberty Profile server',
                                   group      : GROUP_NAME]

    taskDefMap[TASK_INSTALL_APPS] = [name       : TASK_INSTALL_APPS,
                                     description: "Copy applications generated by the Gradle project to a Liberty server's dropins or apps directory.",
                                     group      : GROUP_NAME]

    taskDefMap[TASK_INSTALL_APPS_ARCHIVE] = [name       : TASK_INSTALL_APPS_ARCHIVE,
                                     type       : InstallAppsArchiveTask,
                                     description: "Child of installApps that installs archives",
                                     group      : GROUP_NAME]

    taskDefMap[TASK_INSTALL_APPS_LOOSE] = [name       : TASK_INSTALL_APPS_LOOSE,
                                     type       : InstallAppsLooseTask,
                                     description: "Child of installApps that installs loose projects",
                                     group      : GROUP_NAME]

    taskDefMap[TASK_INSTALL_APPS_SANITY] = [name       : TASK_INSTALL_APPS_SANITY,
                                           type       : InstallAppsSanityTask,
                                           description: "Child of installApps that does a sanity check on the config",
                                           group      : GROUP_NAME]

    taskDefMap[TASK_INSTALL_APPS_AUTOCONFIG] = [name       : TASK_INSTALL_APPS_AUTOCONFIG,
                                            type       : InstallAppsAutoConfigureTask,
                                            description: "Child of installApps that Auto Configures apps not in server.xml",
                                            group      : GROUP_NAME]
  }

  static def a_mustRunAfter_b(Project proj, String taskA, String taskB) {
    Task taskATask = proj.tasks.findByName(taskA)

    if (taskATask != null) {
      Task taskBTask = proj.tasks.findByName(taskB)

      if (taskBTask != null) {
        taskATask.mustRunAfter(taskBTask)
      }
    }
  }

  static def a_dependsOn_b(Project proj, String taskA, String taskB) {
    Task taskATask = proj.tasks.findByName(taskA)

    if (taskATask != null) {
      Task taskBTask = proj.tasks.findByName(taskB)

      if (taskBTask != null) {
        taskATask.dependsOn(taskBTask)
      }
    }
  }

  static void setOnlyIf(Project project, String taskName, Closure configureClosure){
    Task taskLoose = project.tasks.findByName(taskName)
    if (taskLoose != null) {
      taskLoose.onlyIf = configureClosure
    }
  }
}
