package net.wasdev.wlp.gradle.plugins.loose

import groovy.transform.TupleConstructor
import org.gradle.api.Project
import org.gradle.api.Task

@TupleConstructor
class InstallDTO {
  InstallType installType
  Task task
  Project parentProject

  @Override
  String toString() {
    return """
    InstallType: ${installType.toString()}
    Task: ${task.name}
    Parent Project: ${parentProject.name}
    """.stripIndent().normalize()
  }
}
