/*
 * (C) Copyright IBM Corporation 2018.
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

import groovy.xml.QName
import org.junit.*
import org.junit.rules.TestName

public class TestSpringBootApplication15 extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/sample.springboot")
    static String buildFilename = "springboot_archive.gradle"

    File buildDir;

    @Rule
    public TestName testName = new TestName();

    @Before
    public void setup() {
        buildDir = new File(integTestDir, "/" + testName.getMethodName())
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, testName.getMethodName() + '.gradle')
    }

    @After
    public void tearDown() throws Exception {
        runTasks(buildDir,'libertyStop')
    }

    @Test
    public void test_spring_boot_apps_15() {
        try {
            runTasks(buildDir, 'deploy', 'libertyStart')
            String webPage = new URL("http://localhost:9080").getText()
            Assert.assertEquals("Did not get expected http response.","Hello!", webPage)
            Assert.assertTrue('defaultServer/dropins has app deployed',
                    new File(buildDir, 'build/wlp/usr/servers/defaultServer/dropins').list().size() == 0)
            Assert.assertTrue('no app in apps folder',
                    new File(buildDir, "build/wlp/usr/servers/defaultServer/apps/thin-${testName.getMethodName()}-1.0-SNAPSHOT.jar").exists() )
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy. " + e)
        }
    }

    @Test
    public void test_spring_boot_classifier_apps_15() {
        try {
            runTasks(buildDir, 'deploy', 'libertyStart')
            String webPage = new URL("http://localhost:9080").getText()
            Assert.assertEquals("Did not get expected http response.","Hello!", webPage)
            Assert.assertTrue('defaultServer/dropins has app deployed',
                    new File(buildDir, 'build/wlp/usr/servers/defaultServer/dropins').list().size() == 0)
            Assert.assertTrue('no app in apps folder',
                    new File(buildDir, "build/wlp/usr/servers/defaultServer/apps/thin-${testName.getMethodName()}-1.0-SNAPSHOT-test.jar").exists() )
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy. " + e)
        }
    }

    @Test
    public void test_spring_boot_dropins_15() {
        try {
            runTasks(buildDir, 'deploy', 'libertyStart')
            String webPage = new URL("http://localhost:9080").getText()
            Assert.assertEquals("Did not get expected http response.","Hello!", webPage)
            Assert.assertTrue('defaultServer/dropins/spring has no app',
                    new File(buildDir, "build/wlp/usr/servers/defaultServer/dropins/spring/thin-${testName.getMethodName()}-1.0-SNAPSHOT.jar").exists())
            Assert.assertTrue('apps folder should be empty',
                    new File(buildDir, "build/wlp/usr/servers/defaultServer/apps").list().size() == 0 )
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy. " + e)
        }
    }
}
