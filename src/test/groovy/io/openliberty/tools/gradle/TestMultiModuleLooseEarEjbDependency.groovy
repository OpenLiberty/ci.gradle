package io.openliberty.tools.gradle

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import java.io.File
import java.io.FileInputStream

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

import org.junit.Assert
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import org.w3c.dom.Node

public class TestMultiModuleLooseEarEjbDependency extends AbstractIntegrationTest {
    static File resourceDir = new File("build/resources/test/multi-module-loose-ear-ejb-dependency-test")
    static File buildDir = new File(integTestDir, "/multi-module-loose-ear-ejb-dependency-test")
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
            throw new AssertionError("Fail on task deploy.", e)
        }
        
        File looseXml = new File('build/testBuilds/multi-module-loose-ear-ejb-dependency-test/ear/build/wlp/usr/servers/testServer/apps/ejb-dependency-ear-1.0.ear.xml')
        assert looseXml.exists() : 'Loose application config file was not created'
    }

    @Test
    public void test_ejb_module_includes_dependency_classes() {
        File looseXml = new File('build/testBuilds/multi-module-loose-ear-ejb-dependency-test/ear/build/wlp/usr/servers/testServer/apps/ejb-dependency-ear-1.0.ear.xml')
        FileInputStream input = new FileInputStream(looseXml)

        DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance()
        inputBuilderFactory.setIgnoringComments(true)
        inputBuilderFactory.setCoalescing(true)
        inputBuilderFactory.setIgnoringElementContentWhitespace(true)
        inputBuilderFactory.setValidating(false)
        inputBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
        inputBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder()
        Document inputDoc = inputBuilder.parse(input)

        XPath xPath = XPathFactory.newInstance().newXPath()

        String expression = "/archive/archive[@targetInArchive='/ejb-dependency-ejb-jar-1.0.jar']"
        NodeList ejbModuleNodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET)
        Assert.assertEquals("Should have exactly one EJB module archive", 1, ejbModuleNodes.getLength())

        Node ejbModuleNode = ejbModuleNodes.item(0)

        expression = "dir"
        NodeList dirNodes = (NodeList) xPath.compile(expression).evaluate(ejbModuleNode, XPathConstants.NODESET)

        Assert.assertEquals("EJB module should have exactly 2 <dir> elements (own classes + dependency classes)",
                         2, dirNodes.getLength())

        // Verify that one of the directories is from lib-jar module
        boolean foundLibJarClasses = false
        for (int i = 0; i < dirNodes.getLength(); i++) {
            Node dirNode = dirNodes.item(i)
            String sourceOnDisk = dirNode.getAttributes().getNamedItem("sourceOnDisk").getNodeValue()
            
            if (sourceOnDisk.contains("lib-jar") && sourceOnDisk.contains("classes")) {
                foundLibJarClasses = true
                
                // Verify targetInArchive is "/"
                String targetInArchive = dirNode.getAttributes().getNamedItem("targetInArchive").getNodeValue()
                Assert.assertEquals("Dependency classes should be at root of EJB JAR", "/", targetInArchive)
                break
            }
        }
        
        Assert.assertTrue("EJB module should include lib-jar classes directory", foundLibJarClasses)
    }

    @Test
    public void test_ejb_module_own_classes_included() {
        File looseXml = new File('build/testBuilds/multi-module-loose-ear-ejb-dependency-test/ear/build/wlp/usr/servers/testServer/apps/ejb-dependency-ear-1.0.ear.xml')
        FileInputStream input = new FileInputStream(looseXml)

        DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance()
        inputBuilderFactory.setIgnoringComments(true)
        inputBuilderFactory.setCoalescing(true)
        inputBuilderFactory.setIgnoringElementContentWhitespace(true)
        inputBuilderFactory.setValidating(false)
        inputBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
        inputBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder()
        Document inputDoc = inputBuilder.parse(input)

        XPath xPath = XPathFactory.newInstance().newXPath()

        String expression = "/archive/archive[@targetInArchive='/ejb-dependency-ejb-jar-1.0.jar']/dir[contains(@sourceOnDisk, 'ejb-jar') and contains(@sourceOnDisk, 'classes')]"
        NodeList ejbModuleOwnClasses = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET)
        
        Assert.assertTrue("EJB module should include its own classes directory", ejbModuleOwnClasses.getLength() > 0)

        // Verify it targets root of JAR
        Node dirNode = ejbModuleOwnClasses.item(0)
        String targetInArchive = dirNode.getAttributes().getNamedItem("targetInArchive").getNodeValue()
        Assert.assertEquals("EJB module's own classes should be at root of JAR", "/", targetInArchive)
    }
}
