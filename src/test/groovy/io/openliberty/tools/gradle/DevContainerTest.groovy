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

class DevContainerTest extends BaseDevTest {

    static final String projectName = "dev-container";
    static File resourceDir = new File("build/resources/test/dev-test/" + projectName);
    static File testBuildDir = new File(integTestDir, "/test-dev-container")

    @BeforeClass
    public static void setup() throws IOException, InterruptedException, FileNotFoundException {
        createDir(testBuildDir)
        createTestProject(testBuildDir, resourceDir, "build.gradle", true)

        File buildFile = new File(resourceDir, buildFilename)
        copyBuildFiles(buildFile, testBuildDir, false)

        runDevMode("--container", testBuildDir)
    }

    @Test
    public void devmodeContainerTest() throws Exception {
        assertTrue("The container build did not complete.", verifyLogMessage(20000, "Completed building container image.", logFile));
        assertTrue("The application start message is missing.", verifyLogMessage(20000, "CWWKZ0001I: Application rest started", logFile));
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        String stdout = getContents(logFile, "Dev mode std output");
        System.out.println(stdout);
        String stderr = getContents(errFile, "Dev mode std error");
        System.out.println(stderr);
        cleanUpAfterClassCheckLogFile(true);
    }
}
