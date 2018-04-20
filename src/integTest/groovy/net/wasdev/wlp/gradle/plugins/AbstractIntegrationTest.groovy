/*
 * (C) Copyright IBM Corporation 2015, 2018.
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

import java.io.File

import static org.junit.Assert.*

import org.apache.commons.io.FileUtils
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.api.GradleException

abstract class AbstractIntegrationTest {

    public static final String LIBERTY_PROPERTIES_FILENAME_1 = 'wlp1.properties'
    public static final String LIBERTY_PROPERTIES_FILENAME_2 = 'wlp2.properties'
    public static final String OPEN_LIBERTY_PROPERTIES_FILENAME_1 = 'ol1.properties'
    public static final String OPEN_LIBERTY_PROPERTIES_FILENAME_2 = 'ol2.properties'

    public static final String PROPERTY_FILE_DIRECTORY = "src/integTest/properties"

    static File integTestDir = new File('build/testBuilds')
    static final String test_mode = System.getProperty("runit")
    static String WLP_DIR = System.getProperty("wlpInstallDir")
    static String libertyProperties = System.getProperty("propertiesFile")

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

    protected static File copyBuildFiles(File buildFilename, File buildDir) {
        copyFile(buildFilename, new File(buildDir, 'build.gradle'))
        copyPropertyFile(buildDir)
    }

    protected static void copyPropertyFile(File buildDir) {
        File propertyFile
        if (libertyProperties != null) {
            switch (libertyProperties) {
                case LIBERTY_PROPERTIES_FILENAME_2:
                    propertyFile = new File(PROPERTY_FILE_DIRECTORY, LIBERTY_PROPERTIES_FILENAME_2)
                    break;
                case OPEN_LIBERTY_PROPERTIES_FILENAME_1:
                    propertyFile = new File(PROPERTY_FILE_DIRECTORY, OPEN_LIBERTY_PROPERTIES_FILENAME_1)
                    break;
                case OPEN_LIBERTY_PROPERTIES_FILENAME_2:
                    propertyFile = new File(PROPERTY_FILE_DIRECTORY, OPEN_LIBERTY_PROPERTIES_FILENAME_2)
                    break;
                default:
                    propertyFile = new File(PROPERTY_FILE_DIRECTORY, LIBERTY_PROPERTIES_FILENAME_1)
                    break;
            }
        } else {
            throw new GradleException('Tests could not be run. Please specify a properties file.')
        }
        copyFile(propertyFile, new File(buildDir, 'gradle.properties'))
    }

    protected static File createTestProject(File parent, File sourceDir, String buildFilename) {
        if (!sourceDir.exists()){
            throw new AssertionError("The source file '${sourceDir.canonicalPath}' doesn't exist.")
        }
        try {
            // Copy all resources except the individual test .gradle files
            // Do copy settings.gradle.
            FileUtils.copyDirectory(sourceDir, parent, new FileFilter() {
               public boolean accept (File pathname) {
                   return (!pathname.getPath().endsWith(".gradle") ||
                    pathname.getPath().endsWith("settings.gradle") ||
                        pathname.getPath().endsWith("build.gradle"))
               }
            });

            // copy the needed gradle build and property files
            File sourceFile = new File(sourceDir, buildFilename)
            copyBuildFiles(sourceFile, parent)

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
