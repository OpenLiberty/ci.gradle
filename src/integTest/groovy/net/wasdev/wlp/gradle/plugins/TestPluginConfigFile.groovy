package net.wasdev.wlp.gradle.plugins;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import java.io.File;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class TestPluginConfigFile extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/sample.servlet")
    static File buildDir = new File(integTestDir, "/test-plugin-config-file")
    static String buildFilename = "testPluginConfigFile.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        if(test_mode == "offline"){
            WLP_DIR.replace("\\","/")
        }
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runTasks(buildDir, 'libertyStop')
    }

    @Test
    public void test_loose_config_file_exists() {
        try {
            runTasks(buildDir, 'libertyCreate')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task installApps. " + e)
        }
        assert new File('build/testBuilds/test-plugin-config-file/build/liberty-plugin-config.xml').exists() : 'liberty plugin config file was not created in the build directory'
    }

    @Test
    public void test_liberty_plugin_config_file_contents_are_correct(){
      File on = new File("build/testBuilds/test-plugin-config-file/build/liberty-plugin-config.xml");
      FileInputStream input = new FileInputStream(on);

      // get input XML Document
      DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance();
      inputBuilderFactory.setIgnoringComments(true);
      inputBuilderFactory.setCoalescing(true);
      inputBuilderFactory.setIgnoringElementContentWhitespace(true);
      inputBuilderFactory.setValidating(false);
      DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder();
      Document inputDoc=inputBuilder.parse(input);

      // parse input XML Document
      XPath xPath = XPathFactory.newInstance().newXPath();
      String expression = "/liberty-plugin-config/servers";
      NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
      Assert.assertEquals("Number of <servers/> element ==>", 1, nodes.getLength());

      expression = "/liberty-plugin-config/servers/server";
      nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
      Assert.assertEquals("Number of <server/> element ==>", 1, nodes.getLength());

      expression = "/liberty-plugin-config/servers/server/serverName";
      nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
      Assert.assertEquals("Number of <serverName/> element ==>", 1, nodes.getLength());

      Assert.assertEquals("correct serverName value", "LibertyProjectServer",
              nodes.item(0).getTextContent());

      expression = "/liberty-plugin-config/servers/server/configFile";
      nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
      Assert.assertEquals("Number of <configFile/> element ==>", 1, nodes.getLength());

      Assert.assertTrue("correct configFile value", nodes.item(0).getTextContent().replace("\\", "/").contains("/src/main/liberty/config/server-apps-test.xml"));
    }
}
