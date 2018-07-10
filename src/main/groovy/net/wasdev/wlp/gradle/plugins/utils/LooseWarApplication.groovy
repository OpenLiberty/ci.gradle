package net.wasdev.wlp.gradle.plugins.utils;

import java.io.File;
import org.gradle.api.Project;
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.War

public class LooseWarApplication extends LooseApplication {

    public LooseWarApplication(Task task, LooseConfigData config) {
        super(task, config);
    }

    public void addSourceDir() throws Exception {
        Project project = task.getProject()
        WarPluginConvention wpc = new WarPluginConvention(project)

        if (project.webAppDirName != null) {
            wpc.setWebAppDirName(project.webAppDirName)
        }

        File sourceDir = new File(wpc.getWebAppDir().getAbsolutePath());
        config.addDir(sourceDir.getCanonicalPath(), "/");
    }


}
