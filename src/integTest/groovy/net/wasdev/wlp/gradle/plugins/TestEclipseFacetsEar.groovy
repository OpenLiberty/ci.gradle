package net.wasdev.wlp.gradle.plugins;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import groovy.util.XmlParser
import java.util.HashMap

public class TestEclipseFacetsEar extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/loose-ear-test")
    static File buildDir = new File(integTestDir, "/test-eclipse-facets-ear")
    static String buildFilename = "build.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        if(test_mode == "offline"){
            WLP_DIR.replace("\\","/")
        }
        createTestProject(buildDir, resourceDir, buildFilename)

        try {
            runTasks(buildDir, ':ejb-ear:eclipseWtpFacet')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task eclipseWtpFacet. " + e)
        }
    }

    @Test
    public void test_eclipse_facet_file() {
        assert new File('build/testBuilds/test-eclipse-facets-ear/ejb-ear/.settings/org.eclipse.wst.common.project.facet.core.xml').exists() : 'facet file not found in projectDir'
    }

    @Test
    public void test_facet_file_contents() {
        XmlParser pluginXmlParser = new XmlParser()
        Node eclipseFacets = pluginXmlParser.parse(new File('build/testBuilds/test-eclipse-facets-ear/ejb-ear/.settings/org.eclipse.wst.common.project.facet.core.xml'))

        NodeList facets = eclipseFacets.getAt('installed')
        boolean hasJstEarFacet = false

        facets.each { facet -> 
            HashMap attributes = facet.attributes()
            if (attributes.getAt('facet').equals('jst.ear') && attributes.getAt('version').equals('6.0')) {
                hasJstEarFacet = true
            }
        }

        assert hasJstEarFacet : 'The jst.ear facet was not set correctly.'
    }
}
