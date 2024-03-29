/*
 * (C) Copyright IBM Corporation 2018
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
package io.openliberty.tools.gradle

import static org.junit.Assert.*

import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class InstallFeature_acceptLicense extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/sample.servlet")
    static File buildDir = new File(integTestDir, "/InstallFeature_acceptLicense")
    static String buildFilename = "installFeatureServerXmlTest.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
        try {
            runTasks(buildDir, 'libertyCreate')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyCreate.", e)
        }
    }

    @Test
    public void test_installFeature_multiple() {
        try {
            def file = new File(buildDir, "build/wlp/lib/features/com.ibm.websphere.appserver.mongodb-2.0.mf")
            def file_2 = new File(buildDir, "build/wlp/lib/features/com.ibm.websphere.appserver.adminCenter-1.0.mf")
            runTasks(buildDir, 'installFeature')

            assert file.exists() : "com.ibm.websphere.appserver.mongodb-2.0.mf is not installed"
            assert file.canRead() : "com.ibm.websphere.appserver.mongodb-2.0.mf cannot be read"
            assert file_2.exists() : "com.ibm.websphere.appserver.adminCenter-1.0.mf is not installed"
            assert file_2.canRead() : "com.ibm.websphere.appserver.adminCenter-1.0.mf cannot be read"
        } catch (Exception e) {
            throw new AssertionError ("Fail on task installFeature.", e)
        }
    }
}
