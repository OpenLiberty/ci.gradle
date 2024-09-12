package io.openliberty.tools.gradle.utils;

import io.openliberty.tools.common.plugins.config.LooseApplication
import io.openliberty.tools.common.plugins.config.LooseConfigData
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.bundling.War

public class LooseWarApplication extends LooseApplication {
    
    protected Task task;
    protected Logger logger;

    public LooseWarApplication(Task task, LooseConfigData config, Logger logger) {
        super(task.getProject().getLayout().getBuildDirectory().getAsFile().get().getAbsolutePath(), config)
        this.task = task
        this.logger = logger
    }

    public void addSourceDir() throws Exception {

        War war;
        File sourceDir = new File("src/main/webapp")
        if (task.getProject().getPlugins().hasPlugin("war")) {
            war = (War) task.getProject().war
            if (war.getWebAppDirectory().getAsFile().get() != null) {
                sourceDir = new File(war.getWebAppDirectory().getAsFile().get().getAbsolutePath())
            }
        }else {
            logger.warn("Could not get the webAppDirectory location from the WAR plugin because it is not configured. Using the default location src/main/webapp instead. Application may not work as expected.")
        }
        config.addDir(sourceDir, "/")
    }

}
