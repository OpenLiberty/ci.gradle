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
import org.junit.Test
import groovy.util.XmlParser
import java.util.HashMap

public class TestEclipseFacetsWar {
    def projectDir = new File('.')

    @Test
    public void test_eclipse_facet_file() {
        assert new File(projectDir, '.settings/org.eclipse.wst.common.project.facet.core.xml').exists() : 'facet file not found in buildDir'
    }

    @Test
    public void test_facet_file_contents() {
        XmlParser pluginXmlParser = new XmlParser()
        Node eclipseFacets = pluginXmlParser.parse(new File(projectDir, '.settings/org.eclipse.wst.common.project.facet.core.xml'))

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
