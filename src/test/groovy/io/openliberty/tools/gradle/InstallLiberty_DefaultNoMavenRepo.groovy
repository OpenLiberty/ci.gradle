/*
 * (C) Copyright IBM Corporation 2019
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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

import org.junit.BeforeClass
import org.junit.Test
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ResolveException

class InstallLiberty_DefaultNoMavenRepo extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/liberty-test")
    static File buildDir = new File(integTestDir, "/InstallLiberty_DefaultNoMavenRepo")
    static String buildFilename = "install_liberty_default_no_maven_repo.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @Test
    public void test_installLiberty_no_maven_repo_fail() {
        BuildResult result = GradleRunner.create()
            .withProjectDir(buildDir)
            .forwardOutput()
            .withArguments('installLiberty', '-i', '-s')
            .buildAndFail()

        String output = result.getOutput()
        assert output.contains("org.gradle.api.artifacts.ResolveException") : "Expected installLiberty to fail with ResolveException"
    }
}
