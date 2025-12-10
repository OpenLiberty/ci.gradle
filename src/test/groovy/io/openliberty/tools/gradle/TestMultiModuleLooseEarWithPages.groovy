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

import java.nio.file.Paths
import java.util.regex.Pattern

public class TestMultiModuleLooseEarWithPages extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/multi-module-loose-ear-pages-test")
    static File buildDir = new File(integTestDir, "/multi-module-loose-ear-pages-test")
    static File earDir = new File(Paths.get(buildDir.getPath(), "ear", "build").toUri());
    static String buildFilename = "build.gradle"
    public static final String libDirName = "custom/lib-dir"
    public static String copyLibsDirectory = "copy/libs"

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
            <dir sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/jar/build/resources/main" targetInArchive="/"/>
            <file sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/jar/build/resources/tmp/META-INF/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
        </archive>
        <file sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/ear/build/copy/libs/428332275272250/log4j-api-2.9.0.jar" targetInArchive="/custom/lib-dir/log4j-api-2.9.0.jar"/>
        <file sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/ear/build/copy/libs/428332276331083/log4j-core-2.9.0.jar" targetInArchive="/custom/lib-dir/log4j-core-2.9.0.jar"/>
        <file sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/ear/build/copy/libs/428332277620791/commons-lang3-3.18.0.jar" targetInArchive="/custom/lib-dir/commons-lang3-3.18.0.jar"/>
        <file sourceOnDisk="/Users/arunkumarvn/Documents/public/ci.gradle/build/testBuilds/multi-module-loose-ear-pages-test/ear/build/copy/libs/428332278367833/test.jar" targetInArchive="/custom/lib-dir/test.jar"/>
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
        String jarBuildResourcesDir="/multi-module-loose-ear-pages-test/jar/build/resources/main"
        String ejbJar = "/WEB-INF/lib/ejb-jar-1.0-SNAPSHOT.jar"
        String ejbJarArchive = "/ejb-jar-1.0-SNAPSHOT.jar"


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
        Assert.assertTrue("actual targetInArchive paths [" + expectedResult1 + ","
                + expectedResult2 + "] is not matching with actual result : " + ejbJarArchive,
                expectedResult1.equals(ejbJarArchive) || expectedResult2.equals(ejbJarArchive))


        if (OSUtil.isWindows()) {
            warWebappsFolder = "\\multi-module-loose-ear-pages-test\\war\\src\\main\\webapp"
            warBuildResourcesDir = "\\multi-module-loose-ear-pages-test\\war\\build\\resources\\main"
            jarBuildResourcesDir = "\\multi-module-loose-ear-pages-test\\jar\\build\\resources\\main"
            copyLibsDirectory = "copy\\libs"
        }
        expression = "/archive/archive/dir";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <archive/> element ==>", 5, nodes.getLength());

        String sourceOnDisk1 = nodes.item(0).getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
        String sourceOnDisk2 = nodes.item(1).getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
        String sourceOnDisk3 = nodes.item(2).getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
        String sourceOnDisk4 = nodes.item(3).getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
        String sourceOnDisk5 = nodes.item(4).getAttributes().getNamedItem("sourceOnDisk").getNodeValue();

        Assert.assertTrue("actual sourceOnDisk paths [" + sourceOnDisk1 + ","+ sourceOnDisk2 + ","
                + sourceOnDisk3 +  ","
                + sourceOnDisk4 + "] is not containing with actual result : " + warBuildResourcesDir,
                sourceOnDisk1.contains(warBuildResourcesDir) || sourceOnDisk2.contains(warBuildResourcesDir)
                        || sourceOnDisk3.contains(warBuildResourcesDir) || sourceOnDisk4.contains(warBuildResourcesDir)
                        || sourceOnDisk5.contains(warBuildResourcesDir)
        );

        Assert.assertTrue("actual sourceOnDisk paths [" + sourceOnDisk1 + ","+ sourceOnDisk2 + ","
                + sourceOnDisk3 +  ","
                + sourceOnDisk4 + "] is not containing with actual result : " + warWebappsFolder,
                sourceOnDisk1.contains(warWebappsFolder) || sourceOnDisk2.contains(warWebappsFolder)
                        || sourceOnDisk3.contains(warWebappsFolder) || sourceOnDisk4.contains(warWebappsFolder)
                        || sourceOnDisk5.contains(warWebappsFolder)
        );

        Assert.assertTrue("actual sourceOnDisk paths [" + sourceOnDisk1 + ","+ sourceOnDisk2 + ","
                + sourceOnDisk3 +  ","
                + sourceOnDisk4 + "] is not containing with actual result : " + jarBuildResourcesDir,
                sourceOnDisk1.contains(jarBuildResourcesDir) || sourceOnDisk2.contains(jarBuildResourcesDir)
                        || sourceOnDisk3.contains(jarBuildResourcesDir) || sourceOnDisk4.contains(jarBuildResourcesDir)
                        || sourceOnDisk5.contains(jarBuildResourcesDir)
        );

        expression = "/archive/archive/archive";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <archive/> element ==>", 1, nodes.getLength());

        Assert.assertEquals(ejbJar, nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue())

        expression = "/archive/file";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <file/> element ==>", 6, nodes.getLength());

        Assert.assertEquals("/" + libDirName + "/log4j-api-2.9.0.jar", nodes.item(1).getAttributes().getNamedItem("targetInArchive").getNodeValue())
        Assert.assertEquals("/" + libDirName+ "/log4j-core-2.9.0.jar", nodes.item(2).getAttributes().getNamedItem("targetInArchive").getNodeValue())
        Assert.assertEquals("/" + libDirName+ "/commons-lang3-3.18.0.jar", nodes.item(3).getAttributes().getNamedItem("targetInArchive").getNodeValue())
        Assert.assertEquals("/" + libDirName+ "/test.jar", nodes.item(4).getAttributes().getNamedItem("targetInArchive").getNodeValue())

        sourceOnDisk1 = nodes.item(1).getAttributes().getNamedItem("sourceOnDisk").getNodeValue()
        sourceOnDisk2 = nodes.item(2).getAttributes().getNamedItem("sourceOnDisk").getNodeValue()
        sourceOnDisk3 = nodes.item(3).getAttributes().getNamedItem("sourceOnDisk").getNodeValue()
        sourceOnDisk4 = nodes.item(4).getAttributes().getNamedItem("sourceOnDisk").getNodeValue()

        String patternString

        if (OSUtil.isWindows()) {
            // Replace literal '\' with '\\' in the final regex pattern
            // '\\\\' in Groovy string literal results in '\\' in the Java regex string
            String escapedPrefixWindows = copyLibsDirectory.replaceAll('\\\\', '\\\\\\\\')
            patternString = '.*' + escapedPrefixWindows + '\\\\(\\d+)\\\\[^\\\\]+\\.jar$'
        } else {
            // Unix logic: No further escaping is required for slashes.
            patternString = '.*' + copyLibsDirectory + '/(\\d+)/[^/]+\\.jar$'
        }

        Pattern dynamicRegexPattern = Pattern.compile(patternString)

        Assert.assertTrue(sourceOnDisk1 + " not contained in regex pattern " + dynamicRegexPattern,
                dynamicRegexPattern.matcher(sourceOnDisk1).matches() && sourceOnDisk1.endsWith("log4j-api-2.9.0.jar"))
        Assert.assertTrue(sourceOnDisk2 + " not contained in regex pattern " + dynamicRegexPattern,
                dynamicRegexPattern.matcher(sourceOnDisk2).matches() && sourceOnDisk2.endsWith("log4j-core-2.9.0.jar"))
        Assert.assertTrue(sourceOnDisk3 + " not contained in regex pattern " + dynamicRegexPattern,
                dynamicRegexPattern.matcher(sourceOnDisk3).matches() && sourceOnDisk3.endsWith("commons-lang3-3.18.0.jar"))
        Assert.assertTrue(sourceOnDisk4 + " not contained in regex pattern " + dynamicRegexPattern,
                dynamicRegexPattern.matcher(sourceOnDisk4).matches() && sourceOnDisk4.endsWith("test.jar"))

    }
}