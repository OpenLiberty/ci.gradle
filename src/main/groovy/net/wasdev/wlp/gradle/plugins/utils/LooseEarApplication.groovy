package net.wasdev.wlp.gradle.plugins.utils

import java.io.File
import org.gradle.api.Project
import org.gradle.plugins.ear.EarPluginConvention
import org.gradle.api.Task
import org.gradle.plugins.ear.Ear
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.artifacts.Dependency
import org.w3c.dom.Element;



public class LooseEarApplication extends LooseApplication {

    public LooseEarApplication(Task task, LooseConfigData config) {
        super(task, config);
    }

    public void addSourceDir() throws Exception {
        File sourceDir = new File(task.getProject().path.replace(":","") + "/" + task.getProject().getConvention().getPlugin(EarPluginConvention.class).getAppDirName())
        config.addDir(sourceDir.getCanonicalPath(), "/")
    }

    public void addApplicationXmlFile() throws Exception {
        String applicationName = "/" + task.getProject().getConvention().getPlugin(EarPluginConvention.class).getDeploymentDescriptor().getFileName()
        File applicationXmlFile = new File(task.getProject().path.replace(":","") + "/" + task.getProject().getConvention().getPlugin(EarPluginConvention.class).getAppDirName() + "/META-INF/" + applicationName)
        if (applicationXmlFile.exists()) {
            config.addFile(applicationXmlFile.getCanonicalPath(), "/META-INF/application.xml");
        }
        else {
            //task.getProject().getConvention().getPlugin(EarPluginConvention.class).getDeploymentDescriptor().writeTo(task.destinationDir.getParentFile().getAbsolutePath() + applicationName)
            applicationXmlFile = new File(task.destinationDir.getParentFile().getAbsolutePath() + applicationName);
            config.addFile(applicationXmlFile.getCanonicalPath(), "/META-INF/application.xml");
        }
    }
    //addWarMoudule
    /*
    public Element addJarModule(Project proj) throws Exception {
        return addModule(proj, "gradle-jar-plugin");
    }
    
    public Element addEjbModule(Project proj) throws Exception {
        return addModule(proj, "gradle-ejb-plugin");
    }
    
    public Element addModule(Project proj, String pluginId) throws Exception {
        Element moduleArchive = config.addArchive("tempUntilIFigureItOut");
        config.addDir(moduleArchive, proj.compileJava.destinationDir, "/");
        // add manifest.mf
        addManifestFile(moduleArchive, proj, pluginId);
        return moduleArchive;
    }*/
    
    public Element addJarModule(Project proj, Dependency dep) throws Exception {
            return addModule(proj, "gradle-jar-plugin", dep.getName());
    }
    
    public Element addWarModule(Project proj, String warSourceDir, Dependency dep) throws Exception {
        Element warArchive = config.addArchive(dep.getName());
        config.addDir(warArchive, warSourceDir, "/");
        config.addDir(warArchive, proj.sourceSets.main.getOutput().getClassesDirs().getSingleFile().getAbsolutePath(), "/WEB-INF/classes");
        // add Manifest file
        addWarManifestFile(warArchive, proj);
        return warArchive;
    }
    
    public Element addModule(Project proj, String pluginId, String uri) throws Exception {
        Element moduleArchive = config.addArchive(uri);
        config.addDir(moduleArchive, proj.sourceSets.main.getOutput().getClassesDirs().getSingleFile().getAbsolutePath(), "/");
        // add manifest.mf
        File manifestFile = new File(proj.sourceSets.main.getOutput().getResourcesDir().getParentFile().getAbsolutePath() + "/META-INF/MANIFEST.MF")
        addManifestFile(moduleArchive, manifestFile, "gradle-jar-plugin")
        return moduleArchive;
    }
    
    public void addWarManifestFile(Element parent, Project proj) throws Exception {
        //config.addFile(parent, getManifestFile(proj, "gradle-war-plugin", "gradle-war-plugin"), "/META-INF/MANIFEST.MF");
    }
}
