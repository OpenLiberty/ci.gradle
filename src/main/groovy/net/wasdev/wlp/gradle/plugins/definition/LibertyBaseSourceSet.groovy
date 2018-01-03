package net.wasdev.wlp.gradle.plugins.definition

import org.gradle.api.file.SourceDirectorySet

interface LibertyBaseSourceSet {
  String getName()

  SourceDirectorySet getLibertyBase()

  SourceDirectorySet getAllLibertyBase()

  LibertyBaseSourceSet libertyBase(Closure clsr)
}
