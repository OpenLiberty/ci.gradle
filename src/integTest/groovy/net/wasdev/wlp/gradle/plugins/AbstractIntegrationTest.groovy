/*
 * (C) Copyright IBM Corporation 2015.
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

import org.apache.tools.ant.util.FileUtils
import org.junit.BeforeClass
import org.junit.AfterClass

import static org.junit.Assert.*

import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.GradleProject
import org.gradle.tooling.model.Task


abstract class AbstractIntegrationTest {
    static File integTestDir = new File('build/integTest')
    static final String WLP_DIR = System.getProperty("wlpInstallDir").replace("\\", "/")

    @BeforeClass
    public static void setup() {
        deleteDir(integTestDir)
        createDir(integTestDir)
        createFile(integTestDir)

    }

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

    protected static File createFile(File parent) {
        File destFile = new File(parent, 'build.gradle')
        File sourceFile = new File("build/resources/integrationTest/build.gradle")
        if (!sourceFile.exists()){
            throw new AssertionError("The source file '${sourceFile.canonicalPath}' doesn't exist.")
        }
        try {
            FileUtils.getFileUtils().copyFile(sourceFile, destFile, null, true);
        } catch (IOException e) {
            throw new AssertionError("Unable to create file '${destFile.canonicalPath}'.")
        }
    }

    protected static void runTasks(File projectDir, String... tasks) {
        GradleConnector gradleConnector = GradleConnector.newConnector()
        gradleConnector.forProjectDirectory(projectDir)
        ProjectConnection connection = gradleConnector.connect()
        
        try {
            BuildLauncher build = connection.newBuild()
            build.setJvmArguments("-DWLP_DIR=$WLP_DIR")
            build.forTasks(tasks)
            build.run()
        }
        finally {
            connection?.close()
        }
    }
}