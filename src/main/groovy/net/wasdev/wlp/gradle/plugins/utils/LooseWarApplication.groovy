package net.wasdev.wlp.gradle.plugins.utils;

import java.io.File;
import org.gradle.api.Project;
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.plugins.War
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.War

public class LooseWarApplication extends LooseApplication {

    public LooseWarApplication(Task task, LooseConfigData config) {
        super(task, config)
    }

    public void addSourceDir() throws Exception {
        WarPluginConvention wpc = new WarPluginConvention(task.getProject())
        System.out.println("\n::::project " + task.getProject().war.classpath.getAsPath())
        // System.out.println("\n::::War.webAppDir " + task.War.getWebAppDir().getAbsolutePath());
        File sourceDir = new File(wpc.getWebAppDir().getAbsolutePath())
        config.addDir(sourceDir.getCanonicalPath(), "/")
    }


}
