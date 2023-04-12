package io.openliberty.tools.gradle;

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
import org.w3c.dom.Node;

import java.net.HttpURLConnection;
import java.net.URL;

import io.openliberty.tools.common.plugins.util.OSUtil

public class TestMultiModuleLooseEar extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/multi-module-loose-ear-test")
    static File buildDir = new File(integTestDir, "/multi-module-loose-ear-test")
    static String buildFilename = "build.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runTasks(buildDir, 'libertyStop')
    }

    @Test
    public void test_loose_config_file_exists() {
        try {
            runTasks(buildDir, 'deploy')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy. " + e)
        }
        assert new File('build/testBuilds/multi-module-loose-ear-test/ejb-ear/build/wlp/usr/servers/ejbServer/apps/ejb-ear.ear.xml').exists() : 'looseApplication config file was not copied over to the liberty runtime'
    }
    /*
        Expected output to the XML
        <?xml version="1.0" encoding="UTF-8"?>
        <archive>
            <file sourceOnDisk="/Users/../../ci.gradle/build/testBuilds/multi-module-loose-ear-test/ejb-ear/build/tmp/ear/application.xml" targetInArchive="/META-INF/application.xml"/>
            <file sourceOnDisk="/Users/../../ci.gradle/build/testBuilds/multi-module-loose-ear-test/ejb-ear/build/libs/1557427330063525/commons-text-1.1.jar" targetInArchive="/WEB-INF/lib/commons-text-1.1.jar"/>
            <archive targetInArchive="/ejb-war.war">
                <dir sourceOnDisk="/Users/../../ci.gradle/build/testBuilds/multi-module-loose-ear-test/ejb-war/build/classes/java/main" targetInArchive="/WEB-INF/classes"/>
                <file sourceOnDisk="/Users/../../ci.gradle/build/testBuilds/multi-module-loose-ear-test/ejb-war/build/resources/tmp/META-INF/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
                <archive targetInArchive="/WEB-INF/lib/ejb-ejb.jar">
                    <dir sourceOnDisk="/Users/../../ci.gradle/build/testBuilds/multi-module-loose-ear-test/ejb-ejb/build/classes/java/main" targetInArchive="/"/>
                    <file sourceOnDisk="/Users/../../ci.gradle/build/testBuilds/multi-module-loose-ear-test/ejb-ejb/build/resources/tmp/META-INF/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
                </archive>
            </archive>
            <file sourceOnDisk="/Users/../../ci.gradle/build/testBuilds/multi-module-loose-ear-test/loose-ear-test/ejb-ear/build/tmp/META-INF/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
        </archive>
    */
    @Test
    public void test_loose_config_file_contents_are_correct(){
        File on = new File("build/testBuilds/multi-module-loose-ear-test/ejb-ear/build/wlp/usr/servers/ejbServer/apps/ejb-ear.ear.xml");
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
        Assert.assertEquals("Number of <dir/> element ==>", 0, nodes.getLength());

        expression = "/archive/archive";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <archive/> element ==>", 1, nodes.getLength());

        String ejbWar = "/ejb-war.war"

        Assert.assertTrue(ejbWar.equals(nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue()))

        expression = "/archive/archive/archive";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <archive/> element ==>", 1, nodes.getLength());

        String ejbJar = "/WEB-INF/lib/ejb-ejb.jar"

        Assert.assertTrue(ejbJar.equals(nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue()))

        expression = "/archive/file";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <file/> element ==>", 3, nodes.getLength());

        // check that lib dependency is referenced in copyLibsDirectory location
        for (Node node : nodes) {
            if (node.getAttributes().getNamedItem("targetInArchive").getNodeValue().equals("/WEB-INF/lib/commons-text-1.1.jar")) {
                // Check that dependencies are located in the test build dir specified by copyLibsDirectory. Otherwise they would be located in the gradle cache somewhere.
                String nodeValue = node.getAttributes().getNamedItem("sourceOnDisk").getNodeValue();

                if (OSUtil.isWindows()) {
                    Assert.assertTrue(nodeValue.endsWith("\\commons-text-1.1.jar") && nodeValue.contains("\\multi-module-loose-ear-test\\ejb-ear\\build\\libs\\"))
                } else {
                    Assert.assertTrue(nodeValue.endsWith("/commons-text-1.1.jar") && nodeValue.contains("/multi-module-loose-ear-test/ejb-ear/build/libs/"))
                }
            }
        }
    }
}
