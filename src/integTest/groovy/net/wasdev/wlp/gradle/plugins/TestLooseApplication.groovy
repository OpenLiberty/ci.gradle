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

public class TestLooseApplication extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/sample.servlet")
    static File buildDir = new File(integTestDir, "/test-loose-application")
    static String buildFilename = "TestLooseApplication.gradle"

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
            runTasks(buildDir, 'installApps')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task installApps. " + e)
        }
        assert new File('build/testBuilds/test-loose-application/build/wlp/usr/servers/LibertyProjectServer/apps/sample.servlet.war.xml').exists() : 'looseApplication config file was not copied over to the liberty runtime'
    }

    @Test
    public void test_loose_config_file_contents_are_correct(){
      File on = new File("build/testBuilds/test-loose-application/build/wlp/usr/servers/LibertyProjectServer/apps/sample.servlet.war.xml");
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
      String expression = "/archive/dir";
      NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
      Assert.assertEquals("Number of <dir/> element ==>", 2, nodes.getLength());

      expression = "/archive/archive";
      nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
      Assert.assertEquals("Number of <archive/> element ==>", 0, nodes.getLength());

      expression = "/archive/file";
      nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
      Assert.assertEquals("Number of <file/> element ==>", 3, nodes.getLength());

      Assert.assertEquals("archive targetInArchive attribute value", "/WEB-INF/lib/commons-text-1.1.jar",
              nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue());

      Assert.assertEquals("archive targetInArchive attribute value", "/WEB-INF/lib/commons-lang3-3.5.jar",
              nodes.item(1).getAttributes().getNamedItem("targetInArchive").getNodeValue());
    }
}
