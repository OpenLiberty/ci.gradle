/*
 * (C) Copyright IBM Corporation 2025.
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

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import static org.junit.Assert.assertTrue;

class DevContainerTestWithLooseAppFalse extends BaseDevTest {

    static final String projectName = "dev-container-loose-app-false";
    static File resourceDir = new File("build/resources/test/dev-test/" + projectName);
    static File testBuildDir = new File(integTestDir, "/test-dev-container-loose-app-false")

    @BeforeClass
    public static void setup() throws IOException, InterruptedException, FileNotFoundException {
        createDir(testBuildDir)
        createTestProject(testBuildDir, resourceDir, "build.gradle", true)

        File buildFile = new File(resourceDir, buildFilename)
        copyBuildFiles(buildFile, testBuildDir, false)

        runDevMode("--container --stacktrace", testBuildDir)
    }

    @Test
    public void devmodeContainerTest() throws Exception {
        assertTrue("The container build did not complete.", verifyLogMessage(20000, "Completed building container image.", logFile));
        assertTrue("The application start message is missing.", verifyLogMessage(20000, "CWWKZ0001I: Application rest started", logFile));
    }

    @Test
    public void modifyJavaFileTest() throws Exception {
        // modify a java file
        File srcHelloWorld = new File(buildDir, "src/main/java/com/demo/rest/RestApplication.java");
        assertTrue(srcHelloWorld.exists());

        waitLongEnough();
        String str = "// testing";
        BufferedWriter javaWriter = new BufferedWriter(new FileWriter(srcHelloWorld, true));
        javaWriter.append(' ');
        javaWriter.append(str);
        javaWriter.close();
        assertTrue(verifyLogMessage(10000, COMPILATION_SUCCESSFUL));
        assertTrue(verifyLogMessage(10000, "Recompile test-"+projectName));
        assertTrue("The application update message is missing.", verifyLogMessage(20000, "CWWKZ0003I: The application rest updated", logFile));
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
