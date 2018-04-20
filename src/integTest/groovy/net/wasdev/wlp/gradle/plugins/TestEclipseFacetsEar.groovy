/*
 * (C) Copyright IBM Corporation 2018.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
