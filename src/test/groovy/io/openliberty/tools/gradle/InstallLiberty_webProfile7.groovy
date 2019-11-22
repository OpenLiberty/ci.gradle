/*
 * (C) Copyright IBM Corporation 2015, 2017.
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
class InstallLiberty_webProfile7 extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/liberty-test")
    static File buildDir = new File(integTestDir, "/InstallLiberty_webProfile7")
    static String buildFilename = "install_liberty_webProfile7.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename, true)
        try {
            runTasks(buildDir, 'installLiberty')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task installLiberty. "+ e)
        }
    }

    @Test
    public void test_installLiberty_webProfile7() {
        try {
            def file = new File(buildDir, "build/wlp/lib/features/com.ibm.websphere.appserver.webProfile-7.0.mf")
            assert file.exists() : "file not found"
            assert file.canRead() : "file cannot be read"
        } catch (Exception e) {
            throw new AssertionError ("Fail on task installLiberty. "+e)
        }
    }
}
