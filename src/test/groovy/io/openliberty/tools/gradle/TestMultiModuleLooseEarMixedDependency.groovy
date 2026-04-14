package io.openliberty.tools.gradle

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters

import java.io.File
import java.io.FileInputStream
import java.io.IOException

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

import org.apache.commons.io.FileUtils
import org.junit.Assert
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import org.w3c.dom.Node

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestMultiModuleLooseEarMixedDependency extends AbstractIntegrationTest {
    static File resourceDir = new File("build/resources/test/multi-module-loose-ear-mixed-test")
    static File buildDir = new File(integTestDir, "/multi-module-loose-ear-mixed-test")
    static String buildFilename = "build.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        // Inline createTestProject logic
        if (!resourceDir.exists()){
            throw new AssertionError("The source file '${resourceDir.canonicalPath}' doesn't exist.", null)
        }
        try {
            // Copy all resources
            FileUtils.copyDirectory(resourceDir, buildDir)
            // Copy gradle.properties
            copyFile(new File("build/gradle.properties"), new File(buildDir, "gradle.properties"))
        } catch (IOException e) {
            throw new AssertionError("Unable to copy directory '${buildDir.canonicalPath}'.", e)
        }
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
        
        File looseXml = new File('build/testBuilds/multi-module-loose-ear-mixed-test/ear/build/wlp/usr/servers/testServer/apps/ejb-mixed-dependency-ear-1.0.ear.xml')
        assert looseXml.exists() : 'Loose application config file was not created'
    }

    @Test
    public void test_project_dependencies_included() {
        File looseXml = new File('build/testBuilds/multi-module-loose-ear-mixed-test/ear/build/wlp/usr/servers/testServer/apps/ejb-mixed-dependency-ear-1.0.ear.xml')
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

        String expression = "/archive/archive[@targetInArchive='/ejb-mixed-dependency-ejb-jar-1.0.jar']"
        NodeList ejbModuleNodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET)
        Assert.assertEquals("Should have exactly one EJB module archive", 1, ejbModuleNodes.getLength())

        Node ejbModuleNode = ejbModuleNodes.item(0)

        // Check for <dir> elements (project dependencies)
        expression = "dir"
        NodeList dirNodes = (NodeList) xPath.compile(expression).evaluate(ejbModuleNode, XPathConstants.NODESET)

        // Should have exactly 2 <dir> elements: ejb-jar's own classes + lib-jar classes
        Assert.assertEquals("EJB module should have exactly 2 <dir> elements (own classes + lib-jar project dependency)",
                         2, dirNodes.getLength())

        // Verify that one of the directories is from lib-jar module
        boolean foundLibJarClasses = false
        boolean foundEjbClasses = false
        for (int i = 0; i < dirNodes.getLength(); i++) {
            Node dirNode = dirNodes.item(i)
            String sourceOnDisk = dirNode.getAttributes().getNamedItem("sourceOnDisk").getNodeValue()
            
            if (sourceOnDisk.contains("lib-jar") && sourceOnDisk.contains("classes")) {
                foundLibJarClasses = true
            }
            if (sourceOnDisk.contains("ejb-jar") && sourceOnDisk.contains("classes")) {
                foundEjbClasses = true
            }
        }
        
        Assert.assertTrue("EJB module should include its own classes directory", foundEjbClasses)
        Assert.assertTrue("EJB module should include lib-jar classes directory (project dependency)", foundLibJarClasses)
    }
}
