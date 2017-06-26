/*
 * (C) Copyright IBM Corporation 2015, 2017
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
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runners.MethodSorters
import org.junit.FixMethodOrder
import static org.junit.Assert.*

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LibertyTest extends AbstractIntegrationTest{

    @AfterClass
    public static void tearDown() throws Exception {
        deleteDir(integTestDir)
    }

    @Test
    public void test1_start() {
        try {
            runTasks(integTestDir, 'libertyStart')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStart. "+e)
        }
    }

    @Test
    public void test2_executeDeployTask() {
        try {
            runTasks(integTestDir, 'deploy')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy. "+e)
        }
    }

    @Test
    public void test3_executeUndeployTask() {
        try {
            runTasks(integTestDir, 'undeploy')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task undeploy. "+e)
        }
    }

    @Test
    public void test4_stop() {
        try{
            runTasks(integTestDir, 'libertyStop')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStop. "+e)
        }
    }

    @Test
    public void test5_status() {
        try{
          runTasks(integTestDir, 'libertyStatus')
        } catch (Exception e) {
          throw new AssertionError ("Fail on task libertyStatus. "+e)
        }
    }

    @Test
    public void test6_package() {
        try{
           runTasks(integTestDir, 'libertyPackage')
		   
		   def file = new File("build/integTest/testDir/integTest.jar")
		   
		   assert file.exists() : "file not found"
		   assert file.canRead() : "file cannot be read"
		   
        } catch (Exception e) {
           throw new AssertionError ("Fail on task libertyPackage. "+e)
        }
    }

    @Test
    public void test7_installFeature() {
        try{
           runTasks(integTestDir, 'InstallFeature')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task InstallFeature. "+e)
        }
    }

    @Test
    public void test8_uninstallFeature() {
        try{
           runTasks(integTestDir, 'UninstallFeature')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task UninstallFeature. "+e)
        }
    }

    @Test
    public void test9_cleanDirectories() {
        try{
           runTasks(integTestDir, 'cleanDirs')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task Clean. "+e)
        }
    }
}