package io.openliberty.tools.gradle

import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.NodeList

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import io.openliberty.tools.common.plugins.util.OSUtil

public class TestMultiModuleLooseEarWithPages extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/multi-module-loose-ear-pages-test")
    static File buildDir = new File(integTestDir, "/multi-module-loose-ear-pages-test")
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
            throw new AssertionError ("Fail on task deploy.", e)
        }
        assert new File('build/testBuilds/multi-module-loose-ear-pages-test/ear/build/wlp/usr/servers/ejbServer/apps/ejb-ear-1.0-SNAPSHOT.ear.xml').exists() : 'looseApplication config file was not copied over to the liberty runtime'
    }
    /*
        Expected output to the XML
                <?xml version="1.0" encoding="UTF-8"?>
                <archive>
                    <file sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/ear/build/tmp/ear/application.xml" targetInArchive="/META-INF/application.xml"/>
                    <archive targetInArchive="/ejb-war-1.0-SNAPSHOT.war">
                        <dir sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/war/src/main/webapp" targetInArchive="/"/>
                        <dir sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/war/build/classes/java/main" targetInArchive="/WEB-INF/classes"/>
                        <dir sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/war/build/resources/main" targetInArchive="/WEB-INF/classes"/>
                        <file sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/war/build/resources/tmp/META-INF/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
                        <archive targetInArchive="/WEB-INF/lib/ejb-jar-1.0-SNAPSHOT.jar">
                            <dir sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/jar/build/classes/java/main" targetInArchive="/"/>
                            <file sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/jar/build/resources/tmp/META-INF/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
                        </archive>
                    </archive>
                    <dir sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/war/build/resources/main" targetInArchive="/WEB-INF/classes/"/>
                    <archive targetInArchive="/ejb-jar-1.0-SNAPSHOT.jar">
                        <dir sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/jar/build/classes/java/main" targetInArchive="/"/>
                        <file sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/jar/build/resources/tmp/META-INF/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
                    </archive>
                    <file sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/tmp/test/work/.gradle-test-kit/caches/modules-2/files-2.1/org.apache.logging.log4j/log4j-api/2.9.0/e0dcd508dfc4864a2f5a1963d6ffad170d970375/log4j-api-2.9.0.jar" targetInArchive="/lib/log4j-api-2.9.0.jar"/>
                    <file sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/tmp/test/work/.gradle-test-kit/caches/modules-2/files-2.1/org.apache.logging.log4j/log4j-core/2.9.0/52f6548ae1688e126c29b5dc400929dc0128615/log4j-core-2.9.0.jar" targetInArchive="/lib/log4j-core-2.9.0.jar"/>
                    <file sourceOnDisk="/Users/arunkumarvn/.m2/repository/org/apache/commons/commons-lang3/3.18.0/commons-lang3-3.18.0.jar" targetInArchive="/lib/commons-lang3-3.18.0.jar"/>
                    <file sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/ear/lib/test.jar" targetInArchive="/lib/test.jar"/>
                    <file sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/ear/build/tmp/META-INF/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
                </archive>
    */
    @Test
    public void test_loose_config_file_contents_are_correct(){
        File on = new File("build/testBuilds/multi-module-loose-ear-pages-test/ear/build/wlp/usr/servers/ejbServer/apps/ejb-ear-1.0-SNAPSHOT.ear.xml");
        FileInputStream input = new FileInputStream(on);
        String ejbWar = "/ejb-war-1.0-SNAPSHOT.war"
        String warWebappsFolder = "/multi-module-loose-ear-pages-test/war/src/main/webapp"
        String warBuildResourcesDir="/multi-module-loose-ear-pages-test/war/build/resources/main"
        String ejbJar = "/WEB-INF/lib/ejb-jar-1.0-SNAPSHOT.jar"


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
        String expression = "/archive/dir";
        NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <dir/> element ==>", 1, nodes.getLength());

        expression = "/archive/archive";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <archive/> element ==>", 2, nodes.getLength());
        String expectedResult1 = nodes.item(0).getAttributes()
                .getNamedItem("targetInArchive").getNodeValue();
        String expectedResult2 = nodes.item(1).getAttributes()
                .getNamedItem("targetInArchive").getNodeValue();
        Assert.assertTrue("actual targetInArchive paths [" + expectedResult1 + ","
                + expectedResult2 + "] is not matching with actual result : " + ejbWar,
                expectedResult1.equals(ejbWar) || expectedResult2.equals(ejbWar))

        if (OSUtil.isWindows()) {
            warWebappsFolder = "\\multi-module-loose-ear-pages-test\\war\\src\\main\\webapp"
            warBuildResourcesDir="\\multi-module-loose-ear-pages-test\\war\\build\\resources\\main"
        }
        expression = "/archive/archive/dir";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <archive/> element ==>", 4, nodes.getLength());

        String sourceOnDisk1 = nodes.item(0).getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
        String sourceOnDisk2 = nodes.item(1).getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
        String sourceOnDisk3 = nodes.item(2).getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
        String sourceOnDisk4 = nodes.item(3).getAttributes().getNamedItem("sourceOnDisk").getNodeValue();

        Assert.assertTrue("actual sourceOnDisk paths [" + sourceOnDisk1 + ","+ sourceOnDisk2 + ","
                + sourceOnDisk3 +  ","
                + sourceOnDisk4 + "] is not containing with actual result : " + warBuildResourcesDir,
                sourceOnDisk1.contains(warBuildResourcesDir) || sourceOnDisk2.contains(warBuildResourcesDir)|| sourceOnDisk3.contains(warBuildResourcesDir) || sourceOnDisk4.contains(warBuildResourcesDir));

        Assert.assertTrue("actual sourceOnDisk paths [" + sourceOnDisk1 + ","+ sourceOnDisk2 + ","
                + sourceOnDisk3 +  ","
                + sourceOnDisk4 + "] is not containing with actual result : " + warWebappsFolder,
                sourceOnDisk1.contains(warWebappsFolder) || sourceOnDisk2.contains(warWebappsFolder)|| sourceOnDisk3.contains(warWebappsFolder) || sourceOnDisk4.contains(warWebappsFolder));

        expression = "/archive/archive/archive";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <archive/> element ==>", 1, nodes.getLength());

        Assert.assertEquals(ejbJar, nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue())

        expression = "/archive/file";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <file/> element ==>", 6, nodes.getLength());

        Assert.assertEquals("/lib/log4j-api-2.9.0.jar", nodes.item(1).getAttributes().getNamedItem("targetInArchive").getNodeValue())
        Assert.assertEquals("/lib/log4j-core-2.9.0.jar", nodes.item(2).getAttributes().getNamedItem("targetInArchive").getNodeValue())
        Assert.assertEquals("/lib/commons-lang3-3.18.0.jar", nodes.item(3).getAttributes().getNamedItem("targetInArchive").getNodeValue())
        Assert.assertEquals("/lib/test.jar", nodes.item(4).getAttributes().getNamedItem("targetInArchive").getNodeValue())

    }
}
