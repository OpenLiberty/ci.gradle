/**
 * (C) Copyright IBM Corporation 2021, 2025.
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
package io.openliberty.tools.gradle.tasks


import io.openliberty.tools.common.plugins.config.ServerConfigXmlDocument
import io.openliberty.tools.common.plugins.config.XmlDocument
import io.openliberty.tools.common.plugins.util.BinaryScannerUtil
import static io.openliberty.tools.common.plugins.util.BinaryScannerUtil.*;
import io.openliberty.tools.common.plugins.util.GenerateFeaturesUtil;
import io.openliberty.tools.common.plugins.util.GenerateFeaturesUtil.GenerateFeaturesException;
import io.openliberty.tools.common.plugins.util.PluginExecutionException
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil.FeaturesPlatforms
import io.openliberty.tools.gradle.utils.ArtifactDownloadUtil

import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.logging.LogLevel
import org.xml.sax.SAXException
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.TransformerException

class GenerateFeaturesTask extends AbstractFeatureTask {

    // Default value of the optimize task option
    private static final boolean DEFAULT_OPTIMIZE = true;
    // Default value of the generateToSrc option
    private static final boolean DEFAULT_GENERATETOSRC = false;

    // The executable file used to scan binaries for the Liberty features they use.
    private File binaryScanner;

    GenerateFeaturesTask() {
        configure({
            description = 'Generate the features used by an application and add to the configuration of a Liberty server'
            group = 'Liberty'
        })
    }

    private List<String> classFiles;

    @Option(option = 'classFile', description = 'If set and optimize is false, will generate features for the list of classes passed.')
    void setClassFiles(List<String> classFiles) {
        this.classFiles = classFiles;
    }

    private Boolean optimize = null;

    // Need to use a string value to allow the ability to specify a value for the parameter (ie. --optimize=false)
    @Option(option = 'optimize', description = 'Optimize generating features by passing in all classes and only user specified features.')
    void setOptimize(String optimize) {
        this.optimize = Boolean.parseBoolean(optimize);
    }

    private Boolean generateToSrc = null;

    // Need to use a string value to allow the ability to specify a value for the parameter (ie. --generateToSrc=false)
    @Option(option = 'generateToSrc', description = 'Generate features to a file in the project\'s src directory')
    void setGenerateToSrc(String generateToSrc) {
        this.generateToSrc = Boolean.parseBoolean(generateToSrc);
    }

    @TaskAction
    void generateFeatures() {
        binaryScanner = getBinaryScannerJarFromRepository();
        BinaryScannerHandler binaryScannerHandler = new BinaryScannerHandler(binaryScanner);
        logger.debug("Binary scanner jar: " + binaryScanner.getName());

        if (optimize == null) {
            optimize = DEFAULT_OPTIMIZE;
        }
        if (generateToSrc == null) {
            generateToSrc = DEFAULT_GENERATETOSRC;
        }

        initializeConfigDirectory();

        GenerateFeaturesHandler generateFeaturesHandler = new GenerateFeaturesHandler(project,
                binaryScannerHandler, server.configDirectory, getServerDir(project), classFiles, GenerateFeaturesUtil.HEADER_G);
        try {
            generateFeaturesHandler.generateFeatures(optimize, generateToSrc);
        } catch (GenerateFeaturesException e) {
            throw new GradleException(e.getMessage(), e.getCause());
        }
    }

    // Get the features from the server config and optionally exclude the specified config files from the search.
    private Set<String> getServerFeaturesGradle(ServerFeatureUtil servUtil, File generationContextDir, Set<String> generatedFiles, boolean excludeGenerated) {
        servUtil.setLowerCaseFeatures(false);
        // if optimizing, ignore generated files when passing in existing features to binary scanner
        FeaturesPlatforms fp = servUtil.getServerFeatures(generationContextDir, server.serverXmlFile, new HashMap<String, File>(), excludeGenerated ? generatedFiles : null); // pass generatedFiles to exclude them
        Set<String> existingFeatures = fp == null ? new HashSet<String>() : fp.getFeatures();

        servUtil.setLowerCaseFeatures(true);
        return existingFeatures;
    }

    /**
     * Gets the binary scanner jar file from the local cache.
     * Downloads it first from connected repositories such as Maven Central if a newer release is available than the cached version.
     * Note: Maven updates artifacts daily by default based on the last updated timestamp. Users should use 'mvn -U' to force updates if needed.
     *
     * @return The File object of the binary-app-scanner.jar in the local cache.
     * @throws PluginExecutionException indicates the binary-app-scanner.jar could not be found
     */
    private File getBinaryScannerJarFromRepository() throws PluginExecutionException {
        try {
            return ArtifactDownloadUtil.downloadBuildArtifact(project, BINARY_SCANNER_MAVEN_GROUP_ID, BINARY_SCANNER_MAVEN_ARTIFACT_ID, BINARY_SCANNER_MAVEN_TYPE, BINARY_SCANNER_MAVEN_VERSION);
        } catch (Exception e) {
            throw new PluginExecutionException("Could not retrieve the artifact " + BINARY_SCANNER_MAVEN_GROUP_ID + "."
                    + BINARY_SCANNER_MAVEN_ARTIFACT_ID
                    + " needed for generateFeatures. Ensure you have a connection to Maven Central or another repository that contains the "
                    + BINARY_SCANNER_MAVEN_GROUP_ID + "." + BINARY_SCANNER_MAVEN_ARTIFACT_ID
                    + ".jar configured in your build.gradle.",
                    e);
        }
    }

    private Set<String> getClassesDirectoriesGradle() {
        Set<String> classesDirectories = new ArrayList<String>();
        project.sourceSets.main.getOutput().getClassesDirs().each {
            if (it.exists()) {
                classesDirectories.add(it.getAbsolutePath());
            }
        }
        return classesDirectories;
    }

    /**
     * Return the latest EE major version detected in the project dependencies
     *
     * @param project
     * @return latest EE major version corresponding to the EE umbrella dependency, null if an EE umbrella dependency is not found
     */
    protected getEEVersion(Object project) {
        String eeVersion = null
        project.configurations.compileClasspath.allDependencies.each {
            dependency ->
                if ((dependency.group.equals("javax") && dependency.name.equals("javaee-api")) ||
                    (dependency.group.equals("jakarta.platform") &&
                        dependency.name.equals("jakarta.jakartaee-api"))) {
                    String newVersion = dependency.version
                    if (newVersion != null && isLatestVersion(eeVersion, newVersion)) {
                        eeVersion = newVersion
                    }
                }
        }
        return eeVersion;
    }
    /**
     * Returns the latest MicroProfile major version detected in the project dependencies
     *
     * @param project
     * @return latest MP major version corresponding to the MP umbrella dependency, null if an MP umbrella dependency is not found
     */
    protected getMPVersion(Object project) {
        String mpVersion = null
        project.configurations.compileClasspath.allDependencies.each {
            dependency ->
                if (dependency.group.equals("org.eclipse.microprofile") &&
                        dependency.name.equals("microprofile")) {
                    String newVersion = dependency.version
                    if (newVersion != null && isLatestVersion(mpVersion, newVersion)) {
                        mpVersion = newVersion;
                    }
                }
        }
        return mpVersion;
    }

    // Return true if the newVer > currentVer
    protected static boolean isLatestVersion(String currentVer, String newVer) {
        if (currentVer == null || currentVer.isEmpty())  {
            return true;
        }
        // Comparing versions: mp4 > mp3.3 > mp3.0 > mp3
        return (currentVer.compareTo(newVer) < 0);
    }

    private class GenerateFeaturesHandler extends GenerateFeaturesUtil {
        public GenerateFeaturesHandler(Object project, BinaryScannerUtil binaryScannerHandler, File configDirectory, File serverDirectory, List<String> classFiles, String header) {
            super(project, binaryScannerHandler, configDirectory, serverDirectory, classFiles, header);
        }
        @Override
        public ServerFeatureUtil getServerFeatureUtil(boolean suppress, Map files) {
            return GenerateFeaturesTask.this.getServerFeatureUtil(suppress, files);
        }
        @Override
        public Set<String> getServerFeatures(ServerFeatureUtil servUtil, File generationContextDir, Set<String> generatedFiles, boolean excludeGenerated) {
            return getServerFeaturesGradle(servUtil, generationContextDir, generatedFiles, excludeGenerated);
        }
        @Override
        public Set<String> getClassesDirectories(List projects) throws GenerateFeaturesException {
            return getClassesDirectoriesGradle();
        }
        @Override
        public List<Object> getProjectList(Object project) {
            return Collections.singletonList(project);
        }
        @Override
        public String getEEVersion(List projects) {
            return GenerateFeaturesTask.this.getEEVersion(projects.get(0));
        }
        @Override
        public String getMPVersion(List projects) {
            return GenerateFeaturesTask.this.getMPVersion(projects.get(0));
        }
        @Override
        public String getLogLocation(Object project) {
            return project.getBuildDir().getCanonicalPath();
        }
        @Override
        public File getServerXmlFile() {
            return server.serverXmlFile;
        }
        @Override
        public void debug(String msg) {
            logger.debug(msg);
        }
        @Override
        public void debug(String msg, Throwable t) {
            logger.debug(msg, t);
        }
        @Override
        public void warn(String msg) {
            logger.warn(msg);
        }
        @Override
        public void info(String msg) {
            // use logger.lifecycle so that message appears without --info option on
            logger.lifecycle(msg);
        }
    }

    // Define the logging functions of the binary scanner handler and make it available in this plugin
    private class BinaryScannerHandler extends BinaryScannerUtil {
        BinaryScannerHandler(File scannerFile) {
            super(scannerFile);
        }

        @Override
        public void debug(String msg) {
            logger.debug(msg);
        }

        @Override
        public void debug(String msg, Throwable t) {
            logger.debug(msg, t);
        }

        @Override
        public void error(String msg) {
            logger.error(msg);
        }

        @Override
        public void warn(String msg) {
            logger.warn(msg);
        }

        @Override
        public void info(String msg) {
            logger.lifecycle(msg);
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isEnabled(LogLevel.DEBUG);
        }
    }
}
