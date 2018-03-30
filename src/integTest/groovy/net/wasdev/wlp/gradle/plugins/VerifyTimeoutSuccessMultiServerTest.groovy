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

public class VerifyTimeoutSuccessMultiServerTest extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/sample.servlet")
    static File buildDir = new File(integTestDir, "/verify-timeout-success-multi-server-test")
    static String buildFilename = "verifyTimeoutSuccessMultiServerTest.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        if(test_mode == "offline"){
            WLP_DIR.replace("\\","/")
        }
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runTasks(buildDir, 'libertyStop')
    }

    @Test
    public void test_multi_server_start_with_timeout_success() {
        try {
            runTasks(buildDir, 'libertyStart')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStart. "+ e)
        }
        assert new File('build/testBuilds/verify-timeout-success-multi-server-test/build/wlp/usr/servers/libertyServer1/apps/sample.servlet-1.war').exists() : 'application not installed on server1'
        assert new File('build/testBuilds/verify-timeout-success-multi-server-test/build/wlp/usr/servers/libertyServer2/apps/sample.servlet-1.war').exists() : 'application not installed on server2'
        assert new File('build/testBuilds/verify-timeout-success-multi-server-test/build/wlp/usr/servers/libertyServer3/apps/sample.servlet-1.war').exists() : 'application not installed on server3'
    }
}
