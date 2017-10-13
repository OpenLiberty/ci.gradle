package net.wasdev.wlp.gradle.plugins.utils

import org.gradle.api.Project

class LibertyIntstallController {

  static File getInstallDir(Project project) {
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

  static List<String> buildCommand (Project project, String operation) {
    List<String> command = new ArrayList<String>()
    boolean isWindows = System.properties['os.name'].toLowerCase().indexOf("windows") >= 0
    String installDir = getInstallDir(project).toString()

    if (isWindows) {
      command.add(installDir + "\\bin\\server.bat")
    } else {
      command.add(installDir + "/bin/server")
    }
    command.add(operation)
    command.add(project.liberty.server.name)

    return command
  }

  static printServerStatus (Project project){
    def status_process = serverStatusWorker(project)

    status_process.inputStream.eachLine {
      println it
    }
  }

  static boolean isServerRunning (Project project){
    def status_process = serverStatusWorker(project)

    def checker = []
    status_process.inputStream.eachLine {
      checker << it
    }

    for (String s in checker) {
      if (s.contains("Server ${project.liberty.server.name} is running.")){
        return true
      }
    }

    return false
  }

  private static def serverStatusWorker(Project project) {
    def status_process = new ProcessBuilder(buildCommand(project, "status"))
        .redirectErrorStream(true).start()
  }
}
