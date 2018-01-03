package net.wasdev.wlp.gradle.plugins.definition

import org.gradle.api.Action
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.util.ConfigureUtil

class DefaultLibertyBaseSourceSet implements LibertyBaseSourceSet {

  final String name
  final SourceDirectorySet libertyBase
  final SourceDirectorySet allLibertyBase

  DefaultLibertyBaseSourceSet(String displayName, SourceDirectorySetFactory sourceDirectorySetFactory) {
    this.name = displayName

    libertyBase = sourceDirectorySetFactory.create(displayName + " Liberty Base Files source")
    libertyBase.getFilter().include("**/*.*").exclude("config/**")

    allLibertyBase = sourceDirectorySetFactory.create(displayName + " Liberty Base Files source")
    allLibertyBase.source(libertyBase)
    allLibertyBase.getFilter().include("**/*.*").exclude("config/**")
  }

  @Override
  String getName() {
    return name
  }

  @Override
  LibertyBaseSourceSet libertyBase(Closure clsr) {
    ConfigureUtil.configure(clsr, libertyBase)
    this
  }

  LibertyBaseSourceSet libertyBase(Action<? super SourceDirectorySet> configureAction) {
    configureAction.execute(libertyBase)
    this
  }
}
