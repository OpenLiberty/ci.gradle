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
import org.gradle.api.logging.Logger
import org.gradle.plugins.ear.Ear
import org.w3c.dom.Element

import java.util.jar.Manifest

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
        Element moduleArchive = config.addArchive("/" + proj.jar.getArchiveFileName().get());
        proj.sourceSets.main.getOutput().getClassesDirs().each{config.addDir(moduleArchive, it, "/");}
        if (resourcesDirContentsExist(proj)) {
            config.addDir(moduleArchive, proj.sourceSets.main.getOutput().getResourcesDir(), "/");
        }
        addModules(moduleArchive, proj)
        return moduleArchive;
    }

    private void addModules(Element moduleArchive, Project proj) {
        boolean manifestAdded = false
        for (File f : proj.jar.source.getFiles()) {
            String extension = FilenameUtils.getExtension(f.getAbsolutePath())
            switch(extension) {
                case "jar":
                case "war":
                case "rar":
                    config.addFile(moduleArchive, f, "/WEB-INF/lib/" + f.getName());
                    break
                case "MF":
                    // Prefer the jar task's generated manifest (build/tmp/jar/MANIFEST.MF) which has
                    // the correct Class-Path entries from jar { manifest { attributes } } in build.gradle.
                    // This avoids incomplete Class-Path entries in any static MANIFEST.MF in resources.
                    File jarTaskManifest = new File(proj.jar.temporaryDir, "MANIFEST.MF")
                    if (jarTaskManifest.exists()) {
                        config.addFile(moduleArchive, jarTaskManifest, "/META-INF/MANIFEST.MF")
                        // Parse Class-Path and add dependency class directories to module archive
                        addManifestClassPathDependencies(moduleArchive, jarTaskManifest, proj)
                    } else {
                        addManifestFileWithParent(moduleArchive, f, proj.sourceSets.main.getOutput().getResourcesDir().getParentFile().getCanonicalPath())
                    }
                    manifestAdded = true
                    break
                default:
                    break
            }
        }
        // If no .MF file was found in the jar source set (e.g. no static MANIFEST.MF in resources),
        // still add the jar task's generated manifest if it exists.
        if (!manifestAdded) {
            File jarTaskManifest = new File(proj.jar.temporaryDir, "MANIFEST.MF")
            if (jarTaskManifest.exists()) {
                config.addFile(moduleArchive, jarTaskManifest, "/META-INF/MANIFEST.MF")
                // Parse Class-Path and add dependency class directories to module archive
                addManifestClassPathDependencies(moduleArchive, jarTaskManifest, proj)
            }
        }
    }

    private void addManifestClassPathDependencies(Element moduleArchive, File manifestFile, Project proj) {
        try {
            def manifest = new Manifest(new FileInputStream(manifestFile))
            String classPath = manifest.getMainAttributes().getValue("Class-Path")
            logger.info("Processing manifest Class-Path for ${proj.name}: ${classPath}")
            if (classPath) {
                classPath.split(/\s+/).each { String jarName ->
                    if (jarName && jarName.endsWith(".jar")) {
                        String depName = jarName.replace(".jar", "")

                        def depProj = proj.rootProject.findProject(":${depName}")
                        logger.info("Looking for dependency project: ${depName}, found: ${depProj != null}")

                        if (depProj && depProj.hasProperty('sourceSets')) {
                            depProj.sourceSets.main.output.classesDirs.files.each {
                                File classesDir ->
                                    if (classesDir.exists()) {
                                        logger.info("Adding dependency class dir to ${proj.name}: ${classesDir}")
                                        config.addDir(moduleArchive, classesDir, "/")
                                    }
                            }

                            def resourcesDir = depProj.sourceSets.main.output.resourcesDir
                            if (resourcesDir && resourcesDir.exists()) {
                                logger.info("Adding dependency resource dir to ${proj.name}: ${resourcesDir}")
                                config.addDir(moduleArchive, resourcesDir, "/")
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not parse manifest Class-Path from ${manifestFile}: ${e.message}", e)
        }
    }

}
