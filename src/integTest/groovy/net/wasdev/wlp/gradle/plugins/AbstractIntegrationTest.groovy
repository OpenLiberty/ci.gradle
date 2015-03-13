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
    static File buildFile
    static final String WLP_DIR = System.getProperty("wlpInstallDir").replace("\\", "/")

    @BeforeClass
    public static void setup() {
        deleteDir(integTestDir)
        createDir(integTestDir)
        buildFile = createFile(integTestDir, 'build.gradle')

        buildFile << """
buildscript {
    repositories {
        mavenCentral()
        maven {
            name = 'Sonatype Nexus Snapshots'
            url = 'https://oss.sonatype.org/content/repositories/snapshots/'
        }
    }
    dependencies {
        classpath files('../libs/liberty-gradle-plugin-1.0-SNAPSHOT.jar')
        classpath files('$WLP_DIR'+'/bin/tools/ws-server.jar')
        classpath 'net.wasdev.wlp.ant:wlp-anttasks:1.1-SNAPSHOT'
    }
}
"""
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

    protected static File createFile(File parent, String filename) {
        File file = new File(parent, filename)
        if (!file.createNewFile()) {
            throw new AssertionError("Unable to create file '${file.canonicalPath}'.")
        }
        return file
    }

    protected static void runTasks(File projectDir, String... tasks) {
        GradleConnector gradleConnector = GradleConnector.newConnector()
        gradleConnector.forProjectDirectory(projectDir)
        ProjectConnection connection = gradleConnector.connect()
        
        try {
            BuildLauncher build = connection.newBuild()
            build.forTasks(tasks)
            build.run()
        }
        finally {
            connection?.close()
        }
    }
}