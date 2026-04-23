/**
 * (C) Copyright IBM Corporation 2017, 2026.
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

import io.openliberty.tools.gradle.Liberty
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import groovy.xml.XmlParser
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaToolchainService

import javax.inject.Inject

abstract class AbstractLibertyTask extends DefaultTask {

    //params that get built with installLiberty
    protected def params
    protected boolean isWindows = System.properties['os.name'].toLowerCase().indexOf("windows") >= 0
    protected String springBootVersion
    protected Task springBootTask

    // Standard toolchain support properties
    @Nested
    @Optional
    public final Property<JavaLauncher> javaLauncher = project.objects.property(JavaLauncher)

    // Cached toolchain javaHome
    private String resolvedToolchainJavaHome = null

    public JavaLauncher getJavaLauncher() {
        if(!javaLauncher.isPresent()) {
            configureDefaults();
        }
        // return null if somehow configureDefaults failed with default launcher
        return javaLauncher.getOrNull()
    }

    public void setJavaLauncher(JavaLauncher launcher) {
        javaLauncher.set(launcher)
    }

    @Inject
    protected JavaToolchainService getJavaToolchainService() {
        return project.getExtensions().getByType(JavaToolchainService)
    }

    // Constructor to configure default toolchain behavior
    AbstractLibertyTask() {
        configureDefaults()
    }

    /**
     * configure default java launcher using toolchain
     * toolchain can be null for a project, but JavaToolchainService is expected to provide the default launcher
     */
    void configureDefaults() {
        try {
            // Check if the extension exists before trying to use it
            def javaExtension = project.extensions.findByType(JavaPluginExtension)
            if (javaExtension != null) {
                if (javaExtension.toolchain != null && javaExtension.toolchain.languageVersion.isPresent()) {
                    // If the toolchain changes, the launcher updates automatically.
                    javaLauncher.convention(
                            getJavaToolchainService().launcherFor(javaExtension.toolchain))
                } else {
                    logger.debug("JavaPluginExtension toolchain is unconfigured. Toolchain JDK will not be honored.")
                }
            } else {
                logger.debug("JavaPluginExtension not found. Toolchain JDK will not be honored.")
            }
        } catch (Exception e) {
            logger.debug("Could not configure default toolchain: ${e.message}")
        }
    }

    protected boolean isInstallDirChanged(Project project) {

        XmlParser pluginXmlParser = new XmlParser()
        Node libertyPluginConfig = pluginXmlParser.parse(new File(project.getLayout().getBuildDirectory().getAsFile().get(), 'liberty-plugin-config.xml'))
        if (!libertyPluginConfig.getAt('installDirectory').isEmpty()) {
            Node installDirNode = libertyPluginConfig.getAt('installDirectory').get(0)
            File previousInstallDir = new File(installDirNode.text())
            File currentInstallDir = getInstallDir(project)
            if (previousInstallDir.exists() && previousInstallDir.equals(currentInstallDir)) {
                return false
            }
        }
        return true
    }

    protected getInstallDir = { Project project ->
        return Liberty.getInstallDir(project);
    }

    protected File getUserDir(Project project) {
        return getUserDir(project, getInstallDir(project))
    }

    protected File getUserDir(Project project, File installDir) {
        return (project.liberty.userDir == null) ? new File(installDir, 'usr') : new File(project.liberty.userDir)
    }

    @Internal
    protected boolean isUserDirSpecified() {
        return (project.liberty.userDir != null)
    }

    protected File getOutputDir(Map<String, String> params) {
        if (params.get('outputDir') == null ) {
            return (params.get('outputDir'))
        } else {
            return (new File(params.get('outputDir')))
        }
    }

    /**
     * Detect spring boot version dependency
     */
    protected static String findSpringBootVersion(Project project) {
        String version = null
        if (project.plugins.hasPlugin("org.springframework.boot")) {
            try {
                for (Dependency dep : project.buildscript.configurations.classpath.getAllDependencies().toArray()) {
                    if ("org.springframework.boot".equals(dep.getGroup()) &&
                            ("spring-boot-gradle-plugin".equals(dep.getName()) ||
                                "org.springframework.boot.gradle.plugin".equals(dep.getName()))) {
                        version = dep.getVersion()
                        break
                    }
                }
            } catch (MissingPropertyException e) {
                project.getLogger().warn('No buildscript configuration found.')
                throw new GradleException("Unable to determine version of spring boot gradle plugin.")
            }
        }
        return version
    }

    protected static boolean isSpringBoot1(String springBootVersion) {
        if (springBootVersion == null) {
            return false
        }
        return springBootVersion.startsWith('1.')
    }

    protected static boolean isSpringBoot2plus(String springBootVersion) {
        if (springBootVersion == null) {
            return false
        }
        if (springBootVersion.contains('.')) {
            String majorVersion = springBootVersion.substring(0,springBootVersion.indexOf('.'))
            try {
                int majorVersionNumber = Integer.parseInt(majorVersion)
                return majorVersionNumber > 1
            } catch (NumberFormatException e) {
            }
        }
        return false
    }

    protected static String getLibertySpringBootFeature(String springBootVersion) {
        if (isSpringBoot1(springBootVersion)) {
            return "springBoot-1.5"
        } else if (isSpringBoot2plus(springBootVersion)) {
            String majorVersion = springBootVersion.substring(0,springBootVersion.indexOf('.'))
            return "springBoot-"+majorVersion+".0"
        }
        return null
    }

    protected static Task findSpringBootTask(Project project, String springBootVersion) {
        if (springBootVersion == null) {
            return null
        }
        Task task
        //Do not change the order of war and java
        if (isSpringBoot2plus(springBootVersion)) {
            if (project.plugins.hasPlugin('war')) {
                task = project.bootWar
            } else if (project.plugins.hasPlugin('java')) {
                task = project.bootJar
            }
        } else if (isSpringBoot1(springBootVersion)) {
            if (project.plugins.hasPlugin('war')) {
                task = project.war
            } else if (project.plugins.hasPlugin('java')) {
                task = project.jar
            }
        }
        return task
    }
    protected boolean isLibertyInstalledAndValid(Project project) {
        File installDir = getInstallDir(project)
        boolean installationExists = installDir.exists() && new File(installDir,"lib/ws-launch.jar").exists()
        if (!installationExists && (project.liberty.installDir != null)) {
            throw new GradleException("Unable to find a valid Liberty installation at installDir path " + installDir +". Please specify a valid path for the installDir property.")
        }
        return installationExists
    }

    private final String  COM_IBM_WEBSPHERE_PRODUCTID_KEY = "com.ibm.websphere.productId"
    private final String COM_IBM_WEBSPHERE_PRODUCTVERSION_KEY = "com.ibm.websphere.productVersion"

    @Internal
    protected boolean isClosedLiberty() {
        getLibertyInstallProperties().getProperty(COM_IBM_WEBSPHERE_PRODUCTID_KEY).contains("com.ibm.websphere.appserver")
    }

    @Internal
    protected Properties getLibertyInstallProperties() {
        File propertiesDir = new File(getInstallDir(project), "lib/versions")
        File wlpProductInfoProperties = new File(propertiesDir, "WebSphereApplicationServer.properties")
        File olProductInfoProperties = new File(propertiesDir, "openliberty.properties")
        File propsFile
        if (wlpProductInfoProperties.isFile()) {
            propsFile = wlpProductInfoProperties
        }
        else if (olProductInfoProperties.isFile()) {
            propsFile = olProductInfoProperties
        }
        else {
            throw new GradleException("Unable to determine the Liberty installation product information. " +
                    "The Liberty installation may be corrupt.")
        }
        Properties installProps = new Properties()
                propsFile.withInputStream { installProps.load(it) }
        if (!installProps.containsKey(COM_IBM_WEBSPHERE_PRODUCTID_KEY) ||
                !installProps.containsKey(COM_IBM_WEBSPHERE_PRODUCTVERSION_KEY)) {
            throw new GradleException("Unable to determine the Liberty installation product information. " +
                    "The Liberty installation may be corrupt.")
        }
        return installProps
    }

    /**
     * Get the configured Java launcher from standard toolchain properties.
     * This provides the standard way to access toolchain configuration.
     */
    @Internal
    protected JavaLauncher getConfiguredLauncher() {
        try {
            def launcher = getJavaLauncher()
            logger.info("Successfully got configured launcher: ${launcher}")
            return launcher
        } catch (Exception e) {
            logger.debug("Could not get configured launcher: ${e.message}")
            return null
        }
    }

    /**
     * Get the Java home path from the configured toolchain.
     * This provides the standard way to access the JDK path.
     */
    @Internal
    protected String getToolchainJavaHome() {
        if (resolvedToolchainJavaHome != null) {
            return resolvedToolchainJavaHome
        }
        def launcher = getConfiguredLauncher()
        if (launcher != null) {
            resolvedToolchainJavaHome = launcher.metadata.installationPath.asFile.absolutePath
        }
        return resolvedToolchainJavaHome
    }
    @Internal
    protected boolean isToolchainConfigured() {
        def javaExtension = project.extensions.findByType(JavaPluginExtension)
        if (javaExtension != null) {
            def toolchain = project.extensions.getByType(JavaPluginExtension).toolchain
            if (toolchain.getLanguageVersion().isPresent()) {
                logger.info("CWWKM4100I: Using toolchain from build context. JDK Version specified is ${toolchain.getLanguageVersion().get()}")
                return true
            }
        }
        return false
    }
}
