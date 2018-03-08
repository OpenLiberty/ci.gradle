package net.wasdev.wlp.gradle.plugins;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import groovy.util.XmlParser
import java.util.HashMap

public class TestEclipseFacetsWar extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/sample.servlet")
    static File buildDir = new File(integTestDir, "/test-eclipse-facets-war")
    static String buildFilename = "verifyTimeoutSuccessAppsTest.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        if(test_mode == "offline"){
            WLP_DIR.replace("\\","/")
        }
        createTestProject(buildDir, resourceDir, buildFilename)
        
        try {
            runTasks(buildDir, 'eclipseWtpFacet')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task eclipseWtpFacet. " + e)
        }
    }

    @Test
    public void test_eclipse_facet_file() {
        assert new File('build/testBuilds/test-eclipse-facets-war/.settings/org.eclipse.wst.common.project.facet.core.xml').exists() : 'facet file not found in buildDir'
    }

    @Test
    public void test_facet_file_contents() {
        XmlParser pluginXmlParser = new XmlParser()
        Node eclipseFacets = pluginXmlParser.parse(new File('build/testBuilds/test-eclipse-facets-war/.settings/org.eclipse.wst.common.project.facet.core.xml'))

        NodeList facets = eclipseFacets.getAt('installed')
        boolean hasJstWebFacet = false
        boolean hasJavaFacet = false

        facets.each { facet -> 
            HashMap attributes = facet.attributes()
            if (attributes.getAt('facet').equals('jst.web') && attributes.getAt('version').equals('3.0')) {
                hasJstWebFacet = true
            }
            if (attributes.getAt('facet').equals('jst.java') && attributes.getAt('version').equals('1.7')) {
                hasJavaFacet = true
            }
        }

        assert hasJstWebFacet : 'The jst.web facet was not set correctly.'
        assert hasJavaFacet   : 'The jst.java facet was not set correctly.'
    }
}
