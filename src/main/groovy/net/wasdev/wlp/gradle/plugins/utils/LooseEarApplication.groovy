package net.wasdev.wlp.gradle.plugins.utils

import java.io.File
import org.gradle.api.Project
import org.gradle.plugins.ear.EarPluginConvention
import org.gradle.api.Task
import org.gradle.plugins.ear.Ear
import org.gradle.api.internal.file.FileResolver

public class LooseEarApplication extends LooseApplication {

    public LooseEarApplication(Task task, LooseConfigData config) {
        super(task, config);
    }

    public void addSourceDir() throws Exception {
        File sourceDir = new File(task.destinationDir.getAbsolutePath() + task.getProject().getConvention().getPlugin(EarPluginConvention.class).getAppDirName())
        config.addDir(sourceDir.getAbsolutePath(), "/")
    }

    public void addApplicationXmlFile() throws Exception {
        String applicationName = "/" + task.getProject().getConvention().getPlugin(EarPluginConvention.class).getDeploymentDescriptor().getFileName()
        task.getProject().getConvention().getPlugin(EarPluginConvention.class).getDeploymentDescriptor().writeTo(task.destinationDir.getParentFile().getAbsolutePath() + applicationName)
        File applicationXmlFile = new File(task.destinationDir.getParentFile().getAbsolutePath() + applicationName);
        config.addFile(applicationXmlFile.getCanonicalPath(), "/META-INF/application.xml");
    }

}
