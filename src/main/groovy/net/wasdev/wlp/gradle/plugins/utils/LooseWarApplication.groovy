package net.wasdev.wlp.gradle.plugins.utils;

import java.io.File;
import org.gradle.api.Project;
import org.gradle.api.plugins.WarPluginConvention

public class LooseWarApplication extends LooseApplication {

    public LooseWarApplication(Project project, LooseConfigData config) {
        super(project, config);
    }

    public void addSourceDir() throws Exception {
        WarPluginConvention wpc = new WarPluginConvention(project)
        File sourceDir = new File(wpc.getWebAppDir().getAbsolutePath());
        config.addDir(sourceDir.getCanonicalPath(), "/");
    }


}
