/*
 * (C) Copyright IBM Corporation 2023.
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
package io.openliberty.tools.gradle;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import org.apache.commons.io.FileUtils;
import java.io.File;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class DevSkipInstallFeatureTest extends BaseDevTest {

    static final String projectName = "dev-skip-feature-install";
    static File resourceDir = new File("build/resources/test/dev-test/" + projectName);

    static File testBuildDir = new File(integTestDir, "/test-dev-skip-install-feature")

    @BeforeClass
    public static void setup() throws IOException, InterruptedException, FileNotFoundException {
        createDir(testBuildDir)
        createTestProject(testBuildDir, resourceDir, "buildInstallLiberty.gradle", true)

        runTasks(testBuildDir, 'libertyCreate', 'libertyStop')

        // now copy the build.gradle for dev mode invocation
        File buildFile = new File(resourceDir, buildFilename)
        copyBuildFiles(buildFile, testBuildDir, false)

        runDevMode("--skipInstallFeature=true", testBuildDir)
    }

    @Test
    public void restartServerTest() throws Exception {
        tagLog("##restartServerTest start");
        int runningInstallFeatureCount = countOccurrences(RUNNING_INSTALL_FEATURE, logFile);
        String RESTARTED = "The server has been restarted.";
        int restartedCount = countOccurrences(RESTARTED, logFile);
        writer.write("r\n"); // command to restart liberty
        writer.flush();

        // TODO reduce wait time once https://github.com/OpenLiberty/ci.gradle/issues/751 is resolved
        // depending on the order the tests run in, tests may be triggered before this test resulting in a 30s timeout (bug above)
        assertTrue(verifyLogMessage(123000, RESTARTED, ++restartedCount));
        // not supposed to rerun installFeature task just because of a server restart
        assertTrue("Did not find expected log messages for running installFeature task: "+runningInstallFeatureCount, verifyLogMessage(2000, RUNNING_INSTALL_FEATURE, logFile, runningInstallFeatureCount));
        tagLog("##restartServerTest end");
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        String stdout = getContents(logFile, "Dev mode std output");
        System.out.println(stdout);
        String stderr = getContents(errFile, "Dev mode std error");
        System.out.println(stderr);
        cleanUpAfterClass(true);
    }
}
