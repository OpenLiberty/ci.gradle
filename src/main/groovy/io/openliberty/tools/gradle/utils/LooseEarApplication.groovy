package io.openliberty.tools.gradle.utils

import java.io.File
import org.gradle.api.Project
import org.gradle.plugins.ear.EarPluginConvention
import org.gradle.api.Task
import org.gradle.plugins.ear.Ear
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.artifacts.Dependency
import org.w3c.dom.Element;
import org.apache.commons.io.FilenameUtils

import io.openliberty.tools.common.plugins.config.LooseConfigData
import io.openliberty.tools.common.plugins.util.PluginExecutionException
import io.openliberty.tools.common.plugins.config.LooseApplication

public class LooseEarApplication extends LooseApplication {
    
    protected Task task;

    public LooseEarApplication(Task task, LooseConfigData config) {
        super(task.getProject().getBuildDir().getAbsolutePath(), config)
        this.task = task
    }

    public void addSourceDir() throws Exception {
        File sourceDir = new File(task.getProject().path.replace(":","") + "/" + task.getProject().getConvention().getPlugin(EarPluginConvention.class).getAppDirName())
        config.addDir(sourceDir, "/")
    }

    public void addApplicationXmlFile() throws Exception {
        String applicationName = "/" + task.getProject().getConvention().getPlugin(EarPluginConvention.class).getDeploymentDescriptor().getFileName()
        File applicationXmlFile = new File(task.getProject().path.replace(":","") + "/" + task.getProject().getConvention().getPlugin(EarPluginConvention.class).getAppDirName() + "/META-INF/" + applicationName)
        if (applicationXmlFile.exists()) {
            config.addFile(applicationXmlFile, "/META-INF/application.xml");
        }
        else {
            applicationXmlFile = new File(task.getDestinationDirectory().get().getAsFile().getParentFile().getAbsolutePath() + "/tmp/ear" + applicationName);
            config.addFile(applicationXmlFile, "/META-INF/application.xml");
        }
    }
    
    public Element addWarModule(Project proj) throws Exception {
        Element warArchive = config.addArchive("/" + proj.war.getArchiveFileName().get());
        proj.sourceSets.main.getOutput().getClassesDirs().each{config.addDir(warArchive, it, "/WEB-INF/classes");}
        addModules(warArchive,proj)
        return warArchive;
    }
    
    public Element addJarModule(Project proj) throws Exception {
        Element moduleArchive = config.addArchive("/" + proj.jar.getArchiveFileName().get());
        proj.sourceSets.main.getOutput().getClassesDirs().each{config.addDir(moduleArchive, it, "/");}
        addModules(moduleArchive, proj)
        return moduleArchive;
    }
    
    private void addModules(Element moduleArchive, Project proj) {
        for (File f : proj.jar.source.getFiles()) {
            String extension = FilenameUtils.getExtension(f.getAbsolutePath())
            switch(extension) {
                case "jar":
                case "war":
                case "rar":
                    config.addFile(moduleArchive, f, "/WEB-INF/lib/" + f.getName());
                    break
                case "MF":
                    //This checks the manifest file and resource directory of the project's jar source set.
                    //The location of the resource directory should be the same as proj.getProjectDir()/build/resources.
                    //If the manifest file exists, it is copied to proj.getProjectDir()/build/resources/tmp/META-INF. If it does not exist, one is created there.
                    addManifestFileWithParent(moduleArchive, f, proj.sourceSets.main.getOutput().getResourcesDir().getParentFile().getCanonicalPath())
                    break
                default:
                    break
            }
        }
    }
    
}
