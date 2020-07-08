/*
 * (C) Copyright IBM Corporation 2020.
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

import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import org.junit.BeforeClass
import org.junit.Test
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class InstallLiberty_installDir_full_lifecycle_Test extends AbstractIntegrationTest {
    static File resourceDir = new File("build/resources/test/install-dir-property-test/installDir-full-lifecycle")

    static File buildDir = new File(integTestDir, "/InstallLiberty_installDir_full_lifecycle")
    static String buildFilename = "build.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @Test
    void test1_installLiberty() {
        BuildResult result = GradleRunner.create()
            .withProjectDir(buildDir)
            .forwardOutput()
            .withArguments('installLiberty', '-i', '-s')
            .build()

        String output = result.getOutput()
        assert output.contains("Liberty is already installed at") : "Expected installLiberty to detect existing installation at installDir"
    }

    @Test
    void test2_start_stop() {
        try {
            runTasks(buildDir, 'libertyStart')
            runTasks(buildDir, 'libertyStop')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStart. "+e)
        }
    }

    @Test
    void test3_uninstallFeature() {
        try{
           runTasks(buildDir, 'UninstallFeature')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task UninstallFeature. "+e)
        }
    }
}