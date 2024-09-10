package io.openliberty.tools.gradle.utils

import io.openliberty.tools.common.plugins.config.LooseApplication
import io.openliberty.tools.common.plugins.config.LooseConfigData
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.plugins.ear.Ear
import org.w3c.dom.Element

public class LooseEarApplication extends LooseApplication {
    
    protected Task task;
    protected Logger logger;

    public LooseEarApplication(Task task, LooseConfigData config, Logger logger) {
        super(task.getProject().getLayout().getBuildDirectory().getAsFile().get().getAbsolutePath(), config)
        this.task = task
        this.logger = logger
    }

    public void addSourceDir() throws Exception {
        if (task.getProject().getPlugins().hasPlugin("ear")) {
            Ear ear = (Ear) task.getProject().ear
            File sourceDir = new File(task.getProject().path.replace(":","") + "/" + ear.getAppDirectory().getAsFile().get().getAbsolutePath())
            config.addDir(sourceDir, "/")
        }
    }

    public void addApplicationXmlFile() throws Exception {
        if (task.getProject().getPlugins().hasPlugin("ear")) {
            Ear ear = (Ear) task.getProject().ear
            String applicationName = "/application.xml"
            if (ear.getDeploymentDescriptor() != null){
                applicationName = "/" + ear.getDeploymentDescriptor().getFileName()
            }
            File applicationXmlFile = new File(task.getProject().path.replace(":", "") + "/" + ear.getAppDirectory().getAsFile().get().getAbsolutePath() + "/META-INF/" + applicationName)
            if (applicationXmlFile.exists()) {
                config.addFile(applicationXmlFile, "/META-INF/application.xml");
            } else {
                applicationXmlFile = new File(task.getDestinationDirectory().get().getAsFile().getParentFile().getAbsolutePath() + "/tmp/ear" + applicationName);
                config.addFile(applicationXmlFile, "/META-INF/application.xml");
            }
        }
        else {
            logger.warn("Project is expected to have EAR plugin. Application xml file may not be added correctly")
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
