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

class InstallLiberty_installDir_missing_wlp_Test extends AbstractIntegrationTest {
    static File resourceDir = new File("build/resources/test/install-dir-property-test/installDir-missing-wlp")

    static File buildDir = new File(integTestDir, "/InstallLiberty_installDir_missing_wlp")
    static String expectedPropertyDir = new File(buildDir, 'installDir-valid-install/build/wlp').getCanonicalPath()

    static String buildFilename = "build.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @Test
    void test_installLiberty_installDir_missing_wlp() {
        BuildResult result = GradleRunner.create()
            .withProjectDir(buildDir)
            .forwardOutput()
            .withArguments('installLiberty', '-i', '-s')
            .build()

        String output = result.getOutput()
        assert output.contains("path does not reference a wlp folder") : "Expected warning about installDir path not containing wlp"
    }

    @Test
    void test_installLiberty_installDir_cli_property_wlp() {
        BuildResult result = GradleRunner.create()
            .withProjectDir(buildDir)
            .forwardOutput()
            .withArguments('installLiberty', '-Pliberty.installDir=installDir-valid-install/build/wlp', '-i', '-s')
            .build()

        String output = result.getOutput()
        assert output.contains("installDir project property detected. Using $expectedPropertyDir")
    }

    @Test
    void test_installLiberty_installDir_cli_property() {
        BuildResult result = GradleRunner.create()
            .withProjectDir(buildDir)
            .forwardOutput()
            .withArguments('installLiberty', '-Pliberty.installDir=installDir-valid-install/build', '-i', '-s')
            .build()

        String output = result.getOutput()
        assert output.contains("installDir project property detected. Using $expectedPropertyDir".drop(4))
        assert output.contains("Using path $expectedPropertyDir instead.")
    }
}