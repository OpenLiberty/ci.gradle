/*
 * (C) Copyright IBM Corporation 2026.
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

import io.openliberty.tools.ant.ServerTask
import org.gradle.testkit.runner.BuildResult
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

import static org.junit.Assert.assertTrue

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LibertyToolchainJdk8FailTest extends AbstractIntegrationTest {
    static File resourceDir = new File("build/resources/test/basic-toolchain-project-fail-on-java8")
    static File buildDir = new File(integTestDir, "/basic-toolchain-project-fail-on-java8")
    static String buildFilename = "build.gradle"
    static File serverXmlFile = new File(buildDir, "/build/wlp/usr/servers/LibertyProjectServer/server.xml")
    static File messageLog = new File(buildDir, "/build/wlp/usr/servers/LibertyProjectServer/logs/messages.log")
    public static final String TOOLCHAIN_USED = 'CWWKM4100I: Using toolchain from build context. JDK Version specified is %s'
    public static final String TOOLCHAIN_CONFIGURED = 'CWWKM4101I: The :%s task is using the configured toolchain JDK'

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
        addToolchainJdkDownloadPluginToSettings(new File(buildDir, "settings.gradle"))
        try {
            runTasks(buildDir, 'installLiberty')
        } catch (Exception e) {
            throw new AssertionError("Fail on task installLiberty.", e)
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

        try {
            def stop_thread = Thread.start {
                String verify = st.waitForStringInLog(START_SERVER_MESSAGE_REGEXP, timeout, st.getLogFile())
                try {
                    if (verify) {
                        runTasks(buildDir, 'libertyStop')
                    } else {
                        throw new AssertionError("Fail to start server for libertyRun.", null)
                    }
                } catch (Exception e) {
                    throw new AssertionError("Fail on task libertyStop for libertyRun.", e)
                }
            }
            BuildResult result = runTasksFailResult(buildDir, 'libertyRun')
            // here compileJava will fail and libertyRun task will be stopped in between. Hence only libertyCreate task will have toolchain logs
            assertToolchainLogsForTask(result, "libertyCreate", "8", null)
            assertTrue("expected bad class error is not found in build result", result.getOutput().contains(" bad class file: "))
        } catch (Exception e) {
            throw new AssertionError("Fail on task libertyRun.", e)
        }
    }
}
