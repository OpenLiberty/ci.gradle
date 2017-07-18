/**
 * (C) Copyright IBM Corporation 2014, 2015, 2017.
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

import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Set
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.gradle.api.GradleException

class InstallAppsTask extends AbstractTask {

    @TaskAction
    void installApps() {
        
        copyConfigFiles()
        
        boolean installDependencies = false
        boolean installProject = false
        
        switch (getInstallAppPackages()) {
            case "all":
                installDependencies = true
                installProject = true
                break;
            case "dependencies":
                installDependencies = true
                break
            case "project":
                installProject = true
                break
            default:
                return
        }
        
        if (installProject) {
            installProjectArchive()
        }
        
    }
    
    
    private void installProjectArchive() throws Exception {
        File archive = new File(archivePath())
        if(!archive.exists()) {
            throw new GradleException("The project archive was not found and can't be installed.")
        }
        if(project.liberty.installapps.appsDirectory.equals("apps")) {
            Files.copy(archive.toPath(), new File(getServerDir(project), "/apps/" + archive.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        else {
            Files.copy(archive.toPath(), new File(getServerDir(project), "/dropins/" + archive.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
    
    private String getInstallAppPackages() {
        if (project.plugins.hasPlugin("ear")) {
            project.liberty.installapps.installAppPackages = "project"
        }
        return project.liberty.installapps.installAppPackages
    }
    
        
    private String archivePath() {
        if (project.plugins.hasPlugin("ear")) {
            return project.ear.archivePath
        }
        else if (project.plugins.hasPlugin("war")) {
            return project.war.archivePath
        }
        else {
            return project.jar.archivePath
        }
        
    }
    
}
