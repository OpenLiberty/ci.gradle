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
package net.wasdev.wlp.gradle.plugins

import static org.junit.Assert.*

import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class InstallFeature_single extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/liberty-test")
    static File buildDir = new File(integTestDir, "/InstallFeature_single")
    static File buildFilename = new File(resourceDir, "install_feature_single.gradle")

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        if (test_mode == "offline"){
            WLP_DIR.replace("\\","/")
        }
        copyFile(buildFilename, new File(buildDir, 'build.gradle'))
    }
    
    @Test
    public void test_installFeature_single() {
        try {
            def file = new File(buildDir, "build/wlp/lib/features/com.ibm.websphere.appserver.mongodb-2.0.mf")
            runTasks(buildDir, 'installFeature')
            
            if (test_mode == "online") {
                assert file.exists() : "com.ibm.websphere.appserver.mongodb-2.0.mf is not installed"
                assert file.canRead() : "com.ibm.websphere.appserver.mongodb-2.0.mf cannot be read"
            } 
        } catch (Exception e) {
            throw new AssertionError ("Fail on task installFeature. "+e)
        }
    }

    @Test
    public void test_uninstallFeature_single() {
        try {
            def file = new File(buildDir, "build/wlp/lib/features/com.ibm.websphere.appserver.mongodb-2.0.mf")
            runTasks(buildDir, 'uninstallFeature')
            
            if (test_mode == "online") {
                assert !file.exists() : "com.ibm.websphere.appserver.mongodb-2.0.mf is not uninstalled"
            }
        } catch (Exception e) {
            throw new AssertionError ("Fail on task uninstallFeature. "+e)
        }
    }
}

