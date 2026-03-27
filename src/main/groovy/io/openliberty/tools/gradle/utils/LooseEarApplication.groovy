/**
 * (C) Copyright IBM Corporation 2018, 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.tools.gradle.utils

import io.openliberty.tools.common.plugins.config.LooseApplication
import io.openliberty.tools.common.plugins.config.LooseConfigData
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.logging.Logger
import org.gradle.plugins.ear.Ear
import org.w3c.dom.Element

public class LooseEarApplication extends LooseApplication {
    
    protected Task task;
    protected Logger logger;

    public LooseEarApplication(Task task, LooseConfigData config, Logger logger) {
        super(task.getProject().getLayout().getBuildDirectory().getAsFile().get().getAbsolutePath(), config)
        this.task = task
        this.logger = logger
    }

    public void addSourceDir() throws Exception {
        if (task.getProject().getPlugins().hasPlugin("ear")) {
            Ear ear = (Ear) task.getProject().ear
            File sourceDir = new File(task.getProject().path.replace(":","") + "/" + ear.getAppDirectory().getAsFile().get().getPath())
            config.addDir(sourceDir, "/")
        }
    }

    public void addApplicationXmlFile() throws Exception {
        String applicationName = "/application.xml"
        File applicationXmlFile;
        if (task.getProject().getPlugins().hasPlugin("ear")) {
            Ear ear = (Ear) task.getProject().ear
            if (ear.getDeploymentDescriptor() != null) {
                applicationName = "/" + ear.getDeploymentDescriptor().getFileName()
            }
            applicationXmlFile = new File(task.getProject().path.replace(":", "") + "/" + ear.getAppDirectory().getAsFile().get().getAbsolutePath() + "/META-INF/" + applicationName)
            if (applicationXmlFile.exists()) {
                config.addFile(applicationXmlFile, "/META-INF/application.xml")
            }
        }
        if (applicationXmlFile == null || !applicationXmlFile.exists()) {
            applicationXmlFile = new File(task.getDestinationDirectory().get().getAsFile().getParentFile().getAbsolutePath() + "/tmp/ear" + applicationName);
            config.addFile(applicationXmlFile, "/META-INF/application.xml")
        }
    }
    
    public Element addWarModule(Project proj) throws Exception {
        Element warArchive = config.addArchive("/" + proj.war.getArchiveFileName().get());
        if (proj.war.getWebAppDirectory().getAsFile().get() != null) {
            var sourceDir = new File(proj.war.getWebAppDirectory().getAsFile().get().getAbsolutePath())
            config.addDir(warArchive,sourceDir,"/")
        }
        proj.sourceSets.main.getOutput().getClassesDirs().each{config.addDir(warArchive, it, "/WEB-INF/classes");}
        if (resourcesDirContentsExist(proj)) {
            config.addDir(warArchive, proj.sourceSets.main.getOutput().getResourcesDir(), "/WEB-INF/classes");
        }
        addModules(warArchive,proj)
        return warArchive;
    }

    /**
     * checks whether any resource exists in output resources/main directory
     * @param proj current project
     * @return
     */
    protected static boolean resourcesDirContentsExist(Project proj) {
        def resourcesDir = proj.sourceSets.main.getOutput().getResourcesDir()

        // Check if it's a directory, and then check the 'list' array for emptiness
        // (In Groovy, a non-empty array evaluates to true in a boolean context)
        return resourcesDir.isDirectory() && resourcesDir.list()
    }

    public Element addJarModule(Project proj) throws Exception {
        logger.debug("Adding JAR module for project: ${proj.name}")
        Element moduleArchive = config.addArchive("/" + proj.jar.getArchiveFileName().get());
        proj.sourceSets.main.getOutput().getClassesDirs().each{config.addDir(moduleArchive, it, "/");}
        if (resourcesDirContentsExist(proj)) {
            config.addDir(moduleArchive, proj.sourceSets.main.getOutput().getResourcesDir(), "/");
        }
        addModules(moduleArchive, proj)
        
        addDependencyClassDirectories(moduleArchive, proj)
        
        return moduleArchive;
    }
    
    private void addModules(Element moduleArchive, Project proj) {
        for (File f : proj.jar.source.getFiles()) {
            String extension = FilenameUtils.getExtension(f.getAbsolutePath())
            switch(extension) {
                case "jar":
                case "war":
                case "rar":
                    config.addFile(moduleArchive, f, "/WEB-INF/lib/" + f.getName());
                    break
                case "MF":
                    //This checks the manifest file and resource directory of the project's jar source set.
                    //The location of the resource directory should be the same as proj.getProjectDir()/build/resources.
                    //If the manifest file exists, it is copied to proj.getProjectDir()/build/resources/tmp/META-INF. If it does not exist, one is created there.
                    addManifestFileWithParent(moduleArchive, f, proj.sourceSets.main.getOutput().getResourcesDir().getParentFile().getCanonicalPath())
                    break
                default:
                    break
            }
        }
    }

    /**
     * Add dependency class directories so EJB modules can see classes from their dependencies
     * @param moduleArchive
     * @param proj
     */
    private void addDependencyClassDirectories(Element moduleArchive, Project proj) {
        try {
            Set<Project> projectDependencies = new HashSet<Project>();
            
            // Check compileClasspath for compile-time dependencies
            if (proj.configurations.findByName('compileClasspath') != null) {
                proj.configurations.compileClasspath.allDependencies.each { dep ->
                    if (dep instanceof ProjectDependency) { // Ensure it's a project dependency
                        Project depProj = proj.rootProject.findProject(dep.path)
                        if (depProj != null) {
                            projectDependencies.add(depProj)
                        }
                    }
                }
            }
            
            // Check runtimeClasspath for runtime dependencies
            if (proj.configurations.findByName('runtimeClasspath') != null) {
                proj.configurations.runtimeClasspath.allDependencies.each { dep ->
                    if (dep instanceof ProjectDependency) { // Ensure it's a project dependency
                        Project depProj = proj.rootProject.findProject(dep.path)
                        if (depProj != null) {
                            projectDependencies.add(depProj)
                        }
                    }
                }
            }
            
            logger.debug("Found ${projectDependencies.size()} project dependencies for ${proj.name}")
            
            // Process each project dependency
            projectDependencies.each { dependencyProject ->
                if (!dependencyProject.hasProperty('sourceSets')) { // Not a Java project
                    logger.debug("Skipping ${dependencyProject.name} - no sourceSets found (not a Java project)")
                    return
                }
                
                logger.debug("Adding dependency ${dependencyProject.name} class directories to ${proj.name}")
                
                // Add all class directories
                dependencyProject.sourceSets.main.output.classesDirs.files.each { File classesDirectory ->
                    if (classesDirectory.exists()) {
                        logger.debug("  Adding class dir: ${classesDirectory}")
                        config.addDir(moduleArchive, classesDirectory, "/")
                    } else {
                        logger.debug("  Skipping non-existent class dir: ${classesDirectory}")
                    }
                }
                
                // Add resource directory
                def resourcesDirectory = dependencyProject.sourceSets.main.output.resourcesDir
                if (resourcesDirectory?.exists()) {
                    logger.debug("  Adding resource dir: ${resourcesDirectory}")
                    config.addDir(moduleArchive, resourcesDirectory, "/")
                } else {
                    logger.debug("  No resources dir or doesn't exist: ${resourcesDirectory}")
                }
            }
        } catch (Exception e) {
            logger.warn("Could not add dependency class directories for ${proj.name}: ${e.message}", e)
        }
    }
    
}
