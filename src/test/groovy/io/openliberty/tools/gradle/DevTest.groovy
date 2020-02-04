/*
 * (C) Copyright IBM Corporation 2020.
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

class DevTest extends AbstractIntegrationTest {
    static final String projectName = "basic-dev-project";

    static File resourceDir = new File("build/resources/test/dev-test/" + projectName);
    static File buildDir = new File(integTestDir, "dev-test/" + projectName + System.currentTimeMillis()); // append timestamp in case previous build was not deleted
    static String buildFilename = "build.gradle";

    static File targetDir;
    static BufferedWriter writer;
    static File logFile = new File(buildDir, "output.log");
    static Process process;

    @BeforeClass
    public static void setup() throws IOException, InterruptedException, FileNotFoundException {
        createDir(buildDir);
        createTestProject(buildDir, resourceDir, buildFilename);
        runDevMode();
    }
    
    private static void runDevMode() throws IOException, InterruptedException, FileNotFoundException {
        System.out.println("Starting dev mode...");
        startProcess(null, true);
        System.out.println("Started dev mode");
    }

    private static ProcessBuilder buildProcess(String processCommand) {
        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(buildDir);

        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().startsWith("windows")) {
            builder.command("CMD", "/C", processCommand);
        } else {
            builder.command("bash", "-c", processCommand);
        }
        return builder;
    }

    protected static boolean checkLogMessage(int timeout, String message)
            throws InterruptedException, FileNotFoundException {
        checkLogMessage(timeout, message, logFile)
    }

    protected static boolean checkLogMessage(int timeout, String message, File file)
            throws InterruptedException, FileNotFoundException {
        int waited = 0;
        boolean startFlag = false;
        while (!startFlag && waited <= timeout) {
            int sleep = 10;
            Thread.sleep(sleep);
            waited += sleep;
            if (readFile(message, file)) {
                startFlag = true;
                Thread.sleep(1000);
            }
        }
        return (waited > timeout);
    }

    private static boolean readFile(String str, File file) throws FileNotFoundException {
        Scanner scanner = new Scanner(file);
        try {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains(str)) {
                    return true;
                }
            }
        } finally {
            scanner.close();
        }
        return false;
    }

    private static void startProcess(String params, boolean isDevMode) throws IOException, InterruptedException, FileNotFoundException {
        // get gradle wrapper from project root dir
        File gradlew;
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().startsWith("windows")) {
            gradlew = new File("gradlew.bat")
        } else {
            gradlew = new File("gradlew")
        }
        
        StringBuilder command = new StringBuilder(gradlew.getAbsolutePath() + " libertyDev");
        if (params != null) {
            command.append(" " + params);
        }
        System.out.println("Running command: " + command.toString());
        ProcessBuilder builder = buildProcess(command.toString());

        builder.redirectOutput(logFile);
        builder.redirectError(logFile);
        process = builder.start();
        assertTrue(process.isAlive());

        OutputStream stdin = process.getOutputStream();

        writer = new BufferedWriter(new OutputStreamWriter(stdin));

        // check that the server has started
        Thread.sleep(5000);
        assertFalse(checkLogMessage(120000, "CWWKF0011I"));
        if (isDevMode) {
            assertFalse(checkLogMessage(60000, "Enter key to run tests on demand"));
        }

        // verify that the target directory was created
        targetDir = new File(buildDir, "build");
        assertTrue(targetDir.exists());
    }

    @Test
    public void configChangeTest() throws Exception {
        // configuration file change
        File srcServerXML = new File(buildDir, "src/main/liberty/config/server.xml");
        File targetServerXML = new File(targetDir, "wlp/usr/servers/defaultServer/server.xml");
        assertTrue(srcServerXML.exists());
        assertTrue(targetServerXML.exists());

        replaceString("</feature>", "</feature>\n" + "    <feature>mpHealth-2.0</feature>", srcServerXML);

        // check for server configuration was successfully updated message in messages.log
        File messagesLogFile = new File(targetDir, "wlp/usr/servers/defaultServer/logs/messages.log");
        assertFalse(checkLogMessage(60000, "CWWKG0017I", messagesLogFile));
        Thread.sleep(2000);
        Scanner scanner = new Scanner(targetServerXML);
        boolean foundUpdate = false;
        try {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains("<feature>mpHealth-2.0</feature>")) {
                    foundUpdate = true;
                    break;
                }
            }
        } finally {
            scanner.close();
        }
        assertTrue("Could not find the updated feature in the target server.xml file", foundUpdate);
    }

    @Test
    public void modifyJavaFileTest() throws Exception {
        // modify a java file
        File srcHelloWorld = new File(buildDir, "src/main/java/com/demo/HelloWorld.java");
        File targetHelloWorld = new File(targetDir, "classes/java/main/com/demo/HelloWorld.class");
        assertTrue(srcHelloWorld.exists());
        assertTrue(targetHelloWorld.exists());

        long lastModified = targetHelloWorld.lastModified();
        String str = "// testing";
        BufferedWriter javaWriter = new BufferedWriter(new FileWriter(srcHelloWorld, true));
        javaWriter.append(' ');
        javaWriter.append(str);

        javaWriter.close();

        Thread.sleep(5000); // wait for compilation
        boolean wasModified = targetHelloWorld.lastModified() > lastModified;
        assertTrue(wasModified);
    }

    @Test
    public void testDirectoryTest() throws Exception {
        // create the test directory
        File testDir = new File(buildDir, "src/test/java");
        assertTrue(testDir.mkdirs());

        // creates a java test file
        File unitTestSrcFile = new File(testDir, "UnitTest.java");
        String unitTest = """import org.junit.Test;\n
        import static org.junit.Assert.*;\n
        \n
        public class UnitTest {\n
        \n
        @Test\n
        public void testTrue() {\n
            assertTrue(true);\n
            \n
            }\n
        }"""
        Files.write(unitTestSrcFile.toPath(), unitTest.getBytes());
        assertTrue(unitTestSrcFile.exists());

        Thread.sleep(6000); // wait for compilation
        File unitTestTargetFile = new File(targetDir, "classes/java/test/UnitTest.class");
        assertTrue(unitTestTargetFile.exists());
        long lastModified = unitTestTargetFile.lastModified();

        // modify the test file
        String str = "// testing";
        BufferedWriter javaWriter = new BufferedWriter(new FileWriter(unitTestSrcFile, true));
        javaWriter.append(' ');
        javaWriter.append(str);

        javaWriter.close();

        Thread.sleep(2000); // wait for compilation
        assertTrue(unitTestTargetFile.lastModified() > lastModified);

        // delete the test file
        assertTrue(unitTestSrcFile.delete());
        Thread.sleep(2000);
        assertFalse(unitTestTargetFile.exists());

    }

    @Test
    public void manualTestsInvocationTest() throws Exception {
        assertFalse(checkLogMessage(2000,  "Press the Enter key to run tests on demand."));

        writer.write("\n");
        writer.flush();

        assertFalse(checkLogMessage(10000,  "Tests finished."));
    }

    protected static void replaceString(String str, String replacement, File file) throws IOException {
        Path path = file.toPath();
        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(path), charset);

        content = content.replaceAll(str, replacement);
        Files.write(path, content.getBytes(charset));
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        Path path = logFile.toPath();
        Charset charset = StandardCharsets.UTF_8;
        String content = new String(Files.readAllBytes(path), charset);
        System.out.println("Dev mode output: " + content);

        cleanUpAfterClass(true);
    }

    protected static void cleanUpAfterClass(boolean isDevMode) throws Exception {
        stopProcess(isDevMode);

        if (buildDir != null && buildDir.exists()) {
            try {
                FileUtils.deleteDirectory(buildDir);
            } catch (IOException e) {
                // https://github.com/OpenLiberty/open-liberty/issues/10562 prevents a file from being deleted.
                // Instead of failing here, just print an error until the above is fixed
                System.out.println("Could not clean up the build directory " + buildDir + ", IOException: " + e.getMessage());
                e.printStackTrace();
            } 
        }

        if (logFile != null && logFile.exists()) {
            assertTrue(logFile.delete());
        }
    }

    private static void stopProcess(boolean isDevMode) throws IOException, InterruptedException, FileNotFoundException {
        // shut down dev mode
        if (writer != null) {
            if(isDevMode) {
                writer.write("exit"); // trigger dev mode to shut down
            }
            else {
                process.destroy(); // stop run
            }
            writer.flush();
            writer.close();

            // test that dev mode has stopped running
            assertFalse(checkLogMessage(100000, "CWWKE0036I"));
        }
    }
    
}
