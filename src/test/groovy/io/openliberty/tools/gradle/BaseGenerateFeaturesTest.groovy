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
package io.openliberty.tools.gradle

import org.codehaus.plexus.util.FileUtils
import org.w3c.dom.Document

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

/**
 * Helper class for generate features tests
 */
class BaseGenerateFeaturesTest extends AbstractIntegrationTest {

    static String projectName;
    static final String buildFilename = "build.gradle";
    static final String targetDirName = "build";
    static File buildFile;
    static File resourceDir;
    static File buildDir;

    static BufferedWriter writer;
    static File logFile;
    static Process process;
    static String processOutput = "";

    static File serverXmlFile;
    static File newFeatureFile;

    static final String GENERATED_FEATURES_FILE_NAME = "generated-features.xml";
    static final String GENERATED_FEATURES_FILE_PATH = "/src/main/liberty/config/configDropins/overrides/" + GENERATED_FEATURES_FILE_NAME;
    static final String UMBRELLA_EE = "providedCompile \"jakarta.platform:jakarta.jakartaee-api:8.0.0\"";
    static final String UMBRELLA_MP = "providedCompile \"org.eclipse.microprofile:microprofile:3.2\"";
    static final String ESA_EE_DEPENDENCY = "providedCompile 'io.openliberty.features:servlet-4.0:22.0.0.2'";
    static final String ESA_MP_DEPENDENCY = "providedCompile 'io.openliberty.features:microProfile-3.2:22.0.0.2'";
    static final String UMBRELLA_EE_OLD = "providedCompile 'javax:javaee-api:7.0'";
    static final String UMBRELLA_MP_OLD = "providedCompile 'org.eclipse.microprofile:microprofile:1.2'";
    static final String TARGET_EE_NULL = "targetJavaEE: null";
    static final String TARGET_MP_NULL = "targetMicroP: null";

    protected static void setUpBeforeTest(String projectName) throws IOException, InterruptedException, FileNotFoundException {
        this.projectName = projectName;
        this.resourceDir = new File("build/resources/test/generate-features-test/" + projectName);
        this.buildDir = new File(integTestDir, "generate-features-test/" + projectName + System.currentTimeMillis());
        this.logFile = new File(buildDir, "output.log");
        createDir(buildDir);
        createTestProject(buildDir, resourceDir, buildFilename);
        this.newFeatureFile = new File(buildDir, GENERATED_FEATURES_FILE_PATH);
        this.buildFile = new File(buildDir, buildFilename);
        serverXmlFile = new File(buildDir, "src/main/liberty/config/server.xml");
    }

    protected static void cleanUpAfterTest() throws Exception {
        if (writer != null) {
            process.destroy(); // stop run
            writer.flush();
            writer.close();
        }

        // clean up build directory
        if (buildDir.exists()) {
            FileUtils.deleteDirectory(buildDir);
        }
        // delete log file
        if (logFile != null && logFile.exists()) {
            assertTrue(logFile.delete());
        }
    }

    protected static void replaceString(String str, String replacement, File file) throws IOException {
        Path path = file.toPath();
        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(path), charset);

        content = content.replaceAll(str, replacement);
        Files.write(path, content.getBytes(charset));
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

    private static void runProcess(String processCommand) throws IOException, InterruptedException, FileNotFoundException {
        // get gradle wrapper from project root dir
        File gradlew;
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().startsWith("windows")) {
            gradlew = new File("gradlew.bat");
        } else {
            gradlew = new File("gradlew");
        }

        StringBuilder command = new StringBuilder(gradlew.getAbsolutePath() + " " + processCommand);

        System.out.println("Running command: " + command.toString());
        ProcessBuilder builder = buildProcess(command.toString());

        builder.redirectOutput(logFile);
        builder.redirectError(logFile);
        process = builder.start();
        assertTrue(process.isAlive());

        OutputStream stdin = process.getOutputStream();

        writer = new BufferedWriter(new OutputStreamWriter(stdin));

        // wait for process to finish max 20 seconds
        process.waitFor(20, TimeUnit.SECONDS);
        assertFalse(process.isAlive());

        System.out.println(formatOutput(getProcessOutput()));
    }

    protected static String getProcessOutput() {
        // save and return process output
        Path path = logFile.toPath();
        Charset charset = StandardCharsets.UTF_8;
        processOutput = new String(Files.readAllBytes(path), charset);
        return processOutput;
    }

    protected static boolean verifyLogMessageExists(String message, int timeout, File log)
            throws InterruptedException, FileNotFoundException, IOException {
        int waited = 0;
        int sleep = 10;
        while (waited <= timeout) {
            if (readFile(message, log) != null) {
                return true;
            }
            Thread.sleep(sleep);
            waited += sleep;
        }
        return false;
    }

    protected static String findLogMessage(String message, int timeout, File log)
        throws InterruptedException, FileNotFoundException, IOException {
        int waited = 0;
        int sleep = 10;
        while (waited <= timeout) {
            String line = readFile(message, log);
            if (line != null) {
                return line;
            }
            Thread.sleep(sleep);
            waited += sleep;
        }
        return null;
    }

    protected static String readFile(String str, File file) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = br.readLine();
        try {
            while (line != null) {
                if (line.contains(str)) {
                    return line;
                }
                line = br.readLine();
            }
        } finally {
            br.close();
        }
        return null;
    }

    /**
     * Given an configuration XML file return the features in the featureManager
     * element if any
     *
     * @param file configuration XML file
     * @return set of features, empty list if no features are found
     */
    protected static Set<String> readFeatures(File configurationFile) throws Exception {
        Set<String> features = new HashSet<String>();

        // return empty list if file does not exist or is not an XML file
        if (!configurationFile.exists() || !configurationFile.getName().endsWith(".xml")) {
            return features;
        }

        // read configuration xml file
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setIgnoringComments(true);
        docBuilderFactory.setCoalescing(true);
        docBuilderFactory.setIgnoringElementContentWhitespace(true);
        docBuilderFactory.setValidating(false);
        DocumentBuilder documentBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = documentBuilder.parse(configurationFile);

        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "/server/featureManager/feature";
        org.w3c.dom.NodeList nodes = (org.w3c.dom.NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            features.add(nodes.item(i).getTextContent());
        }
        return features;
    }

    protected static void runCompileAndGenerateFeatures() throws IOException, InterruptedException, FileNotFoundException {
        runProcess("compileJava generateFeatures");
    }

    protected static void runCompileAndGenerateFeaturesDebug() throws IOException, InterruptedException, FileNotFoundException {
        runProcess("compileJava generateFeatures --debug");
    }

    protected static void runGenerateFeatures() throws IOException, InterruptedException, FileNotFoundException {
        runProcess("generateFeatures");
    }

    // Format the output to help debug test failures.
    // The problem is that the test case log looks just like the JUnit log of
    // the calling process.
    // For a short stream just print it. Add a start and end string to separate
    // it from the rest of the log.
    // For long output streams, add a header which indicates how many lines to skip
    // if you want to read the end. Also add a trailer to similarly show how many
    // lines to scroll up to find the beginning. Number each line to help parse
    // the output.
    protected static String formatOutput(String output) {
        if (output == null || output.length() < 101) {
            return "\n==Process Output==\n" + output + "\n==End==";
        }
        String[] lines = output.split("\r\n|\r|\n");
        StringBuffer result = new StringBuffer(String.format("==Process Output %d lines==\n", lines.length));
        int count = 1;
        for (String line : lines) {
            result.append(String.format("%5d>%s\n", count++, line));
        }
        result.append(String.format("==Process Output End %d lines==\n", lines.length));
        return result.toString();
    }
}
