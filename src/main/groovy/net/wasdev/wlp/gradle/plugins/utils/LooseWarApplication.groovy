package net.wasdev.wlp.gradle.plugins.utils;

import java.io.File;
import org.gradle.api.Project;

public class LooseWarApplication extends LooseApplication {

    public LooseWarApplication(Project project, LooseConfigData config) {
        super(project, config);
    }

    public void addSourceDir() throws Exception {
        File sourceDir = new File(project.war.webAppDir.getAbsolutePath(), "src/main/webapp");
        config.addDir(sourceDir.getCanonicalPath(), "/");
    }


}
