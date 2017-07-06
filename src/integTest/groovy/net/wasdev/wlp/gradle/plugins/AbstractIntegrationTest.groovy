/*
 * (C) Copyright IBM Corporation 2015, 2017.
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
package net.wasdev.wlp.gradle.plugins

import static org.junit.Assert.*

import org.apache.commons.io.FileUtils
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection


abstract class AbstractIntegrationTest {
    static File integTestDir = new File('build/testBuilds')
    static final String test_mode = System.getProperty("runit")
    static String WLP_DIR = System.getProperty("wlpInstallDir")

    protected static void deleteDir(File dir) {
        if (dir.exists()) {
            if (!integTestDir.deleteDir()) {
                throw new AssertionError("Unable to delete directory '$dir.canonicalPath'.")
            }
        }
    }

    protected static void createDir(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new AssertionError("Unable to create directory '$dir.canonicalPath'.")
            }
        }
    }
    
    protected static File createTestProject(File parent, File sourceDir, String buildFilename) {
        if (!sourceDir.exists()){
            throw new AssertionError("The source file '${sourceDir.canonicalPath}' doesn't exist.")
        }
        try {
            FileUtils.copyDirectory(sourceDir, parent)
            renameBuildFile(buildFilename, parent)
        } catch (IOException e) {
            throw new AssertionError("Unable to copy directory '${parent.canonicalPath}'.")
        }
    }

    protected static void runTasks(File projectDir, String... tasks) {
        GradleConnector gradleConnector = GradleConnector.newConnector()
        gradleConnector.forProjectDirectory(projectDir)
        ProjectConnection connection = gradleConnector.connect()

        try {
            BuildLauncher build = connection.newBuild()
            build.setJvmArguments("-DWLP_DIR=$WLP_DIR")
            build.withArguments("-i"); 
            build.forTasks(tasks)
            build.run()
        }
        finally {
            connection?.close()
        }
    }
    
    public static void renameBuildFile(String buildFilename, File buildDir) {
        File sourceFile = new File(buildDir, buildFilename)
        sourceFile.renameTo(buildDir.toString() + '/build.gradle')
    }
    
    protected static File copyFile(File sourceFile, File destFile) {
        if (!sourceFile.exists()){
            throw new AssertionError("The source file '${sourceFile.canonicalPath}' doesn't exist.")
        }
        try {
            FileUtils.copyFile(sourceFile, destFile)
        } catch (Exception e) {
            throw new AssertionError("Unable to create file '${destFile.canonicalPath}'.")
        }
    }

}
