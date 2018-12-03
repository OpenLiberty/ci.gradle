package net.wasdev.wlp.gradle.plugins.utils;

import java.io.File;
import org.w3c.dom.Element;
import org.gradle.api.Project;
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.War
import org.gradle.api.GradleException

import net.wasdev.wlp.common.plugins.config.LooseConfigData

public class LooseApplication {
    protected Task task;
    protected LooseConfigData config;

    public LooseApplication(Task task, LooseConfigData config) {
        this.task = task;
        this.config = config;
    }
    public void addOutputDir(Element parent, Task task, String target) {
      config.addDir(parent, task.classpath.getFiles().toArray()[0].getCanonicalPath(), target);
    }

    public void addOutputDir(Element parent, String path, String target) {
      config.addDir(parent, path, target);
    }

    public Element getDocumentRoot() {
      return config.getDocumentRoot();
    }

    public LooseConfigData getConfig() {
        return config;
    }

    public void addOutputDir(Element parent, File proj, String target) {
        config.addDir(parent, proj.getAbsolutePath(), target);
    }

    public void addOutputDirectory(Element parent, Project project, String target) {
        config.addDir(parent, project.sourceSets.main.getOutput().getClassesDirs().getSingleFile().getAbsolutePath(), target);
    }

    public void addManifestFile(File mf) throws Exception {
        if(mf.exists()){
          config.addFile(mf.getAbsolutePath(), "/META-INF/MANIFEST.MF");
        }
    }

    public void addManifestFile(Element parent, Project project) throws Exception {
         if(task.getManifest() != null)
           config.addFile(parent, project.sourceSets.main.getOutput().getResourcesDir().getParentFile().getAbsolutePath() + "/META-INF/MANIFEST.MF", "/META-INF/MANIFEST.MF");
     }
    public Element addArchive(Element parent, String target) {
        return config.addArchive(parent, target);
    }
}
