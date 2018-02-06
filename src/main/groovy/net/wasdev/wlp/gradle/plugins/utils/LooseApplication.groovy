package net.wasdev.wlp.gradle.plugins.utils;

import org.w3c.dom.Element
import org.gradle.api.Project
import org.gradle.api.Task

class LooseApplication {
    protected Task task
    protected LooseConfigData config

    LooseApplication(Task task, LooseConfigData config) {
        this.task = task
        this.config = config
    }

    void addOutputDir(Element parent, Task task, String target) {
      config.addDir(parent, task.classpath.getFiles().toArray()[0].getCanonicalPath(), target)
    }

    void addOutputDir(Element parent, String path, String target) {
      config.addDir(parent, path, target)
    }

    Element getDocumentRoot() {
      return config.getDocumentRoot()
    }

    LooseConfigData getConfig() {
        return config
    }

    void addOutputDir(Element parent, File proj, String target) {
        config.addDir(parent, proj.getAbsolutePath(), target)
    }

    void addOutputDirectory(Element parent, Project project, String target) {
        config.addDir(parent, project.sourceSets.main.getOutput().getClassesDirs().getSingleFile().getAbsolutePath(), target)
    }

    void addManifestFile(File mf) throws Exception {
        if(mf.exists()){
          config.addFile(mf.getAbsolutePath(), "/META-INF/MANIFEST.MF")
        }
    }

    void addManifestFile(Element parent, Project project) throws Exception {
         if(task.getManifest() != null)
           config.addFile(parent, project.sourceSets.main.getOutput().getResourcesDir().getParentFile()
               .getAbsolutePath() + "/META-INF/MANIFEST.MF", "/META-INF/MANIFEST.MF")
     }

    Element addArchive(Element parent, String target) {
        return config.addArchive(parent, target)
    }
}
