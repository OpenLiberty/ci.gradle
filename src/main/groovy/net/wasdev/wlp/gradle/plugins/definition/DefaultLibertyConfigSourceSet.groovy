package net.wasdev.wlp.gradle.plugins.definition

import org.gradle.api.Action
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.util.ConfigureUtil

class DefaultLibertyConfigSourceSet implements LibertyConfigSourceSet {

  final String name
  final SourceDirectorySet libertyConfig
  final SourceDirectorySet allLibertyConfig

  DefaultLibertyConfigSourceSet(String displayName, SourceDirectorySetFactory sourceDirectorySetFactory) {
    this.name = displayName

    libertyConfig = sourceDirectorySetFactory.create(displayName + " Liberty Config Files source")
    libertyConfig.getFilter().include("**/*")

    allLibertyConfig = sourceDirectorySetFactory.create(displayName + " Liberty Config Files source")
    allLibertyConfig.source(libertyConfig)
    allLibertyConfig.getFilter().include("**/*.*")
  }

  @Override
  String getName() {
    return name
  }

  @Override
  LibertyConfigSourceSet libertyConfig(Closure clsr) {
    ConfigureUtil.configure(clsr, libertyConfig)
    this
  }

  LibertyConfigSourceSet libertyConfig(Action<? super SourceDirectorySet> configureAction) {
    configureAction.execute(libertyConfig)
    this
  }
}
