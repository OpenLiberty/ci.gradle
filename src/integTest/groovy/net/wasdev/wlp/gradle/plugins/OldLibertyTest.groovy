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
import net.wasdev.wlp.ant.ServerTask

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class OldLibertyTest extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/liberty-test")
    static File buildDir = new File(integTestDir, "/old-liberty-test")
    static String buildFilename = "oldStyleTest.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        if(test_mode == "offline"){
            WLP_DIR.replace("\\","/")
            createTestProject(buildDir, resourceDir, buildFilename)
        }else if(test_mode == "online"){
            createTestProject(buildDir, resourceDir, buildFilename)
            try {
                runTasks(buildDir, 'installLiberty')
            } catch (Exception e) {
                throw new AssertionError ("Fail on task installLiberty. "+ e)
            }
        }
    }

    @Test
    public void test1_start() {
        try {
            runTasks(buildDir, 'libertyStart')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStart. "+e)
        }
    }

    @Test
    public void test2_executeDeployTask() {
        try {
            runTasks(buildDir, 'deploy')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy. "+e)
        }
    }

    @Test
    public void test3_executeUndeployTask() {
        try {
            runTasks(buildDir, 'undeploy')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task undeploy. "+e)
        }
    }

    @Test
    public void test4_stop() {
        try{
            runTasks(buildDir, 'libertyStop')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStop. "+e)
        }
    }

    @Test
    public void test5_status() {
        try{
          runTasks(buildDir, 'libertyStatus')
        } catch (Exception e) {
          throw new AssertionError ("Fail on task libertyStatus. "+e)
        }
    }

    @Test
    public void test6_package() {
        try{
           runTasks(buildDir, 'libertyPackage')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task libertyPackage. "+e)
        }
    }

    @Test
    public void test7_installFeature() {
        try{
           runTasks(buildDir, 'InstallFeature')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task InstallFeature. "+e)
        }
    }

    @Test
    public void test8_uninstallFeature() {
        try{
           runTasks(buildDir, 'UninstallFeature')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task UninstallFeature. "+e)
        }
    }

    @Test
    public void test9_cleanDirectories() {
        try{
           runTasks(buildDir, 'cleanDirs')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task Clean. "+e)
        }
    }

    @Test
    public void test10_run() {
        final int timeout = 30000     // 30 sec
        final String START_SERVER_MESSAGE_REGEXP = "CWWKF0011I.*"

        ServerTask st = new ServerTask()
        def installDir = new File(buildDir.getAbsolutePath() + "/build/wlp")
        st.setInstallDir(installDir)
        st.setServerName((test_mode == "offline") ? "libertyOffline" : "libertyOnline")
        st.initTask()

        try{
            def stop_thread = Thread.start {
                String verify = st.waitForStringInLog(START_SERVER_MESSAGE_REGEXP, timeout, st.getLogFile())
                try {
                    if (verify) {
                        runTasks(buildDir, 'libertyStop')
                    } else {
                        throw new AssertionError ("Fail to start server for libertyRun.")
                    }
                } catch (Exception e) {
                    throw new AssertionError ("Fail on task libertyStop for libertyRun. "+e)
                }
            }
            runTasks(buildDir, 'libertyRun')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyRun. "+e)
        }
    }

}
