/*
 * (C) Copyright IBM Corporation 2017, 2018.
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

package net.wasdev.wlp.gradle.plugins;

import java.io.File
import org.junit.Test
import org.junit.BeforeClass
import org.apache.commons.io.FileUtils
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection

/**
 * Runs tests on the arquillian-tests project test cases. Any failures will result in
 * this test failing, but you'll have to look at the cause in the arquillian-tests folder.
 */
class ConfigureArquillianTest extends AbstractIntegrationTest {

    static File resourceDir = new File("build/resources/integrationTest/arquillian-tests")
    static File buildDir = new File(integTestDir, "/arquillian-tests")
    static String buildFilename = "build.gradle"

    static final ERROR_MESSAGE = "Build(s) failed. Check the build/testBuilds/arquillian-tests folder for more information on the cause, and potential test failures."

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        FileUtils.copyDirectory(resourceDir, buildDir);
        copyFile(new File("build/gradle.properties"), new File(buildDir, 'gradle.properties'))
    }

    @Test
    public void test_build() {
        try {
            GradleConnector gradleConnector = GradleConnector.newConnector()
            gradleConnector.forProjectDirectory(buildDir)
            ProjectConnection connection = gradleConnector.connect()

            try {
                BuildLauncher build = connection.newBuild()
                build.withArguments("-i", "-s")
                build.forTasks("build")
                build.setStandardOutput(System.out)
                build.setStandardError(System.out)
                build.run()
            }
            finally {
                connection?.close()
            }
        } catch (Exception e) {
            e.printStackTrace()
            throw new AssertionError(ERROR_MESSAGE)
        }
    }

    private static void watch(final Process process) {
        new Thread() {
                    public void run() {
                        BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line = null;
                        try {
                            while ((line = input.readLine()) != null) {
                                System.out.println(line);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
    }
}
