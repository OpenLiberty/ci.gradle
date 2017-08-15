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
      config.addDir(parent, project.war.destinationDir, target);
    }

    public Element getDocumentRoot() {
      return config.getDocumentRoot();
    }
    /*
    public LooseConfigData getConfig() {
        return config;
    }

    public Element getDocumentRoot() {
        return config.getDocumentRoot();
    }

    public Element addArchive(Element parent, String target) {
        return config.addArchive(parent, target);
    }

    public void addOutputDir(Element parent, MavenProject proj, String target) {
        config.addDir(parent, proj.getBuild().getOutputDirectory(), target);
    }

    public void addManifestFile(Element parent, MavenProject proj, String pluginId) throws Exception {
        config.addFile(parent, getManifestFile(proj, "org.apache.maven.plugins", pluginId), "/META-INF/MANIFEST.MF");
    }

    public void addManifestFile(MavenProject proj, String pluginId) throws Exception {
        config.addFile(getManifestFile(proj, "org.apache.maven.plugins", pluginId), "/META-INF/MANIFEST.MF");
    }

    public String getPluginConfiguration(String pluginGroupId, String pluginArtifactId, String key) {
        Xpp3Dom dom = project.getGoalConfiguration(pluginGroupId, pluginArtifactId, null, null);
        if (dom != null) {
            Xpp3Dom val = dom.getChild(key);
            if (val != null) {
                return val.getValue();
            }
        }
        return null;
    }

    public String getManifestFile(MavenProject proj, String pluginGroupId, String pluginArtifactId) throws Exception {
        Xpp3Dom dom = proj.getGoalConfiguration(pluginGroupId, pluginArtifactId, null, null);
        if (dom != null) {
            Xpp3Dom archive = dom.getChild("archive");
            if (archive != null) {
                Xpp3Dom val = archive.getChild("manifestFile");
                if (val != null) {
                    return proj.getBasedir().getAbsolutePath() + "/" + val.getValue();
                }
            }
        }
        return getDefaultManifest().getCanonicalPath();
    }

    public File getDefaultManifest() throws Exception {
        if (defaultMF == null) {
            defaultMF = new File(
                    project.getBuild().getDirectory() + "/liberty-maven/resources/META-INF/MANIFEST.MF");
            defaultMF.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(defaultMF);

            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.write(fos);
            fos.close();
        }
        return defaultMF;
    }*/
}
