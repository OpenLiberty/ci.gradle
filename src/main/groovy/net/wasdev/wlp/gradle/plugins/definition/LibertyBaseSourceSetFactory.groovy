package net.wasdev.wlp.gradle.plugins.definition

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.internal.file.FileResolver

class LibertyBaseSourceSetFactory implements NamedDomainObjectFactory<LibertyBaseSourceSet> {

  private final FileResolver fileResolver

  LibertyBaseSourceSetFactory(FileResolver fileResolver) {
    this.fileResolver = fileResolver
  }

  @Override
  LibertyBaseSourceSet create(String name) {
    new DefaultLibertyBaseSourceSet(name, fileResolver)
  }
}
