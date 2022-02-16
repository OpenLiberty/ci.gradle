/*
 * (C) Copyright IBM Corporation 2022.
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class BaseDevTest extends AbstractIntegrationTest {
    static final String projectName = "basic-dev-project";

    static File resourceDir = new File("build/resources/test/dev-test/" + projectName);
    static File buildDir = new File(integTestDir, "dev-test/" + projectName + System.currentTimeMillis()); // append timestamp in case previous build was not deleted
    static String buildFilename = "build.gradle";

    static File targetDir;
    static BufferedWriter writer;
    // ProcessBuilder.redirectOutput() and .redirectError() are not threadsafe.
    // Dev mode sends the Liberty output and error output to stderr while
    // regular dev mode messages are sent to stdout. If you try to redirect
    // both streams into one file they will both be corrupted and tests 
    // will fail. When you verify a log message exists you must specify
    // the correct file. Use logFile for "compilation was successful"
    // and errFile for "compilation had errors" or Liberty messages like 
    // "CWWKF0011I" or "The server installed the following features".
    static File logFile = new File(buildDir, "output.log");
    static File errFile = new File(buildDir, "stderr.log");
    static Process process;

    protected static void runDevMode() throws IOException, InterruptedException, FileNotFoundException {
        System.out.println("Starting dev mode...");
        startProcess(null, true);
        System.out.println("Started dev mode");
    }

    protected static ProcessBuilder buildProcess(String processCommand) {
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

    protected static boolean verifyLogMessage(int timeout, String message)
            throws InterruptedException, FileNotFoundException {
        verifyLogMessage(timeout, message, logFile)
    }

    protected static boolean verifyLogMessage(int timeout, String message, File file)
            throws InterruptedException, FileNotFoundException {
        int waited = 0;
        int sleep = 100;
        while (waited <= timeout) {
            if (readFile(message, file)) {
                Thread.sleep(1000);
                return true;
            }
            Thread.sleep(sleep);
            waited += sleep;
        }
        return false;
    }

    protected static boolean verifyLogMessage(int timeout, String message, int occurrences)
            throws InterruptedException, FileNotFoundException {
        verifyLogMessage(timeout, message, logFile, occurrences)
    }

    protected static boolean verifyLogMessage(int timeout, String message, File file, int occurrences)
            throws InterruptedException, FileNotFoundException {
        int waited = 0;
        int sleep = 10;
        while (waited <= timeout) {
            Thread.sleep(sleep);
            waited += sleep;
            if (countOccurrences(message, file) == occurrences) {
                return true;
            }
        }
        return false;
    }

    protected static boolean verifyFileExists(File file, int timeout)
            throws InterruptedException {
        int waited = 0;
        int sleep = 100;
        while (waited <= timeout) {
            if (file.exists()) {
                return true;
            }
            Thread.sleep(sleep);
            waited += sleep;
        }
        return false;
    }

    protected static boolean verifyFileDoesNotExist(File file, int timeout)
            throws InterruptedException {
        int waited = 0;
        int sleep = 100;
        while (waited <= timeout) {
            Thread.sleep(sleep);
            waited += sleep;
            if (!file.exists()) {
                return true;
            }
        }
        return false;
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
        builder.redirectError(errFile);
        process = builder.start();
        assertTrue(process.isAlive());

        OutputStream stdin = process.getOutputStream();

        writer = new BufferedWriter(new OutputStreamWriter(stdin));

        // check that the server has started
        assertTrue(verifyLogMessage(120000, "CWWKF0011I", errFile));
        if (isDevMode) {
            assertTrue(verifyLogMessage(60000, "Liberty is running in dev mode."));
        }

        // verify that the target directory was created
        targetDir = new File(buildDir, "build");
        assertTrue(targetDir.exists());
    }

    protected static String getContents(File f, String hdr) {
        Path path = f.toPath();
        Charset charset = StandardCharsets.UTF_8;
        String s = new String(Files.readAllBytes(path), charset);
        String content = "\n" + hdr + " v(" + s.length() + ")\n"
        content += s
        content +=  "\n" + hdr + " ^(" + s.length() + ")\n"
        return content
    }
    protected static void replaceString(String str, String replacement, File file) throws IOException {
        Path path = file.toPath();
        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(path), charset);

        content = content.replaceAll(str, replacement);
        Files.write(path, content.getBytes(charset));
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
            int serverStoppedOccurrences = countOccurrences("CWWKE0036I", logFile);
            if (isDevMode) {
                writer.write("exit"); // trigger dev mode to shut down
            } else {
                process.destroy(); // stop run
            }
            writer.flush();
            writer.close();

            // test that dev mode has stopped running
            assertTrue(verifyLogMessage(100000, "CWWKE0036I", errFile, ++serverStoppedOccurrences));
        }
    }

    /**
     * Count number of lines that contain the given string
     */
    protected static int countOccurrences(String str, File file) throws FileNotFoundException, IOException {
        int occurrences = 0;
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = br.readLine();
        try {
            while (line != null) {
                if (line.contains(str)) {
                    occurrences++;
                }
                line = br.readLine();
            }
        } finally {
            br.close();
        }
        return occurrences;
    }
}
