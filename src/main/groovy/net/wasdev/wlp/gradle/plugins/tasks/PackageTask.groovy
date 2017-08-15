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

class PackageTask extends AbstractServerTask {

    @TaskAction
    void packageServer() {

        def params = buildLibertyMap(project);
        def fileType = getPackageFileType(project.liberty.server.packageLiberty.include)

        def archive = project.liberty.server.packageLiberty.archive

        copyConfigFiles()
        if (archive != null && archive.length() != 0) {
            def archiveFile = new File(archive)

            if (archiveFile.exists() && archiveFile.isDirectory()) {
                archiveFile = new File(archiveFile, project.getName() + fileType)
            }
            params.put('archive', archiveFile)
            logger.debug 'Packaging ' + archiveFile
        } else {
            // default output directory
            def buildLibsDir = new File(project.getBuildDir(), 'libs')

            createDir(buildLibsDir)

            def defaultPackageFile = new File(buildLibsDir, project.getName() + fileType)
            params.put('archive', defaultPackageFile)
            logger.debug 'Packaging default ' + defaultPackageFile
        }

        if (project.liberty.server.packageLiberty.include != null && project.liberty.server.packageLiberty.include.length() != 0) {
            params.put('include', project.liberty.server.packageLiberty.include)
        }
        if (project.liberty.server.packageLiberty.os != null && project.liberty.server.packageLiberty.os.length() != 0) {
            params.put('os', project.liberty.server.packageLiberty.os)
        }

        executeServerCommand(project, 'package', params)
    }

    private String getPackageFileType(String include) {
        if (include != null && include.contains("runnable")) {
            return ".jar"
        }
        return ".zip"
    }

    private static void createDir(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new AssertionError("Unable to create directory '$dir.canonicalPath'.")
            }
        }
    }
}
