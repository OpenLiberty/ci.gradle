package io.openliberty.tools.gradle.utils;

import io.openliberty.tools.common.plugins.config.LooseApplication
import io.openliberty.tools.common.plugins.config.LooseConfigData
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.bundling.War

import java.nio.file.Path

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

    /**
     * get web app source directories
     * @param project
     * @return
     */
    public static List<Path> getWebSourceDirectoriesToMonitor(Project project) {
        List<Path> retVal = new ArrayList<Path>();
        Task warTask = project.getTasks().findByName('war')
        if (warTask != null) {
            setWarSourceDir(warTask, retVal)
        } else if (project.configurations.deploy != null) {
            setWarSourceDirForDeployDependencies(project, retVal)
        } else {
            retVal.add("src/main/webapp")
        }
        return retVal;
    }
    /**
     * find war deploy dependencies and add source dir
     * @param project
     * @param retVal
     */
    private static void setWarSourceDirForDeployDependencies(Project project, ArrayList<Path> retVal) {
        Task warTask
        HashMap<File, Dependency> completeDeployDeps = DevTaskHelper.getDeployDependencies(project)
        for (Map.Entry<File, Dependency> entry : completeDeployDeps) {
            Dependency dependency = entry.getValue();
            File dependencyFile = entry.getKey();

            if (dependency instanceof ProjectDependency) {
                Project dependencyProject = dependency.getDependencyProject()
                String projectType = FilenameUtils.getExtension(dependencyFile.toString())
                switch (projectType) {
                    case "war":
                        warTask = dependencyProject.getTasks().findByName('war')
                        if (warTask != null) {
                            setWarSourceDir(warTask, retVal)
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private static void setWarSourceDir(Task warTask, ArrayList<Path> retVal) {
        War war = (War) warTask.getProject().war
        if (war.getWebAppDirectory().getAsFile().get() != null) {
            retVal.add(war.getWebAppDirectory().get().asFile.toPath())
        }
    }
}
