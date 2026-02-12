package io.openliberty.tools.gradle;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCompileJSPSource17 extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/sampleJSP.servlet")
    static File buildDir = new File(integTestDir, "/test-compile-jsp-source-17")
    static String buildFilename = "testCompileJSP17.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
        runTasks(buildDir, 'installFeature')
        runTasks(buildDir, 'compileJsp')
    }
    @Test
    public void check_for_jsp() {
        assert new File('build/testBuilds/test-compile-jsp-source-17/src/main/webapp/index.jsp').exists() : 'index.jsp not found!'
    }

    @Test
    public void test_1() {
        
        assert new File('build/testBuilds/test-compile-jsp-source-17/build/compileJsp').exists() : 'compileJsp Directory not found!'
    }

    @Test
    public void test_2() {
        assert new File('build/testBuilds/test-compile-jsp-source-17/build/classes/java/_index.class').exists() : '_index.class not found!'
    }

    @Test
    public void check_jsp_server_xml_exists() {
        assert new File('build/testBuilds/test-compile-jsp-source-17/build/compileJsp/servers/defaultServer/server.xml').exists() : 'server.xml not found!'
    }

    @Test
    public void check_jsp_server_xml_contains_features() {
        File serverXml = new File("build/testBuilds/test-compile-jsp-source-17/build/compileJsp/servers/defaultServer/server.xml")
        FileInputStream input = new FileInputStream(serverXml)
        
        // get input XML Document 
        DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance()
        inputBuilderFactory.setIgnoringComments(true)
        inputBuilderFactory.setCoalescing(true)
        inputBuilderFactory.setIgnoringElementContentWhitespace(true)
        inputBuilderFactory.setValidating(false)
        inputBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
        inputBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)  
        DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder()
        Document inputDoc=inputBuilder.parse(input)
        
        // parse input XML Document
        XPath xPath = XPathFactory.newInstance().newXPath()
        String expression = "/server/featureManager/feature[text()]"     
        NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET)
        Assert.assertEquals("Number of <feature/> elements ==>", 2, nodes.getLength())
        
        ArrayList<String> features = new ArrayList<String>()

        for(int i = 0; i < nodes.getLength(); i++) {
            features.add(nodes.item(i).getTextContent().trim())
        }

        Assert.assertTrue("servlet-3.1 <feature/> found ==>", features.contains("servlet-3.1"))
        Assert.assertTrue("jsp-2.3 <feature/> found ==>", features.contains("jsp-2.3"))

        // parse input XML Document
        String expression2 = "/server/jspEngine";       
        nodes = (NodeList) xPath.compile(expression2).evaluate(inputDoc, XPathConstants.NODESET)
        Assert.assertEquals("Number of <jspEngine/> elements ==>", 1, nodes.getLength())

        if (nodes.item(0) instanceof Element) {
            Element child = (Element) nodes.item(0)
            String nodeValue = child.getAttribute("javaSourceLevel")
            Assert.assertTrue("Unexpected javaSourceLevel ==>"+nodeValue, nodeValue.equals("17"))
        }
    }

    @Test
    public void check_jsp_messages_log_exists() {
        assert new File('build/testBuilds/test-compile-jsp-source-17/build/compileJsp/servers/defaultServer/logs/messages.log').exists() : 'messages.log not found!'
    }

    @Test
    public void check_jsp_messages_log_has_java_toolchain_version() {
        verifyFileContents(0,
                "java.version = 17",
                new File('build/testBuilds/test-compile-jsp-source-17/build/compileJsp/servers/defaultServer/logs/messages.log'))
    }
}
