package net.wasdev.wlp.gradle.plugins.utils;

import java.io.File;
import org.gradle.api.Project;
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.War

import io.openliberty.tools.common.plugins.config.LooseConfigData
import io.openliberty.tools.common.plugins.util.PluginExecutionException
import io.openliberty.tools.common.plugins.config.LooseApplication

public class LooseWarApplication extends LooseApplication {
    
    protected Task task;

    public LooseWarApplication(Task task, LooseConfigData config) {
        super(task.getProject().getBuildDir().getAbsolutePath(), config)
        this.task = task
    }

    public void addSourceDir() throws Exception {
        WarPluginConvention wpc = task.getProject().getConvention().findPlugin(WarPluginConvention)
        File sourceDir = new File(wpc.getWebAppDir().getAbsolutePath())
        config.addDir(sourceDir, "/")
    }

}
