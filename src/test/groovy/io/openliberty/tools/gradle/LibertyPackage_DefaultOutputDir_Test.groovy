/*
 * (C) Copyright IBM Corporation 2022.
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
package io.openliberty.tools.gradle

import static org.junit.Assert.*

import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.Test

import org.gradle.testkit.runner.BuildResult

class LibertyPackage_DefaultOutputDir_Test extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/liberty-package-test")
    static File buildDir = new File(integTestDir, "/liberty-package-defaultOutputDir-test")
    static File buildFilename = new File(resourceDir, "liberty-package-defaultOutputDir.gradle")

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        copyBuildFiles(buildFilename, buildDir)
        copySettingsFile(resourceDir, buildDir)
        try {
            runTasks(buildDir, 'installLiberty', 'libertyStart')
        } catch (Exception e) {
            throw new AssertionError ("Error during server start lifecycle. "+ e)
        }
    }

    @AfterClass
    public static void tearDown() {
        try {
            runTasks(buildDir, 'libertyStop')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStop. "+ e)
        }
    }

    @Test
    public void test_package_archiveZipPath() {
        try{
           BuildResult result = runTasksResult(buildDir, 'libertyPackage')

           assertFalse("Running server message found in logs.", result.getOutput().contains('[ant:server] Server defaultServer package failed. It must be stopped before it can be packaged.'))
        } catch (Exception e) {
           throw new AssertionError ("Fail on task libertyPackage. "+ e)
        }
    }
}
