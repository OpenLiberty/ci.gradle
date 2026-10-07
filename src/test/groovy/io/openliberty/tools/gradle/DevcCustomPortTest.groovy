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
package io.openliberty.tools.gradle;

import static org.junit.Assert.*;

import java.io.File;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * Verifies that libertyDev --container resolves variable-based httpEndpoint ports
 * from liberty.server.var instead of always using the Liberty defaults (9080/9443).
 * Regression test for GH#541.
 *
 * The project under test configures:
 *   liberty.server.var.'default.http.port'  = '9090'
 *   liberty.server.var.'default.https.port' = '9453'
 * and server.xml references those via ${default.http.port} / ${default.https.port}.
 * The container command logged by devc must show -p 9090:9090 and -p 9453:9453.
 */
class DevcCustomPortTest extends BaseDevTest {

    static final String projectName = "dev-container-custom-port"
    static File resourceDir = new File("build/resources/test/dev-test/" + projectName)
    static File testBuildDir = new File(integTestDir, "/test-dev-container-custom-port")

    @BeforeClass
    public static void setup() throws IOException, InterruptedException, FileNotFoundException {
        createDir(testBuildDir)
        createTestProject(testBuildDir, resourceDir, "build.gradle", true)

        File buildFile = new File(resourceDir, buildFilename)
        copyBuildFiles(buildFile, testBuildDir, false)

        runDevMode("--container", testBuildDir)
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        String stdout = getContents(logFile, "Dev mode std output");
        System.out.println(stdout);
        String stderr = getContents(errFile, "Dev mode std error");
        System.out.println(stderr);
        cleanUpAfterClassCheckLogFile(true);
    }

    @Test
    public void testCustomHttpPortInContainerCommand() throws Exception {
        // The container-side port must be 9090 (resolved from liberty.server.var), not the default 9080.
        // The host-side port may differ if 9090 was already in use, so check for ":9090" only.
        assertTrue("Container command should map to internal HTTP port 9090, not the default 9080.",
                verifyLogMessage(2000, ":9090", logFile))
    }

    @Test
    public void testCustomHttpsPortInContainerCommand() throws Exception {
        // The container-side port must be 9453 (resolved from liberty.server.var), not the default 9443.
        assertTrue("Container command should map to internal HTTPS port 9453, not the default 9443.",
                verifyLogMessage(2000, ":9453", logFile))
    }

    @Test
    public void testServerStarted() throws Exception {
        assertTrue("The application start message is missing.",
                verifyLogMessage(2000, "CWWKZ0001I: Application rest started", logFile))
    }
}
