package net.wasdev.wlp.gradle.plugins.loose

import net.wasdev.wlp.gradle.plugins.utils.LooseApplication
import net.wasdev.wlp.gradle.plugins.utils.LooseConfigData
import org.apache.commons.io.FilenameUtils
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.Task
import org.w3c.dom.Element

import java.nio.file.Paths
import java.text.MessageFormat
import java.util.regex.Matcher
import java.util.regex.Pattern

class LooseWarApplication extends LooseApplication {

    File appsDir
    LooseWarApplication(InstallDTO installDTO, LooseConfigData config, File appsDir) {
        super(installDTO.task, config)

        this.appsDir = appsDir
    }

    void addSourceDir() throws Exception {
        WarPluginConvention wpc = new WarPluginConvention(task.getProject())
        File sourceDir = new File(wpc.getWebAppDir().getAbsolutePath())
        config.addDir(sourceDir.getCanonicalPath(), "/")
    }

    void installLooseConfigWar(LooseConfigData config, InstallDTO installDTO) throws Exception {
        if (!appsDir.exists() && !installDTO.task.sourceSets.allJava.getSrcDirs().isEmpty()) {
            throw new GradleException(
                MessageFormat.format("Failed to install loose application from project {0}. The project has not been compiled.", project.name))
        }

//        LooseWarApplication looseWar = new LooseWarApplication(installDTO.task, config)
//        looseWar.addSourceDir()
//        looseWar.addOutputDir(looseWar.getDocumentRoot(), installDTO.task, "/WEB-INF/classes/")
        addSourceDir()
        addOutputDir(getDocumentRoot(), installDTO.task, "/WEB-INF/classes/")

        //retrieves dependent library jar files
        addWarEmbeddedLib(getDocumentRoot(), this, installDTO.task)

        //add Manifest file
        File manifestFile = Paths.get(installDTO.parentProject.sourceSets.main.getOutput().getResourcesDir()
            .getParentFile().getAbsolutePath(), "/META-INF/MANIFEST.MF").toFile()

//        addManifestFile(manifestFile, "gradle-war-plugin")
        addManifestFile(manifestFile)
    }

    private void addWarEmbeddedLib(Element parent, LooseWarApplication looseApp, Task task) throws Exception {
        ArrayList<File> deps = new ArrayList<File>()
        task.classpath.each { deps.add(it) }
        //Removes WEB-INF/lib/main directory since it is not rquired in the xml
        if (deps != null && !deps.isEmpty()) {
            deps.remove(0)
        }
        File parentProjectDir = new File(task.getProject().getRootProject().rootDir.getAbsolutePath())
        for (File dep : deps) {
            String dependentProjectName = "project ':${getProjectPath(parentProjectDir, dep)}'"
            Project siblingProject = task.project.getRootProject().findProject(dependentProjectName)
            boolean isCurrentProject = ((task.getProject().toString()).equals(dependentProjectName))
            if (!isCurrentProject && siblingProject != null) {
                Element archive = looseApp.addArchive(parent, "/WEB-INF/lib/" + dep.getName());
                looseApp.addOutputDirectory(archive, siblingProject, "/")
                Task resourceTask = siblingProject.getTasks().findByPath(":" + dependentProjectName + ":processResources")
                if (resourceTask.getDestinationDir() != null) {
                    looseApp.addOutputDir(archive, resourceTask.getDestinationDir(), "/")
                }
                looseApp.addManifestFile(archive, siblingProject, "gradle-jar-plugin");
            } else if (FilenameUtils.getExtension(dep.getAbsolutePath()).equalsIgnoreCase("jar")) {
                looseApp.getConfig().addFile(parent, dep.getAbsolutePath(), "/WEB-INF/lib/" + dep.getName())
            } else {
                looseApp.addOutputDir(looseApp.getDocumentRoot(), dep.getAbsolutePath(), "/WEB-INF/classes/")
            }
        }
    }

    String getProjectPath(File parentProjectDir, File dep) {
        String dependencyPathPortion = dep.getAbsolutePath().replace(parentProjectDir.getAbsolutePath() + "/", "")
        String projectPath = dep.getAbsolutePath().replace(dependencyPathPortion, "")
        Pattern pattern = Pattern.compile("/build/.*")
        Matcher matcher = pattern.matcher(dependencyPathPortion)
        projectPath = matcher.replaceAll("")
        return projectPath
    }
}
