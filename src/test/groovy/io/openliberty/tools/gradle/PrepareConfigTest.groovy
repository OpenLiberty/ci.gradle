/*
 * (C) Copyright IBM Corporation 2026.
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
import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilderFactory

class PrepareConfigTest extends AbstractIntegrationTest {
    static File resourceDir = new File("build/resources/test/prepare-config-test")
    static File buildDir = new File(integTestDir, "/prepare-config-test")
    static String buildFilename = "testPrepareConfig.gradle"
    
    private static final String CONFIG_FILE_PATH = "build/liberty-plugin-config.xml"
    private static final String MOCK_SERVER_PATH = "build/tmp/wlp/usr/servers/testServer"
    private static final String SERVER_PATH_PREFIX = "/liberty-plugin-config/servers/server"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
        runTasks(buildDir, 'libertyPrepareConfig')
    }

    @Test
    public void test_config_file_generated() {
        File configFile = new File(buildDir, CONFIG_FILE_PATH)
        assertTrue("liberty-plugin-config.xml should exist", configFile.exists())
    }

    @Test
    public void test_xml_content_validation() {
        def xpathTests = [
            [path: "$SERVER_PATH_PREFIX/serverName", expected: "testServer", message: "Server name"],
            [path: "/liberty-plugin-config/installDirectory", contains: ["tmp", "wlp"], message: "Install directory"],
            [path: "$SERVER_PATH_PREFIX/userDirectory", contains: ["tmp", "wlp", "usr"], message: "User directory"],
            [path: "$SERVER_PATH_PREFIX/serverDirectory", contains: ["tmp", "testServer"], message: "Server directory"],
            [path: "$SERVER_PATH_PREFIX/configDirectory", contains: ["liberty/config"], message: "Config directory"],
            [path: "$SERVER_PATH_PREFIX/applications/application/appsDirectory", expected: "apps", message: "Apps directory"],
            [path: "$SERVER_PATH_PREFIX/looseApplication", expected: "true", message: "Loose application"],
            [path: "$SERVER_PATH_PREFIX/applications/application/projectType", expected: "war", message: "Project type"]
        ]

        xpathTests.each { test ->
            String value = getXPathValue(test.path)
            assertNotNull("${test.message} should be present", value)
            
            if (test.expected) {
                assertEquals("${test.message} should be ${test.expected}", test.expected, value)
            } else if (test.contains) {
                test.contains.each { substring ->
                    assertTrue("${test.message} should contain ${substring}", value.contains(substring))
                }
            }
        }
    }

    @Test
    public void test_mock_server_structure() {
        def paths = [
            [path: "build/tmp", name: "tmp"],
            [path: "build/tmp/wlp", name: "wlp"],
            [path: "build/tmp/wlp/usr", name: "usr"],
            [path: "build/tmp/wlp/usr/servers", name: "servers"],
            [path: MOCK_SERVER_PATH, name: "testServer"]
        ]

        paths.each { pathInfo ->
            File dir = new File(buildDir, pathInfo.path)
            assertTrue("${pathInfo.name} directory should exist", dir.exists())
            assertTrue("${pathInfo.name} should be a directory", dir.isDirectory())
        }
    }

    @Test
    public void test_config_files_copied() {
        File mockServerDir = new File(buildDir, MOCK_SERVER_PATH)
        
        def files = [
            [name: "server.xml", contentChecks: ["featureManager", "jakartaee-9.1", "microProfile-5.0"]],
            [name: "bootstrap.properties", contentChecks: ["default.http.port", "default.https.port"]]
        ]

        files.each { fileInfo ->
            File file = new File(mockServerDir, fileInfo.name)
            assertTrue("${fileInfo.name} should be copied to mock server", file.exists())
            
            String content = file.text
            fileInfo.contentChecks.each { check ->
                assertTrue("${fileInfo.name} should contain ${check}", content.contains(check))
            }
        }
    }

    private String getXPathValue(String expression) {
        File configFile = new File(buildDir, CONFIG_FILE_PATH)
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
        
        // Security features
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        factory.setXIncludeAware(false)
        factory.setExpandEntityReferences(false)

        Document doc = factory.newDocumentBuilder().parse(configFile)
        return javax.xml.xpath.XPathFactory.newInstance().newXPath().evaluate(expression, doc)
    }
}

