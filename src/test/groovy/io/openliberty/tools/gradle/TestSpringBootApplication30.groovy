/*
 * (C) Copyright IBM Corporation 2023, 2024.
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
import org.junit.*
import org.junit.rules.TestName

import static org.junit.Assert.assertTrue

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

public class TestSpringBootApplication30 extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/sample.springboot3")
    static String buildFilename = "springboot_3_archive.gradle"

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
    public void test_spring_boot_apps_30() {
        try {
            runTasks(buildDir, 'deploy', 'libertyStart')

            String webPage = new URL("http://localhost:9080").getText()
            Assert.assertEquals("Did not get expected http response.","Hello!", webPage)
            Assert.assertTrue('defaultServer/dropins has app deployed',
                    new File(buildDir, 'build/wlp/usr/servers/defaultServer/dropins').list().size() == 0)
            Assert.assertTrue('defaultServer/configDropins/defaults has no config',
                    new File(buildDir, 'build/wlp/usr/servers/defaultServer/configDropins/defaults').list().size() == 1)
            File configDropinsDir=new File(buildDir, 'build/wlp/usr/servers/defaultServer/configDropins/defaults')
            File configDropinsFile=new File(configDropinsDir,configDropinsDir.list().getAt(0))
            try (FileInputStream input = new FileInputStream(configDropinsFile)) {
                // get configDropins XML Document
                DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance();
                inputBuilderFactory.setIgnoringComments(true);
                inputBuilderFactory.setCoalescing(true);
                inputBuilderFactory.setIgnoringElementContentWhitespace(true);
                inputBuilderFactory.setValidating(false);
                inputBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
                inputBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder();
                Document inputDoc=inputBuilder.parse(input);

                // parse configDropins XML Document
                XPath xPath = XPathFactory.newInstance().newXPath();
                String expression = "/server/springBootApplication";
                NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
                Assert.assertTrue("Number of <springBootApplication/> element ==>", nodes.getLength()>0);

                Node node = nodes.item(0);
                Element element = (Element)node;
                Assert.assertEquals("Value of the 1st <springBootApplication/> ==>"+element.getAttribute("location"), "thin-${testName.getMethodName()}-1.0-SNAPSHOT.jar".toString(), element.getAttribute("location"));
            }
            Assert.assertTrue('no app in apps folder',
                    new File(buildDir, "build/wlp/usr/servers/defaultServer/apps/thin-${testName.getMethodName()}-1.0-SNAPSHOT.jar").exists() )
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy.", e)
        }
    }

    @Test
    public void test_spring_boot_classifier_apps_30() {
        try {
            runTasks(buildDir, 'deploy', 'libertyStart')

            String webPage = new URL("http://localhost:9080").getText()
            Assert.assertEquals("Did not get expected http response.","Hello!", webPage)
            Assert.assertTrue('defaultServer/dropins has app deployed',
                    new File(buildDir, 'build/wlp/usr/servers/defaultServer/dropins').list().size() == 0)
            Assert.assertTrue('no app in apps folder',
                    new File(buildDir, "build/wlp/usr/servers/defaultServer/apps/thin-${testName.getMethodName()}-1.0-SNAPSHOT-test.jar").exists() )
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy.", e)
        }
    }

    @Test
    public void test_spring_boot_classifier_apps_30_no_feature() {
        try {
            runTasks(buildDir, 'deploy')

            Assert.assertTrue('defaultServer/dropins has app deployed',
                    new File(buildDir, 'build/wlp/usr/servers/defaultServer/dropins').list().size() == 0)
            Assert.assertTrue('no app in apps folder',
                    new File(buildDir, "build/wlp/usr/servers/defaultServer/apps/thin-${testName.getMethodName()}-1.0-SNAPSHOT-test.jar").exists() )
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy.", e)
        }
    }

    @Test
    public void test_spring_boot_war_apps_30() {
        try {
            runTasks(buildDir, 'deploy', 'libertyStart')

            String webPage = new URL("http://localhost:9080").getText()
            Assert.assertEquals("Did not get expected http response.","Hello!", webPage)
            Assert.assertTrue('defaultServer/dropins has app deployed',
                    new File(buildDir, 'build/wlp/usr/servers/defaultServer/dropins').list().size() == 0)
            Assert.assertTrue('no app in apps folder',
                    new File(buildDir, "build/wlp/usr/servers/defaultServer/apps/thin-${testName.getMethodName()}-1.0-SNAPSHOT.war").exists() )
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy.", e)
        }
    }

    @Test
    public void test_spring_boot_war_classifier_apps_30() {
        try {
            runTasks(buildDir, 'deploy', 'libertyStart')

            String webPage = new URL("http://localhost:9080").getText()
            Assert.assertEquals("Did not get expected http response.","Hello!", webPage)
            Assert.assertTrue('defaultServer/dropins has app deployed',
                    new File(buildDir, 'build/wlp/usr/servers/defaultServer/dropins').list().size() == 0)
            Assert.assertTrue('no app in apps folder',
                    new File(buildDir, "build/wlp/usr/servers/defaultServer/apps/thin-${testName.getMethodName()}-1.0-SNAPSHOT-test.war").exists() )
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy.", e)
        }
    }

    @Test
    public void test_spring_boot_dropins_30() {
        try {
            runTasks(buildDir, 'deploy', 'libertyStart')
            String webPage = new URL("http://localhost:9080").getText()
            Assert.assertEquals("Did not get expected http response.","Hello!", webPage)
            Assert.assertTrue('defaultServer/dropins/spring has no app',
                    new File(buildDir, "build/wlp/usr/servers/defaultServer/dropins/spring/thin-${testName.getMethodName()}-1.0-SNAPSHOT.jar").exists())
            Assert.assertTrue('apps folder should be empty',
                    new File(buildDir, "build/wlp/usr/servers/defaultServer/apps").list().size() == 0 )
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy.", e)
        }
    }

    @Test
    public void test_spring_boot_plugins_dsl_apps_30() {
        try {
            runTasks(buildDir, 'deploy', 'libertyStart')

            String webPage = new URL("http://localhost:9080").getText()
            Assert.assertEquals("Did not get expected http response.","Hello!", webPage)
            Assert.assertTrue('defaultServer/dropins has app deployed',
                    new File(buildDir, 'build/wlp/usr/servers/defaultServer/dropins').list().size() == 0)
            Assert.assertTrue('no app in apps folder',
                    new File(buildDir, "build/wlp/usr/servers/defaultServer/apps/thin-${testName.getMethodName()}-1.0-SNAPSHOT.jar").exists() )
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy.", e)
        }
    }

    @Test
    public void test_spring_boot_with_springbootapplication_apps_30() {
        try {
            runTasks(buildDir, 'deploy', 'libertyStart')

            String webPage = new URL("http://localhost:9080").getText()
            Assert.assertEquals("Did not get expected http response.","Hello!", webPage)
            Assert.assertTrue('defaultServer/dropins has app deployed',
                    new File(buildDir, 'build/wlp/usr/servers/defaultServer/dropins').list().size() == 0)
            Assert.assertTrue('generated thin app name not same as specified in server.xml <SpingBootApplication/> node',
                    new File(buildDir, "build/wlp/usr/servers/defaultServer/apps/${testName.getMethodName()}-1.0-SNAPSHOT.jar").exists() )
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy.", e)
        }
    }

    @Test
    public void test_spring_boot_with_springbootapplication_nodes_apps_30() {
        try {
            BuildResult result = runTasksFailResult(buildDir, 'deploy', 'libertyStart')
            String output = result.getOutput()
            assertTrue(output.contains("Found multiple springBootApplication elements specified in the server configuration file"))
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy.", e)
        }
    }

    @Test
    public void test_spring_boot_with_springbootapplication_nodes_apps_include_30() {
        try {
            BuildResult result = runTasksFailResult(buildDir, 'deploy', 'libertyStart')
            String output = result.getOutput()
            assertTrue(output.contains("Found multiple springBootApplication elements specified in the server configuration in files"))
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy.", e)
        }
    }


}
