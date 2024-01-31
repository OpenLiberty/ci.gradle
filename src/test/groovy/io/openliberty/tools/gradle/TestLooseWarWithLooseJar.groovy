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

import io.openliberty.tools.common.plugins.util.OSUtil

public class TestLooseWarWithLooseJar extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/loose-war-with-loose-jar")
    static File buildDir = new File(integTestDir, "/loose-war-with-loose-jar")
    static String buildFilename = "build.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @Test
    public void test_loose_config_file_exists() {
        try {
            runTasks(buildDir, 'deploy')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy.", e)
        }
        assert new File('build/testBuilds/loose-war-with-loose-jar/ejb-war/build/wlp/usr/servers/testServer/apps/ejb-war.war.xml').exists() : 'looseApplication config file was not copied over to the liberty runtime'
    }

    /*
        Expected output to the XML
        <?xml version="1.0" encoding="UTF-8"?>
        <archive>
            <dir sourceOnDisk="/Users/../../ci.gradle/build/testBuilds/loose-war-with-loose-jar/ejb-war/src/main/webapp" targetInArchive="/"/>
            <dir sourceOnDisk="/Users/../../ci.gradle/build/testBuilds/loose-war-with-loose-jar/build/classes/java/main" targetInArchive="/WEB-INF/classes/"/>
            <archive targetInArchive="/WEB-INF/lib/ejb-ejb.jar">
                <dir sourceOnDisk="/Users/../../ci.gradle/build/testBuilds/loose-war-with-loose-jar/ejb-ejb/build/classes/java/main" targetInArchive="/"/>
                <file sourceOnDisk="/Users/../../ci.gradle/build/testBuilds/loose-war-with-loose-jar/ejb-ejb/build/resources/tmp/META-INF/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
            </archive>
            <file sourceOnDisk="/Users/../.gradle/caches/modules-2/files-2.1/javax/javaee-api/7.0/51399f902cc27a808122edcbebfaa1ad989954ba/javaee-api-7.0.jar" targetInArchive="/WEB-INF/lib/javaee-api-7.0.jar"/>
            <file sourceOnDisk="/Users/../.gradle/caches/modules-2/files-2.1/com.sun.mail/javax.mail/1.5.0/ec2410fdf7e0a3022e7c2a2e6241039d1abc1e98/javax.mail-1.5.0.jar" targetInArchive="/WEB-INF/lib/javax.mail-1.5.0.jar"/>
            <file sourceOnDisk="/Users/../.m2/repository/javax/activation/activation/1.1/activation-1.1.jar" targetInArchive="/WEB-INF/lib/activation-1.1.jar"/>
            <file sourceOnDisk="/Users/../../ci.gradle/build/testBuilds/loose-war-with-loose-jar/ejb-war/build/tmp/META-INF/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
        </archive>
    */
    @Test
    public void test_loose_config_file_contents_are_correct() {
        File on = new File("build/testBuilds/loose-war-with-loose-jar/ejb-war/build/wlp/usr/servers/testServer/apps/ejb-war.war.xml");
        FileInputStream input = new FileInputStream(on);

        // get input XML Document
        DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance();
        inputBuilderFactory.setIgnoringComments(true);
        inputBuilderFactory.setCoalescing(true);
        inputBuilderFactory.setIgnoringElementContentWhitespace(true);
        inputBuilderFactory.setValidating(false);
        inputBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
        inputBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)  
        DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder();
        Document inputDoc=inputBuilder.parse(input);

        // parse input XML Document
        XPath xPath = XPathFactory.newInstance().newXPath();

        //Check for correct number of dir elements in loose war
        String expression = "/archive/dir";
        NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <dir/> element ==>", 2, nodes.getLength());

        //Check loose jar archive element is present
        expression = "/archive/archive";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <archive/> element ==>", 1, nodes.getLength());

        //Check loose jar targetLocation is correct
        Assert.assertEquals("sibling archive targetInArchive attribute value", "/WEB-INF/lib/ejb-ejb.jar",
            nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue()); 

        //Check loose jar contains correct amount of dir elements
        expression = "/archive/archive/dir";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <dir/> element ==>", 1, nodes.getLength());

        //Check loose jar classes dir location
        String nodeValue = nodes.item(0).getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
        if (OSUtil.isWindows()) {
            Assert.assertEquals("sibling archive targetInArchive attribute value", buildDir.getCanonicalPath() + "\\ejb-ejb\\build\\classes\\java\\main",
                nodes.item(0).getAttributes().getNamedItem("sourceOnDisk").getNodeValue()); 
        } else {
            Assert.assertEquals("sibling archive targetInArchive attribute value", buildDir.getCanonicalPath() + "/ejb-ejb/build/classes/java/main",
                nodes.item(0).getAttributes().getNamedItem("sourceOnDisk").getNodeValue()); 
        }
        
         //Check loose jar contains correct amount of file elements
        expression = "/archive/archive/file";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <file/> element ==>", 1, nodes.getLength());

        //Check loose jar manifest file location
        nodeValue = nodes.item(0).getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
        if (OSUtil.isWindows()) {
            Assert.assertEquals("sibling archive targetInArchive attribute value", buildDir.getCanonicalPath() + "\\ejb-ejb\\build\\resources\\tmp\\META-INF\\MANIFEST.MF",
                nodes.item(0).getAttributes().getNamedItem("sourceOnDisk").getNodeValue());
        } else {
            Assert.assertEquals("sibling archive targetInArchive attribute value", buildDir.getCanonicalPath() + "/ejb-ejb/build/resources/tmp/META-INF/MANIFEST.MF",
                nodes.item(0).getAttributes().getNamedItem("sourceOnDisk").getNodeValue());
        }
 

        //Check correct number of additional file elements are present
        expression = "/archive/file";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <file/> element ==>", 4, nodes.getLength());

        Assert.assertEquals("archive targetInArchive attribute value", "/WEB-INF/lib/javaee-api-7.0.jar",
            nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue()); 

        // Check that dependencies are not located in the test build dir since copyLibsDirectory not set. They will be located in the gradle cache somewhere.
        nodeValue = nodes.item(0).getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
        if (OSUtil.isWindows()) {
            assert nodeValue.endsWith("\\javaee-api-7.0.jar") && !nodeValue.contains("\\loose-war-with-loose-jar\\") : 'archive sourceOnDisk attribute value not correct'
        } else {
            assert nodeValue.endsWith("/javaee-api-7.0.jar") && !nodeValue.contains("/loose-war-with-loose-jar/") : 'archive sourceOnDisk attribute value not correct'
        }

        Assert.assertEquals("archive targetInArchive attribute value", "/WEB-INF/lib/javax.mail-1.5.0.jar",
            nodes.item(1).getAttributes().getNamedItem("targetInArchive").getNodeValue());

        // Check that dependencies are not located in the test build dir since copyLibsDirectory not set. They will be located in the gradle cache somewhere.
        nodeValue = nodes.item(1).getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
        if (OSUtil.isWindows()) {
            assert nodeValue.endsWith("\\javax.mail-1.5.0.jar") && !nodeValue.contains("\\loose-war-with-loose-jar/") : 'archive sourceOnDisk attribute value not correct'
        } else {
            assert nodeValue.endsWith("/javax.mail-1.5.0.jar") && !nodeValue.contains("/loose-war-with-loose-jar/") : 'archive sourceOnDisk attribute value not correct'
        }

        Assert.assertEquals("archive targetInArchive attribute value", "/WEB-INF/lib/activation-1.1.jar",
            nodes.item(2).getAttributes().getNamedItem("targetInArchive").getNodeValue());

        // Check that dependencies are not located in the test build dir since copyLibsDirectory not set. They will be located in the gradle cache somewhere.
        nodeValue = nodes.item(2).getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
        if (OSUtil.isWindows()) {
            assert nodeValue.endsWith("\\activation-1.1.jar") && !nodeValue.contains("\\loose-war-with-loose-jar\\") : 'archive sourceOnDisk attribute value not correct'
        } else {
            assert nodeValue.endsWith("/activation-1.1.jar") && !nodeValue.contains("/loose-war-with-loose-jar/") : 'archive sourceOnDisk attribute value not correct'
        }
    }
}
