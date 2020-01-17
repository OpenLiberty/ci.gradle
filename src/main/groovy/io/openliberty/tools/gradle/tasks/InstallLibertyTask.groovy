/**
 * (C) Copyright IBM Corporation 2014, 2020.
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

import javax.xml.parsers.*

import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.logging.LogLevel
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import groovy.xml.MarkupBuilder

import java.util.Map.Entry
import java.util.Set

import org.gradle.api.GradleException

class InstallLibertyTask extends AbstractTask {
    protected Properties libertyRuntimeProjectProps = new Properties()
    protected String detachedCoords
    protected String detachedConfigFilePath
    // default to install the latest Open Liberty kernel from Maven Central repository
    protected String defaultRuntime = "io.openliberty:openliberty-kernel:[19.0.0.9,)"

    InstallLibertyTask() {
        configure({
            description 'Installs Liberty from a repository'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })
        outputs.upToDateWhen {
            getInstallDir(project).exists() && project.buildDir.exists() && new File(project.buildDir, 'liberty-plugin-config.xml').exists()
        }
    }

    @Input
    @Optional
    Configuration getLibertyRuntimeConfiguration() {
        return project.configurations.libertyRuntime
    }

    @TaskAction
    void install() {
        if (!isLibertyInstalled(project)) {
            def params = buildInstallLibertyMap(project)

            project.ant.taskdef(name: 'installLiberty',
                                classname: 'io.openliberty.tools.ant.install.InstallLibertyTask',
                                classpath: project.buildscript.configurations.classpath.asPath)
            project.ant.installLiberty(params)

            String licenseFilePath = project.configurations.getByName('libertyLicense').getAsPath()
            if (licenseFilePath) {
                def command = "java -jar " + licenseFilePath + " --acceptLicense " + project.buildDir
                def process = command.execute()
                process.waitFor()
            }
            
        } else {
            logger.info ("Liberty is already installed at: " + getInstallDir(project))
        }
        createPluginXmlFile()
    }

    protected void createPluginXmlFile() {
        if(!this.state.upToDate) {
            if (!project.buildDir.exists()) {
                logger.info ("Creating missing project buildDir at ${project.buildDir}.")
                project.buildDir.mkdirs()
            }

            new File(project.buildDir, 'liberty-plugin-config.xml').withWriter { writer ->
                def xmlDoc = new MarkupBuilder(writer)
                xmlDoc.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")
                xmlDoc.'liberty-plugin-config'('version':'2.0') {
                    outputLibertyPropertiesToXml(xmlDoc)
                }
            }
            logger.info ("Adding Liberty plugin config info to ${project.buildDir}/liberty-plugin-config.xml.")
        }
    }

    private boolean checkAndLoadInstallExtensionProperties(Map<String,String> props) {
        boolean hasInstallExtProps = false;

        if (project.liberty.install.licenseCode != null) {
            hasInstallExtProps = true
            props.put('licenseCode', project.liberty.install.licenseCode)
        }

        if (project.liberty.install.version != null) {
            hasInstallExtProps = true
            props.put('version', project.liberty.install.version)
        }

        if (project.liberty.install.type != null) {
            hasInstallExtProps = true
            props.put('type', project.liberty.install.type)
        }

        if (project.liberty.install.username != null) {
            hasInstallExtProps = true
            props.put('username', project.liberty.install.username)
            props.put('password', project.liberty.install.password)
        }

        if (project.liberty.install.runtimeUrl != null) {
            hasInstallExtProps = true
            props.put('runtimeUrl', project.liberty.install.runtimeUrl)
        }

        if (project.liberty.install.useOpenLiberty != null) {
            hasInstallExtProps = true
            boolean value = Boolean.parseBoolean(project.liberty.install.useOpenLiberty)
            props.put('useOpenLiberty', value)
        }

        if (project.liberty.install.maxDownloadTime != null) {
            hasInstallExtProps = true
            props.put('maxDownloadTime', project.liberty.install.maxDownloadTime)
        }

        if (hasInstallExtProps  && project.liberty.install.useOpenLiberty == null) {
            // default to true
            props.put('useOpenLiberty', 'true')
        }

        if (hasInstallExtProps  && project.liberty.install.maxDownloadTime == null) {
            // default to zero
            props.put('maxDownloadTime', '0')
        }

        return hasInstallExtProps
    }

    private Map<String, String> buildInstallLibertyMap(Project project) {

        detachedCoords = null
        detachedConfigFilePath = null
        loadLibertyRuntimeProperties()

        Map<String, String> result = new HashMap()
        boolean hasInstallExtensionProps = checkAndLoadInstallExtensionProperties(result)

        if (!hasInstallExtensionProps) {
            String runtimeFilePath = project.configurations.getByName('libertyRuntime').getAsPath()
            String coordinatesToUse = null

            try {
                if (runtimeFilePath) {
                    coordinatesToUse = getLibertyRuntimeCoordinates()
                    String newCoords = getUpdatedLibertyRuntimeCoordinates(coordinatesToUse)

                    if (newCoords != null && !newCoords.equals(coordinatesToUse)) {
                        coordinatesToUse = newCoords
                        detachedCoords = coordinatesToUse
                    }
                } else {
                    // default to get the Open Liberty runtime from maven
                    // if the file cannot be located, throw an error - a maven repo may not be configured
                    coordinatesToUse = getDefaultLibertyRuntimeCoordinates()
                    detachedCoords = coordinatesToUse
                }
                if (detachedCoords != null) {
                    Dependency dep = project.dependencies.create(detachedCoords)
                    Configuration detachedConfig = project.configurations.detachedConfiguration( dep )
                    ResolvedConfiguration resolvedConfig = detachedConfig.getResolvedConfiguration()
                    if (resolvedConfig.hasError()) {
                        resolvedConfig.rethrowFailure()
                    }
                    detachedConfigFilePath = detachedConfig.getAsPath()
                    runtimeFilePath = detachedConfigFilePath
                }
            } catch (ResolveException e) {
                throw new GradleException("Could not find artifact with coordinates " + coordinatesToUse + ". Verify a Maven repository is configured that contains the corresponding artifact.",e)
            }

            File localFile = new File(runtimeFilePath)

            if (localFile.exists()) {
                logger.debug 'Getting WebSphere Liberty archive file from the local Gradle repository.'
                result.put('runtimeUrl', localFile.toURI().toURL())
            } else {
                logger.debug 'Liberty archive file does not exist in the local Gradle repository with path: ' + runtimeFilePath
                throw new GradleException("Could not find artifact with coordinates " + coordinatesToUse + ". Verify a Maven repository is configured that contains the corresponding artifact.")
            }
        }

        if (project.liberty.baseDir == null) {
           result.put('baseDir', project.buildDir)
        } else {
           result.put('baseDir', project.liberty.baseDir)
        }

        if (project.liberty.cacheDir != null) {
            result.put('cacheDir', project.liberty.cacheDir)
        }

        result.put('offline', project.gradle.startParameter.offline)

        return result
    }

    protected void outputLibertyPropertiesToXml(MarkupBuilder xmlDoc) {
        xmlDoc.installDirectory (getInstallDir(project).toString())

        if (detachedCoords != null) {
            String[] coords = detachedCoords.split(":")
            xmlDoc.assemblyArtifact {
                groupId (coords[0])
                artifactId (coords[1])
                version (coords[2])
                type ('zip')
            }
            xmlDoc.assemblyArchive (detachedConfigFilePath)

        } else if (project.configurations.libertyRuntime != null) {
            project.configurations.libertyRuntime.dependencies.each { libertyArtifact ->
                xmlDoc.assemblyArtifact {
                    groupId (libertyArtifact.group)
                    artifactId (libertyArtifact.name)
                    version (libertyArtifact.version)
                    type ('zip')
                }
                xmlDoc.assemblyArchive (project.configurations.libertyRuntime.resolvedConfiguration.resolvedArtifacts.getAt(0).file.toString())
            }
        }
    }

    @Internal
    protected String getLibertyRuntimeCoordinates() {
        String runtimeCoords = null
        Configuration config = project.configurations.getByName('libertyRuntime')
        if (config != null) {
             config.dependencies.find { libertyArtifact ->
                 runtimeCoords = libertyArtifact.group + ':' + libertyArtifact.name + ':' + libertyArtifact.version
                 logger.debug 'Existing Liberty runtime coordinates: ' + runtimeCoords
                 return true
             }
        }
        return runtimeCoords
    }

    protected String getUpdatedLibertyRuntimeCoordinates(String coords) {
        boolean useDefault = true
        String updatedCoords = defaultRuntime
        if (coords != null) {
            updatedCoords = coords
            useDefault = false
        } else {
            logger.debug 'Liberty runtime coordinates were null. Using default coordinates: ' + updatedCoords
        }

        String[] coordinates = updatedCoords.split(":")

        if (project.liberty.runtime != null && !project.liberty.runtime.isEmpty()) {
            String propGroupId = project.liberty.runtime.getProperty("group")
            if (propGroupId != null) {
                coordinates[0] = propGroupId
            }

            String propArtifactId = project.liberty.runtime.getProperty("name")
            if (propArtifactId != null) {
                coordinates[1] = propArtifactId
            }

            String propVersion = project.liberty.runtime.getProperty("version")
            if (propVersion != null) {
                coordinates[2] = propVersion
            }
        }

        // check for overridden liberty runtime properties in project properties
        if (!libertyRuntimeProjectProps.isEmpty()) {
            String propGroupId = libertyRuntimeProjectProps.getProperty("group")
            if (propGroupId != null) {
                coordinates[0] = propGroupId
            }

            String propArtifactId = libertyRuntimeProjectProps.getProperty("name")
            if (propArtifactId != null) {
                coordinates[1] = propArtifactId
            }

            String propVersion = libertyRuntimeProjectProps.getProperty("version")
            if (propVersion != null) {
                coordinates[2] = propVersion
            }
        }

        updatedCoords = coordinates[0] + ':' + coordinates[1] + ':' + coordinates[2]
        if ( (useDefault && !updatedCoords.equals(defaultRuntime)) ||
            (!useDefault && !updatedCoords.equals(coords)) ) {
                logger.debug 'Updated Liberty runtime coordinates: ' + updatedCoords
        }

        return updatedCoords
    }

    @Internal
    protected String getDefaultLibertyRuntimeCoordinates() {

        // check for overrides in liberty.runtime properties
        return getUpdatedLibertyRuntimeCoordinates(defaultRuntime)
    }

    private void loadLibertyRuntimeProperties() {
        Set<Entry<Object, Object>> entries = project.getProperties().entrySet()
        for (Entry<Object, Object> entry : entries) {
            String key = (String) entry.getKey()
            if (key.equals("liberty.runtime")) {
                // dealing with array of properties
                Object value = entry.getValue()
                String propValue = value == null ? null : value.toString()
                if (propValue != null) {
                    if ((propValue.startsWith("{") && propValue.endsWith("}")) || (propValue.startsWith("[") && propValue.endsWith("]"))) {
                        propValue = propValue.substring(1, propValue.length() -1)
                    }

                    // parse the array where properties are delimited by commas and the name/value are separated with a colon
                    String[] values = propValue.split(",")
                    for (String nextNameValuePair : values) {
                        String trimmedNameValuePair = nextNameValuePair.trim()
                        String[] splitNameValue = trimmedNameValuePair.split(":")
                        String nextPropName = splitNameValue[0].trim()

                        // remove surrounding quotes from property names and property values
                        if (nextPropName.startsWith("\"") && nextPropName.endsWith("\"")) {
                            nextPropName = nextPropName.substring(1, nextPropName.length() -1)
                        }

                        String nextPropValue = null
                        if (splitNameValue.length == 2) {
                            nextPropValue = splitNameValue[1].trim()
                            if (nextPropValue.startsWith("\"") && nextPropValue.endsWith("\"")) {
                                nextPropValue = nextPropValue.substring(1, nextPropValue.length() -1)
                            }
                            libertyRuntimeProjectProps.setProperty(nextPropName, nextPropValue)
                        }
                    }
                }
            } else if (key.startsWith("liberty.runtime.")) {
                // dealing with single property
                String suffix = key.substring("liberty.runtime.".length())
                if (suffix.startsWith("\"") && suffix.endsWith("\"")) {
                    suffix = suffix.substring(1, suffix.length() -1)
                }

                Object value = entry.getValue()
                String propValue = value == null ? null : value.toString()
                if (propValue != null) {
                    if (propValue.startsWith("\"") && propValue.endsWith("\"")) {
                        propValue = propValue.substring(1, propValue.length() -1)
                    }
                    libertyRuntimeProjectProps.setProperty(suffix, propValue)
                }
            }
        }
    }
}
