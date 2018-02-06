package net.wasdev.wlp.gradle.plugins.definition

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.internal.file.FileResolver

class LibertyConfigSourceSetFactory implements NamedDomainObjectFactory<LibertyConfigSourceSet> {

  private final FileResolver fileResolver

  LibertyConfigSourceSetFactory(FileResolver fileResolver) {
    this.fileResolver = fileResolver
  }

  @Override
  LibertyConfigSourceSet create(String name) {
    new DefaultLibertyBaseSourceSet(name, fileResolver)
  }
}
