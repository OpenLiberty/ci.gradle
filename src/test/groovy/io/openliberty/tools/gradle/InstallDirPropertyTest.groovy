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

import org.junit.BeforeClass
import org.junit.Test
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

class InstallDirPropertyTest extends AbstractIntegrationTest {
    static File resourceDir = new File("build/resources/test/install-dir-property-test")
    static File buildDir = new File(integTestDir, "/InstallDirPropertyTest")
    static String buildFilename = "testInstallDirProperty.gradle"

    static File runtimeInstallDir = new File(buildDir, 'wlp')
    static File parentProjectBuildDir = new File(buildDir, 'build')
    static File subProjectBuildDir = new File(buildDir, 'webapp/build')

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @Test
    void test_installLiberty_invalid_installDir_fail() {
        BuildResult result = GradleRunner.create()
            .withProjectDir(buildDir)
            .forwardOutput()
            .withArguments('installLiberty', '-i', '-s')
            .buildAndFail()

        String output = result.getOutput()
        assert output.contains("Please specify a valid installDir") : "Expected installLiberty to fail with GradleException"
    }
}