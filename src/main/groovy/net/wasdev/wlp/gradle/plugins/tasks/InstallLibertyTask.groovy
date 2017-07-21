/**
 * (C) Copyright IBM Corporation 2014, 2015.
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

import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class InstallLibertyTask extends AbstractTask {

    @TaskAction
    void install() {
        def params = buildInstallLibertyMap(project)
        project.ant.taskdef(name: 'installLiberty', 
                            classname: 'net.wasdev.wlp.ant.install.InstallLibertyTask', 
                            classpath: project.buildscript.configurations.classpath.asPath)
        project.ant.installLiberty(params)
       
        if (project.liberty.license || (project.liberty.licenseArtifact.groupId &&
                                        project.liberty.licenseArtifact.artifactId &&
                                        project.liberty.licenseArtifact.version)) {
            project.getConfigurations().create('LicenseConfig')
            if (project.liberty.license) {
                project.dependencies.add('LicenseConfig', project.liberty.license)
            } else {
                project.dependencies.add('LicenseConfig', project.liberty.licenseArtifact.groupId + ":" +
                                                          project.liberty.licenseArtifact.artifactId + ":" +
                                                          project.liberty.licenseArtifact.version)
            }
            String filePath = project.getConfigurations().getByName('LicenseConfig').getAsPath()
            def command = "java -jar " + filePath + " --acceptLicense " + project.buildDir
            def process = command.execute()
            process.waitFor()
        }
    }

    private Map<String, String> buildInstallLibertyMap(Project project) {
        Map<String, String> result = new HashMap();
        if (project.liberty.install.licenseCode != null) {
           result.put('licenseCode', project.liberty.install.licenseCode)
        }

        if (project.liberty.install.version != null) {
            result.put('version', project.liberty.install.version)
        }

        if (project.liberty.install.runtimeUrl != null) {
            result.put('runtimeUrl', project.liberty.install.runtimeUrl)
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

        if (project.liberty.install.type != null) {
            result.put('type', project.liberty.install.type)
        }

        result.put('maxDownloadTime', project.liberty.install.maxDownloadTime)

        result.put('offline', project.gradle.startParameter.offline)

        return result;
    }

}
