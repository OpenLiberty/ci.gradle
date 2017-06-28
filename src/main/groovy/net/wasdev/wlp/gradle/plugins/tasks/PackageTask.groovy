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

import org.gradle.api.tasks.TaskAction

class PackageTask extends AbstractTask {

    @TaskAction
    void packageServer() {
    
        def params = buildLibertyMap(project);

        final archive = project.liberty.packageLiberty.archive
        
        // default output directory
        def buildLibsDir = new File(project.getBuildDir(), 'libs')
        def fileType = getPackageFileType(project.liberty.packageLiberty.include)
        
        if (archive != null && archive.length() != 0) {
            def archiveFile = new File(project.getProjectDir(), archive)
            
            if (archiveFile.exists()) {
                if (archiveFile.isDirectory()) {
                    // package ${project.name}.zip | jar 
                    archiveFile = new File(archiveFile, project.getName() + fileType)
                } 
            } else {
                if (archive.endsWith(".zip") || archive.endsWith(".jar")) {
                    archiveFile = new File(buildLibsDir, archive)
                } else {
                    archiveFile = new File(buildLibsDir, archive + fileType)
                }
            }
            
            params.put('archive', archiveFile)
            logger.info 'Packaging ' + archiveFile
            
        } else {
            def defaultPackageFile = new File(buildLibsDir, project.getName() + fileType)
            params.put('archive', defaultPackageFile)
            logger.info 'Packaging ' + defaultPackageFile
        }
        
        if (project.liberty.packageLiberty.include != null && project.liberty.packageLiberty.include.length() != 0) {
            params.put('include', project.liberty.packageLiberty.include)
        }
        if (project.liberty.packageLiberty.os != null && project.liberty.packageLiberty.os.length() != 0) {
            params.put('os', project.liberty.packageLiberty.os)
        }

        executeServerCommand(project, 'package', params)
    }
    
    private String getPackageFileType(String include) {
        if (include != null && include.contains("runnable")) {
            return ".jar"
        }
        return ".zip"
    }
}
