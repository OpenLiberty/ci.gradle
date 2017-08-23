package net.wasdev.wlp.gradle.plugins.utils;


import java.io.File;
import org.w3c.dom.Element;
import org.gradle.api.Project;

public class LooseApplication {
    protected Project project;
    protected LooseConfigData config;

    public LooseApplication(Project project, LooseConfigData config) {
        this.project = project;
        this.config = config;
    }
    public void addOutputDir(Element parent, Project project, String target) {
      config.addDir(parent, project.war.destinationDir.getParentFile().getCanonicalPath() + "/classes", target);
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

    public void addOutputDirectory(Element parent, Project proj, String target) {
        //config.addDir(parent, proj.sourceSets.main.getOutput().getClassesDirs().getSingleFile().getParentFile().getParentFile().getAbsolutePath(), target);
        config.addDir(parent, proj.sourceSets.main.getOutput().getClassesDirs().getSingleFile().getParentFile().getParentFile().getAbsolutePath(), target);
    }

    public void addManifestFile(File mf, String pluginId) throws Exception {
        if(!mf.exists()){
          project.jar.manifest.writeTo(mf.getAbsolutePath())
        }
        config.addFile(mf.getAbsolutePath(), "/META-INF/MANIFEST.MF");
    }

    public void addManifestFile(Element parent, Project proj, String pluginId) throws Exception {
        if(proj.jar.getManifest() != null)
          config.addFile(parent, proj.sourceSets.main.getOutput().getResourcesDir().getParentFile().getAbsolutePath() + "/META-INF/MANIFEST.MF", "/META-INF/MANIFEST.MF");
    }

    public Element addArchive(Element parent, String target) {
        return config.addArchive(parent, target);
    }
}
