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

import java.net.HttpURLConnection;
import java.net.URL;

public class TestLooseEarApplicationEarlibs extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/loose-ear-earlibs-test")
    static File buildDir = new File(integTestDir, "/test-loose-ear-application-earlibs")
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
        assert new File('build/testBuilds/test-loose-ear-application-earlibs/SampleEAR/build/wlp/usr/servers/ejbServer/apps/SampleEAR.ear.xml').exists() : 'looseApplication config file was not copied over to the liberty runtime'
    }
/*
  Expected output to the XML
    <?xml version="1.0" encoding="UTF-8"?>
    <archive>
        <dir sourceOnDisk="/Users/mbowersox/git/ci.gradle/build/testBuilds/loose-ear-earlibs-test/SampleEAR/src/main/application" targetInArchive="/"/>
        <file sourceOnDisk="/Users/mbowersox/git/ci.gradle/build/testBuilds/loose-ear-earlibs-test/SampleEAR/build/tmp/ear/application.xml" targetInArchive="/META-INF/application.xml"/>
        <archive targetInArchive="/SampleEJB.jar">
            <dir sourceOnDisk="/Users/mbowersox/git/ci.gradle/build/testBuilds/loose-ear-earlibs-test/SampleEJB/build/classes/java/main" targetInArchive="/"/>
            <file sourceOnDisk="/Users/mbowersox/git/ci.gradle/build/testBuilds/loose-ear-earlibs-test/SampleEAR/build/tmp/META-INF/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
        </archive>
        <archive targetInArchive="/SampleWAR.war">
            <dir sourceOnDisk="/Users/mbowersox/git/ci.gradle/build/testBuilds/loose-ear-earlibs-test/SampleWAR/build/classes/java/main" targetInArchive="/WEB-INF/classes"/>
            <file sourceOnDisk="/Users/mbowersox/git/ci.gradle/build/testBuilds/loose-ear-earlibs-test/SampleEAR/build/tmp/META-INF/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
        </archive>
        <archive targetInArchive="/SampleWAR2.war">
            <dir sourceOnDisk="/Users/mbowersox/git/ci.gradle/build/testBuilds/loose-ear-earlibs-test/SampleWAR2/build/classes/java/main" targetInArchive="/WEB-INF/classes"/>
            <file sourceOnDisk="/Users/mbowersox/git/ci.gradle/build/testBuilds/loose-ear-earlibs-test/SampleEAR/build/tmp/META-INF/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
        </archive>
        <file sourceOnDisk="/Users/mbowersox/git/ci.gradle/build/testBuilds/loose-ear-earlibs-test/SampleEAR/build/tmp/META-INF/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
    </archive>


*/
    @Test
    public void test_loose_config_file_contents_are_correct(){
        File on = new File("build/testBuilds/test-loose-ear-application-earlibs/SampleEAR/build/wlp/usr/servers/ejbServer/apps/SampleEAR.ear.xml");
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
        Assert.assertEquals("Number of <archive/> element ==>", 3, nodes.getLength());

        String sampleEJB = "/SampleEJB.jar"
        String sampleWAR = "/SampleWAR.war"
        String sampleWAR2 = "/SampleWAR2.war"
        

        Assert.assertTrue(
                sampleWAR.equals(nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue()) ||
                    sampleWAR2.equals(nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue()) ||
                        sampleEJB.equals(nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue()))

        Assert.assertTrue(
                sampleWAR.equals(nodes.item(1).getAttributes().getNamedItem("targetInArchive").getNodeValue()) ||
                    sampleWAR2.equals(nodes.item(1).getAttributes().getNamedItem("targetInArchive").getNodeValue()) ||
                        sampleEJB.equals(nodes.item(1).getAttributes().getNamedItem("targetInArchive").getNodeValue()))

        Assert.assertTrue(
                sampleWAR.equals(nodes.item(2).getAttributes().getNamedItem("targetInArchive").getNodeValue()) ||
                    sampleWAR2.equals(nodes.item(2).getAttributes().getNamedItem("targetInArchive").getNodeValue()) ||
                        sampleEJB.equals(nodes.item(2).getAttributes().getNamedItem("targetInArchive").getNodeValue()))

        expression = "/archive/file";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <file/> element ==>", 2, nodes.getLength());

    }

    @Test
    public void test_start_with_timeout_success() {
        try {
            runTasks(buildDir, 'libertyStart')
            Assert.assertTrue(getURLResponseCode("http://localhost:9080/SampleWAR") == 200);
            Assert.assertTrue(getURLResponseCode("http://localhost:9080/SampleWAR2") == 200);
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStart. "+ e)
        }
    }

    private int getURLResponseCode(String address){
        URL url = new URL(address);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        return connection.getResponseCode();
    }
}
