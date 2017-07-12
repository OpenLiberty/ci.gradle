/**
 * (C) Copyright IBM Corporation 2014, 2017.
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

import javax.xml.parsers.*

import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class InstallLibertyTask extends AbstractTask {
	
	final MAVEN_REPO = "http://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/"

    @TaskAction
    void install() {
        def params = buildInstallLibertyMap(project)
        project.ant.taskdef(name: 'installLiberty', 
                            classname: 'net.wasdev.wlp.ant.install.InstallLibertyTask', 
                            classpath: project.buildscript.configurations.classpath.asPath)
        project.ant.installLiberty(params)
    }

    private Map<String, String> buildInstallLibertyMap(Project project) {

        Map<String, String> result = new HashMap();
        if (project.liberty.install.licenseCode != null) {
           result.put('licenseCode', project.liberty.install.licenseCode)
        }

        if (project.liberty.install.version != null) {
            result.put('version', project.liberty.install.version)
        }
        
        if (project.liberty.install.type != null) {
            result.put('type', project.liberty.install.type)
        }

        if (project.liberty.install.runtimeUrl != null) {
            result.put('runtimeUrl', project.liberty.install.runtimeUrl)
        } 
        else if (project.liberty.assemblyArtifact.artifactId != null ||
            project.liberty.assemblyArtifact.version != null) {
            
            String version 
            String artifactId = "wlp-webProfile7"
            String type = "zip"

            if (project.liberty.assemblyArtifact.artifactId != null) {
                artifactId = project.liberty.assemblyArtifact.artifactId
            }

            if (project.liberty.assemblyArtifact.version != null) {
                version = project.liberty.assemblyArtifact.version
            } else {
                def maven_version = getLatestVersionFromMaven(artifactId)
                if (maven_version != null) {
                    version = maven_version
                }
            }
            
            if (project.liberty.assemblyArtifact.type != null) {
                type = project.liberty.assemblyArtifact.type
            }
            
            String artifactPath =  artifactId + "/" + version + "/" + artifactId + "-" + version + "." + type
            String remoteMavenRepo = MAVEN_REPO + artifactPath
            String localMavenRepo = new File(System.getProperty('user.home'), '.m2/repository').absolutePath + 
                                    "/com/ibm/websphere/appserver/runtime/" + artifactPath
            
            File localFile = new File(localMavenRepo)
            
            if (localFile.exists()) {
                logger.debug 'Getting WebSphere Liberty server from the local Maven repository.'
                result.put('runtimeUrl', localFile.toURI().toURL())
            }
            else { 
                logger.debug 'Getting WebSphere Liberty server from the remote Maven repository.'
                result.put('runtimeUrl', remoteMavenRepo)
            }
            logger.debug 'Maven runtimeUrl is ' + result.getAt('runtimeUrl')
        } 
        
        if (project.liberty.install.baseDir == null) {
           result.put('baseDir', project.buildDir)
        } else {
           result.put('baseDir', project.liberty.install.baseDir)
        }

        if (project.liberty.install.cacheDir != null) {
            result.put('cacheDir', project.liberty.install.cacheDir)
        }

        if (project.liberty.install.username != null) {
            result.put('username', project.liberty.install.username)
            result.put('password', project.liberty.install.password)
        }

        result.put('maxDownloadTime', project.liberty.install.maxDownloadTime)

        result.put('offline', project.gradle.startParameter.offline)

        return result
    }
    
    private String getLatestVersionFromMaven(String artifactId) {
        def url= MAVEN_REPO + artifactId + "/maven-metadata.xml"
        def metadata
        def version
        
        try {
            metadata = new XmlSlurper().parse(url)
            version = metadata.versioning.latest
            logger.debug "Obtained the latest release version from the Maven repository : " + version
        } catch (Exception e) {
            logger.debug "Failed to get latest version from the Maven repository : " + e
            version = "17.0.0.2"
        }
        
        return version
    }
}
