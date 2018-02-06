package net.wasdev.wlp.gradle.plugins.definition

import org.gradle.api.file.SourceDirectorySet

interface LibertyConfigSourceSet {
  String getName()

  SourceDirectorySet getLibertyConfig()

  SourceDirectorySet getAllLibertyConfig()

  LibertyConfigSourceSet libertyConfig(Closure clsr)
}
