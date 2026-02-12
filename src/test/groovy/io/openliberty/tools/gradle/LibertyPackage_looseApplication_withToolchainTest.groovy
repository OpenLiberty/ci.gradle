/*
 * (C) Copyright IBM Corporation 2017.
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

import org.gradle.testkit.runner.BuildResult
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertTrue

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LibertyPackage_looseApplication_withToolchainTest extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/liberty-package-test")
    static File buildDir = new File(integTestDir, "/liberty-package-looseApplication-toolchain-test")
    static File buildFilename = new File(resourceDir, "liberty-package-looseApplication-toolchain.gradle")

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        copyBuildFiles(buildFilename, buildDir)
        copySettingsFile(resourceDir, buildDir)
        try {
            BuildResult result= runTasksResult(buildDir, 'installLiberty', 'libertyStart', 'libertyStop')
            String consoleLogOutput = result.getOutput();
            assertTrue("Toolchain with version message not found in logs.", consoleLogOutput.contains('CWWKM4100I: Using toolchain from build context. JDK Version specified is 8'))
            assertTrue("Toolchain honored message for task create not found in logs.", consoleLogOutput.contains('CWWKM4101I: The :libertyCreate task is using the configured toolchain JDK'))
            assertTrue("Toolchain honored message for task stop not found in logs.", consoleLogOutput.contains('CWWKM4101I: The :libertyStop task is using the configured toolchain JDK'))
        } catch (Exception e) {
            throw new AssertionError ("Fail on task installLiberty.", e)
        }
    }

    @Test
    public void test_package_archiveZipPath() {
        try{
           runTasks(buildDir, 'libertyPackage')

           def file = new File(buildDir, 'build/libs/testPackage.zip')

           assert file.exists() : "file not found"
           assert file.canRead() : "file cannot be read"

        } catch (Exception e) {
           throw new AssertionError ("Fail on task libertyPackage.", e)
        }
    }
}
