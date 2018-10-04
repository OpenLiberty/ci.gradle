/**
 * (C) Copyright IBM Corporation 2017, 2018.
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
package net.wasdev.wlp.gradle.plugins.tasks

import groovy.io.FileType
import jdk.internal.util.xml.PropertiesDefaultHandler
import net.wasdev.wlp.gradle.plugins.extensions.DeployExtension
import net.wasdev.wlp.gradle.plugins.extensions.LibertyExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import static groovy.io.FileType.*

import java.lang.reflect.Field
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.MessageFormat

abstract class AbstractTask extends DefaultTask {

    //params that get built with installLiberty
    def params
    protected boolean isWindows = System.properties['os.name'].toLowerCase().indexOf("windows") >= 0
    protected getInstallDir = { Project project ->
        if (project.liberty.installDir == null) {
            if (project.liberty.install.baseDir == null) {
                return new File(project.buildDir, 'wlp')
            } else {
                return new File(project.liberty.install.baseDir, 'wlp')
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
    public static String findSpringBootVersion(Project project) {
        String version = null

        try {
            for (Dependency dep : project.buildscript.configurations.classpath.getAllDependencies().toArray()) {
                if ("org.springframework.boot".equals(dep.getGroup()) && "spring-boot-gradle-plugin".equals(dep.getName())) {
                    version = dep.getVersion()
                    break
                }
            }
        } catch (MissingPropertyException e) {
            project.getLogger().warn('No buildscript configuration found.')
            return version
        }

        return version
    }

    protected boolean isLibertyInstalled(Project project) {
        File installDir = getInstallDir(project)
        return (installDir.exists() && new File(installDir, "lib/ws-launch.jar").exists())
    }

    final String  COM_IBM_WEBSPHERE_PRODUCTID_KEY = "com.ibm.websphere.productId"
    final String COM_IBM_WEBSPHERE_PRODUCTVERSION_KEY = "com.ibm.websphere.productVersion"

    protected boolean isOpenLiberty() {
        getLibertyInstallProperties().getProperty(COM_IBM_WEBSPHERE_PRODUCTID_KEY).contains("io.openliberty")
    }

    protected boolean isClosedLiberty() {
        getLibertyInstallProperties().getProperty(COM_IBM_WEBSPHERE_PRODUCTID_KEY).contains("com.ibm.websphere.appserver")
    }

    protected String getInstallVersion() {
        getLibertyInstallProperties().getProperty(COM_IBM_WEBSPHERE_PRODUCTVERSION_KEY)
    }

    protected Properties getLibertyInstallProperties() {
        String COM_IBM_WEBSPHERE_PRODUCTID_KEY = "com.ibm.websphere.productId"
        String COM_IBM_WEBSPHERE_PRODUCTVERSION_KEY = "com.ibm.websphere.productVersion"
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
