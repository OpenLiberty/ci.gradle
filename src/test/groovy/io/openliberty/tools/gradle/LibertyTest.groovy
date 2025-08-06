/*
 * (C) Copyright IBM Corporation 2015, 2023.
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
import io.openliberty.tools.ant.ServerTask
import java.util.concurrent.*

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LibertyTest extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/liberty-test")
    static File buildDir = new File(integTestDir, "/liberty-test")
    static String buildFilename = "build.gradle"
    static File serverXmlFile = new File(buildDir, "/build/wlp/usr/servers/LibertyProjectServer/server.xml")

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
        try {
            runTasks(buildDir, 'installLiberty')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task installLiberty.", e)
        }
    }
        
    @Test
    public void test0_run() {
        final int timeout = 120000     // 120 sec, but polling will break out earlier typically
        final String START_SERVER_MESSAGE_REGEXP = "CWWKF0011I.*"

        ServerTask st = new ServerTask()
        def installDir = new File(buildDir.getAbsolutePath() + "/build/wlp")
        st.setInstallDir(installDir)
        st.setServerName('LibertyProjectServer')
        st.initTask()

        try{
            def stop_thread = Thread.start {
                String verify = st.waitForStringInLog(START_SERVER_MESSAGE_REGEXP, timeout, st.getLogFile())
                try {
                    if (verify) {
                        runTasks(buildDir, 'libertyStop')
                    } else {
                        throw new AssertionError ("Fail to start server for libertyRun.", null)
                    }
                } catch (Exception e) {
                    throw new AssertionError ("Fail on task libertyStop for libertyRun.", e)
                }
            }
            runTasks(buildDir, 'libertyRun')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyRun.", e)
        }
    }

    @Test
    public void test1_start() {
        try {
            runTasks(buildDir, 'libertyStart')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStart.", e)
        }
    }

    @Test
    public void test2_executeDeployTask() {
        try {
            runTasks(buildDir, 'deploy')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy.", e)
        }
    }

    @Test
    public void test3_executeUndeployTask() {
        try {
            runTasks(buildDir, 'undeploy')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task undeploy.", e)
        }
    }

    @Test
    public void test4_stop() {
        try{
            runTasks(buildDir, 'libertyStop')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStop.", e)
        }
    }

    @Test
    public void test5_status() {
        try{
          runTasks(buildDir, 'libertyStatus')
        } catch (Exception e) {
          throw new AssertionError ("Fail on task libertyStatus.", e)
        }
    }

    @Test
    public void test6_package() {
        try{
           runTasks(buildDir, 'libertyPackage')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task libertyPackage.", e)
        }
    }

    @Test
    public void test7_installFeature() {
        try{
           runTasks(buildDir, 'InstallFeature')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task InstallFeature.", e)
        }
    }

    @Test
    public void test8_uninstallFeature() {
        try{
           runTasks(buildDir, 'UninstallFeature')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task UninstallFeature.", e)
        }
    }

    @Test
    public void test9_cleanDirectories() {
        try{
           runTasks(buildDir, 'cleanDirs')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task cleanDirs.", e)
        }

        try{
           runTasks(buildDir, 'libertyStart')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task libertyStart after cleanDirs.", e)
        }

        // First test: Try to run clean while the server is running
        // This tests the original scenario
        try{
            // Add timeout mechanism to prevent test from hanging
            def timeout = 60000 // 60 seconds timeout
            def future = Executors.newSingleThreadExecutor().submit({
                runTasks(buildDir, 'clean')
                return true
            })
            
            try {
                future.get(timeout, TimeUnit.MILLISECONDS)
            } catch (TimeoutException e) {
                future.cancel(true)
                throw new AssertionError("Task 'clean' timed out after ${timeout/1000} seconds", e)
            }
        } catch (Exception e) {
            e.printStackTrace()
            throw new AssertionError ("Fail on task clean while Liberty server is running.", e)
        }

        // Second test: Stop the server and then run clean
        // This tests the more reliable approach with explicit server stop
        try{
           // Stop the server before cleaning to ensure all resources are released
           runTasks(buildDir, 'libertyStop')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task libertyStop before clean.", e)
        }

        // Add a small delay to ensure file locks are fully released
        try {
            Thread.sleep(2000)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt()
        }

        try{
            // Add timeout mechanism to prevent test from hanging
            def timeout = 60000 // 60 seconds timeout
            def future = Executors.newSingleThreadExecutor().submit({
                runTasks(buildDir, 'clean')
                return true
            })
            
            try {
                future.get(timeout, TimeUnit.MILLISECONDS)
            } catch (TimeoutException e) {
                future.cancel(true)
                throw new AssertionError("Task 'clean' timed out after ${timeout/1000} seconds", e)
            }
        } catch (Exception e) {
           throw new AssertionError ("Fail on task clean after server stop.", e)
        }

        try{
           runTasks(buildDir, 'cleanDirs')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task cleanDirs after clean.", e)
        }

        try{
           runTasks(buildDir, 'libertyStart')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task libertyStart after second clean.", e)
        }

        // try deleting the server.xml and see if we can recover
        assert serverXmlFile.exists() : 'server.xml file does not exist in LibertyProjectServer'
        assert serverXmlFile.delete() : 'server.xml could not be deleted in LibertyProjectServer'

        try{
           runTasks(buildDir, 'libertyStop')
        } catch (Exception e) {
           throw new AssertionError ("Fail on task libertyStop after deleting server.xml.", e)
        }

        assert !serverXmlFile.exists() : 'server.xml file unexpectedly exists in LibertyProjectServer after libertyStop'

        try{
            runTasks(buildDir, 'libertyStatus')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStatus after deleting server.xml.", e)
        }

        assert serverXmlFile.exists() : 'server.xml file does not exist in LibertyProjectServer after libertyStatus'

        try{
            // Add timeout mechanism to prevent test from hanging
            def timeout = 60000 // 60 seconds timeout
            def future = Executors.newSingleThreadExecutor().submit({
                runTasks(buildDir, 'clean')
                return true
            })
            
            try {
                future.get(timeout, TimeUnit.MILLISECONDS)
            } catch (TimeoutException e) {
                future.cancel(true)
                throw new AssertionError("Task 'clean' timed out after ${timeout/1000} seconds", e)
            }
        } catch (Exception e) {
            throw new AssertionError ("Fail on task clean after deleting server.xml.", e)
        }
    }
}
