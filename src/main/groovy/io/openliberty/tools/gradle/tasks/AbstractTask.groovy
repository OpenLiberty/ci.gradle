/**
 * (C) Copyright IBM Corporation 2017, 2020.
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


import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.Task
import org.gradle.api.tasks.Internal

abstract class AbstractTask extends DefaultTask {

    //params that get built with installLiberty
    protected def params
    protected boolean isWindows = System.properties['os.name'].toLowerCase().indexOf("windows") >= 0
    protected String springBootVersion
    protected Task springBootTask

    protected getInstallDir = { Project project ->
        if (project.liberty.installDir == null) {
            if (project.liberty.baseDir == null) {
                return new File(project.buildDir, 'wlp')
            } else {
                return new File(project.liberty.baseDir, 'wlp')
            }
        } else {
            return new File(project.liberty.installDir)
        }
    }

    protected File getUserDir(Project project) {
        return getUserDir(project, getInstallDir(project))
    }

    protected File getUserDir(Project project, File installDir) {
        return (project.liberty.userDir == null) ? new File(installDir, 'usr') : new File(project.liberty.userDir)
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
                    if ("org.springframework.boot".equals(dep.getGroup()) && "spring-boot-gradle-plugin".equals(dep.getName())) {
                        version = dep.getVersion()
                        break
                    }
                }
            } catch (MissingPropertyException e) {
                project.getLogger().warn('No buildscript configuration found.')
                throw new GradleException("Unable to determe version of spring boot gradle plugin.")
            }
        }
        return version
    }

    protected static Task findSpringBootTask(Project project, String springBootVersion) {
        if (springBootVersion == null) {
            return null
        }
        Task task
        //Do not change the order of war and java
        if (springBootVersion.startsWith('2.')) {
            if (project.plugins.hasPlugin('war')) {
                task = project.bootWar
            } else if (project.plugins.hasPlugin('java')) {
                task = project.bootJar
            }
        } else if (springBootVersion.startsWith('1.')) {
            if (project.plugins.hasPlugin('war')) {
                task = project.war
            } else if (project.plugins.hasPlugin('java')) {
                task = project.jar
            }
        }
        return task
    }
    @Internal
    protected boolean isLibertyInstalled(Project project) {
        File installDir = getInstallDir(project)
        return (installDir.exists() && new File(installDir, "lib/ws-launch.jar").exists())
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

}
