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
package net.wasdev.wlp.gradle.plugins

import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runners.MethodSorters
import org.junit.FixMethodOrder
import org.apache.commons.io.FileUtils;
import static org.junit.Assert.*

import java.io.File

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class InstallLIbertyOutputUpToDateTest extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/liberty-test")
    static File buildDir = new File(integTestDir, "/install-liberty-output-upToDateTest")
    static File buildFilename = new File(resourceDir, "install_liberty_upToDate.gradle")

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        if (test_mode == "offline"){
            WLP_DIR.replace("\\","/")
            copyBuildFiles(buildFilename, buildDir)
        } else if (test_mode == "online"){
            copyBuildFiles(buildFilename, buildDir)
            try {
                runTasks(buildDir, 'installLiberty')
            } catch (Exception e) {
                throw new AssertionError ("Fail on task installLiberty. "+ e)
            }
        }
    }

    @Test
    public void test_installLiberty() {
        try{
           File file = new File("build/wlp/lib/versions/WebSphereApplicationServer.properties");
            
           buildFilename = new File(resourceDir, "install_liberty_upToDate2.gradle")
           copyBuildFiles(buildFilename, buildDir)
           runTasks(buildDir, 'installLiberty')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task installLiberty. " + e)
        }
    }
}
