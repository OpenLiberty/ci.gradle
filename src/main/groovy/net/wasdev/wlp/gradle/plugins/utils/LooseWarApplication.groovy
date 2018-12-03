package net.wasdev.wlp.gradle.plugins.utils;

import java.io.File;
import org.gradle.api.Project;
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.War

import net.wasdev.wlp.common.plugins.config.LooseConfigData

public class LooseWarApplication extends LooseApplication {

    public LooseWarApplication(Task task, LooseConfigData config) {
        super(task, config)
    }

    public void addSourceDir() throws Exception {
        WarPluginConvention wpc = task.getProject().getConvention().findPlugin(WarPluginConvention)
        File sourceDir = new File(wpc.getWebAppDir().getAbsolutePath())
        config.addDir(sourceDir.getCanonicalPath(), "/")
    }


}
