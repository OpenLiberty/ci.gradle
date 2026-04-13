package io.openliberty.tools.gradle

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

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

public class TestMultiModuleLooseEarTransitiveDependency extends AbstractIntegrationTest {
    static File projectDir = new File(TestMultiModuleLooseEarTransitiveDependency.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getParentFile().getParentFile().getParentFile()
    static File resourceDir = new File(projectDir, "build/resources/test/multi-module-loose-ear-transitive-test")
    static File buildDir = new File(projectDir, "build/testBuilds/multi-module-loose-ear-transitive-test")
    static String buildFilename = "build.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        // Inline createTestProject logic with absolute paths
        if (!resourceDir.exists()){
            throw new AssertionError("The source file '${resourceDir.canonicalPath}' doesn't exist.", null)
        }
        try {
            // Copy all resources except .gradle files (include settings.gradle and build.gradle)
            FileUtils.copyDirectory(resourceDir, buildDir, new FileFilter() {
               public boolean accept (File pathname) {
                   return ((!pathname.getPath().endsWith(".gradle") && 
                            !pathname.getPath().endsWith(".gradle.kts")) ||
                            pathname.getPath().endsWith("settings.gradle") ||
                            pathname.getPath().endsWith("build.gradle"))
               }
            });
            // Copy build.gradle
            copyFile(new File(resourceDir, buildFilename), new File(buildDir, "build.gradle"))
            // Copy gradle.properties with absolute path
            copyFile(new File(projectDir, "build/gradle.properties"), new File(buildDir, "gradle.properties"))
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
        
        File looseXml = new File('build/testBuilds/multi-module-loose-ear-transitive-test/ear/build/wlp/usr/servers/testServer/apps/ejb-transitive-dependency-ear-1.0.ear.xml')
        assert looseXml.exists() : 'Loose application config file was not created'
    }

    @Test
    public void test_ejb_module_includes_transitive_dependencies() {
        File looseXml = new File('build/testBuilds/multi-module-loose-ear-transitive-test/ear/build/wlp/usr/servers/testServer/apps/ejb-transitive-dependency-ear-1.0.ear.xml')
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

        String expression = "/archive/archive[@targetInArchive='/ejb-transitive-dependency-ejb-jar-1.0.jar']"
        NodeList ejbModuleNodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET)
        Assert.assertEquals("Should have exactly one EJB module archive", 1, ejbModuleNodes.getLength())

        Node ejbModuleNode = ejbModuleNodes.item(0)

        expression = "dir"
        NodeList dirNodes = (NodeList) xPath.compile(expression).evaluate(ejbModuleNode, XPathConstants.NODESET)

        Assert.assertEquals("EJB module should have exactly 3 <dir> elements (own classes + lib-jar + base-jar)", 
                         3, dirNodes.getLength())

        // Verify that directories are from all three modules
        boolean foundEjbClasses = false
        boolean foundLibJarClasses = false
        boolean foundBaseJarClasses = false
        
        for (int i = 0; i < dirNodes.getLength(); i++) {
            Node dirNode = dirNodes.item(i)
            String sourceOnDisk = dirNode.getAttributes().getNamedItem("sourceOnDisk").getNodeValue()
            
            if (sourceOnDisk.contains("ejb-jar") && sourceOnDisk.contains("classes")) {
                foundEjbClasses = true
                String targetInArchive = dirNode.getAttributes().getNamedItem("targetInArchive").getNodeValue()
                Assert.assertEquals("EJB module's own classes should be at root of JAR", "/", targetInArchive)
            } else if (sourceOnDisk.contains("lib-jar") && sourceOnDisk.contains("classes")) {
                foundLibJarClasses = true
                String targetInArchive = dirNode.getAttributes().getNamedItem("targetInArchive").getNodeValue()
                Assert.assertEquals("Direct dependency classes should be at root of EJB JAR", "/", targetInArchive)
            } else if (sourceOnDisk.contains("base-jar") && sourceOnDisk.contains("classes")) {
                foundBaseJarClasses = true
                String targetInArchive = dirNode.getAttributes().getNamedItem("targetInArchive").getNodeValue()
                Assert.assertEquals("Transitive dependency classes should be at root of EJB JAR", "/", targetInArchive)
            }
        }
        
        Assert.assertTrue("EJB module should include its own classes directory", foundEjbClasses)
        Assert.assertTrue("EJB module should include lib-jar classes directory (direct dependency)", foundLibJarClasses)
        Assert.assertTrue("EJB module should include base-jar classes directory (transitive dependency)", foundBaseJarClasses)
    }
}
