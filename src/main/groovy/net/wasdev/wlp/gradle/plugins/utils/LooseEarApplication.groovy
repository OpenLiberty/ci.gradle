package net.wasdev.wlp.gradle.plugins.utils

import org.gradle.api.Project
import org.gradle.plugins.ear.EarPluginConvention
import org.gradle.api.Task
import org.w3c.dom.Element;
import org.apache.commons.io.FilenameUtils


class LooseEarApplication extends LooseApplication {

    LooseEarApplication(Task task, LooseConfigData config) {
        super(task, config);
    }

    void addSourceDir() throws Exception {
        File sourceDir = new File(task.getProject().path.replace(":","") + "/" + task.getProject().getConvention().getPlugin(EarPluginConvention.class).getAppDirName())
        config.addDir(sourceDir.getCanonicalPath(), "/")
    }

    void addApplicationXmlFile() throws Exception {
        String applicationName = "/" + task.getProject().getConvention().getPlugin(EarPluginConvention.class).getDeploymentDescriptor().getFileName()
        File applicationXmlFile = new File(task.getProject().path.replace(":","") + "/" + task.getProject().getConvention().getPlugin(EarPluginConvention.class).getAppDirName() + "/META-INF/" + applicationName)
        if (applicationXmlFile.exists()) {
            config.addFile(applicationXmlFile.getCanonicalPath(), "/META-INF/application.xml")
        }
        else {
            applicationXmlFile = new File(task.destinationDir.getParentFile().getAbsolutePath() + "/tmp/ear" + applicationName)
            config.addFile(applicationXmlFile.getCanonicalPath(), "/META-INF/application.xml")
        }
    }
    
    Element addWarModule(Project proj) throws Exception {
        Element warArchive = config.addArchive("/" + proj.war.archiveName);
        proj.sourceSets.main.getOutput().getClassesDirs().each{
            config.addDir(warArchive, it.getAbsolutePath(), "/WEB-INF/classes")
        }
        addModules(warArchive,proj)
        return warArchive
    }
    
    Element addJarModule(Project proj) throws Exception {
        Element moduleArchive = config.addArchive("/" + proj.jar.archiveName)
        proj.sourceSets.main.getOutput().getClassesDirs().each{config.addDir(moduleArchive, it.getAbsolutePath(), "/WEB-INF/classes")}
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
                    config.addFile(moduleArchive, f.getAbsolutePath(), "/WEB-INF/lib/" + f.getName())
                    break
                case "MF":
                    config.addFile(moduleArchive, f.getAbsolutePath(), "/META-INF/MANIFEST.MF")
                    break
                default:
                    break
            }
        }
    }
    
}
