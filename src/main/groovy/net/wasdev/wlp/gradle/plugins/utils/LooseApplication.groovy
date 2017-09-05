package net.wasdev.wlp.gradle.plugins.utils;


import java.io.File;
import org.w3c.dom.Element;
import org.gradle.api.Project;
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.War

public class LooseApplication {
    protected Task task;
    protected LooseConfigData config;

    public LooseApplication(Task task, LooseConfigData config) {
        this.task = task;
        this.config = config;
    }
    public void addOutputDir(Element parent, Task task, String target) {
      System.out.println("\n\n\n::::::::::::::")
      System.out.println(task.destinationDir.getParentFile().getCanonicalPath())
      System.out.println("\n\n\n::::::::::::::")
      config.addDir(parent, task.destinationDir.getParentFile().getCanonicalPath() + "/classes/java/main", target);
    }

    public Element getDocumentRoot() {
      return config.getDocumentRoot();
    }

    public LooseConfigData getConfig() {
        return config;
    }

    public void addOutputDir(Element parent, File proj, String target) {
        config.addDir(parent, task.getProject().getAbsolutePath(), target);
    }

    public void addOutputDirectory(Element parent, Task task, String target) {
        config.addDir(parent, task.getProject().sourceSets.main.getOutput().getClassesDirs().getSingleFile().getAbsolutePath(), target);
    }

    public void addManifestFile(File mf, String pluginId) throws Exception {
        if(!mf.exists()){
          task.manifest.writeTo(mf.getAbsolutePath())
          config.addFile(mf.getAbsolutePath(), "/META-INF/MANIFEST.MF");
        }
    }

    public void addManifestFile(Element parent, Task task, String pluginId) throws Exception {
        if(task.getManifest() != null)
          config.addFile(parent, task.getProject().sourceSets.main.getOutput().getResourcesDir().getParentFile().getAbsolutePath() + "/META-INF/MANIFEST.MF", "/META-INF/MANIFEST.MF");
    }

    public Element addArchive(Element parent, String target) {
        return config.addArchive(parent, target);
    }
}
