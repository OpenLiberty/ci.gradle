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

import io.openliberty.tools.gradle.tasks.GenerateFeaturesTask
import org.codehaus.plexus.util.FileUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
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

import static org.junit.Assert.*

class GenerateFeaturesTest extends AbstractIntegrationTest {
    static final String projectName = "basic-dev-project";

    static File resourceDir = new File("build/resources/test/generate-features-test/" + projectName);
    static File buildDir = new File(integTestDir, "generate-features-test/" + projectName + System.currentTimeMillis());
    static String buildFilename = "build.gradle";

    static BufferedWriter writer;
    static File logFile = new File(buildDir, "output.log");
    static Process process;
    static String processOutput = "";

    static File newFeatureFile;
    static File serverXmlFile;
    static File targetDir;

    static final String GENERATED_FEATURES_FILE_NAME = "generated-features.xml";
    static final String GENERATED_FEATURES_FILE_PATH = "/src/main/liberty/config/configDropins/overrides/" + GENERATED_FEATURES_FILE_NAME;

    @Before
    public void setUp() throws IOException, InterruptedException, FileNotFoundException {
        createDir(buildDir);
        createTestProject(buildDir, resourceDir, buildFilename);

        newFeatureFile = new File(buildDir, GENERATED_FEATURES_FILE_PATH);
        serverXmlFile = new File(buildDir, "src/main/liberty/config/server.xml");
        targetDir = new File(buildDir, "build");
    }

    @After
    public void cleanUp() throws Exception {
        if (writer != null) {
            process.destroy(); // stop run
            writer.flush();
            writer.close();
        }

        // clean up build directory
        if (buildDir.exists()) {
            FileUtils.deleteDirectory(buildDir);
        }
    }

    @Test
    public void basicTest() throws Exception {
        runCompileAndGenerateFeatures();
        // verify that the target directory was created
        assertTrue(targetDir.exists());

        // verify that the generated features file was created
        assertTrue(newFeatureFile.exists());

        // verify that the correct features are in the generated-features.xml
        Set<String> features = readFeatures(newFeatureFile);
        assertEquals(1, features.size());
        Set<String> expectedFeatures = new HashSet<String>(Arrays.asList("servlet-4.0"));
        assertEquals(expectedFeatures, features);

        // place generated features in server.xml
        replaceString("<!--replaceable-->",
                "<featureManager>\n" +
                        "  <feature>servlet-4.0</feature>\n" +
                        "</featureManager>\n", serverXmlFile);

        runGenerateFeatures();
        // no additional features should be generated
        assertTrue(newFeatureFile.exists());
        features = readFeatures(newFeatureFile);
        assertEquals(0, features.size());
    }

    @Test
    public void noClassFiles() throws Exception {
        // do not compile before running generateFeatures
        runGenerateFeatures();

        // verify that generated features file was not created
        assertFalse(newFeatureFile.exists());

        // verify class files not found warning message
        assertTrue(processOutput.contains(GenerateFeaturesTask.NO_CLASS_FILES_WARNING));
    }

    @Test
    public void customFeaturesTest() throws Exception {
        // complete the setup of the test
        replaceString("<!--replaceable-->",
            "<featureManager>\n" +
            "  <feature>jaxrs-2.1</feature>\n" +
            "  <feature>usr:custom-1.0</feature>\n" +
            "</featureManager>\n", serverXmlFile);
        assertFalse("Before running", newFeatureFile.exists());
        // run the test
        runCompileAndGenerateFeatures();

        // verify that the generated features file was created
        assertTrue(newFeatureFile.exists());

        // verify that the correct features are in the generated-features.xml
        Set<String> features = readFeatures(newFeatureFile);
        assertEquals(1, features.size());
        Set<String> expectedFeatures = new HashSet<String>(Arrays.asList("servlet-4.0"));
        assertEquals(expectedFeatures, features);
    }

    @Test
    public void serverXmlCommentNoFMTest() throws Exception {
        // initially the expected comment is not found in server.xml
        assertFalse(verifyLogMessageExists(GenerateFeaturesTask.FEATURES_FILE_MESSAGE, 10, serverXmlFile));
        // also we wish to test behaviour when there is no <featureManager> element so test that
        assertFalse(verifyLogMessageExists("<featureManager>", 10, serverXmlFile));

        runCompileAndGenerateFeatures();

        // verify that generated features file was created
        assertTrue(newFeatureFile.exists());

        // verify expected comment found in server.xml
        Charset charset = StandardCharsets.UTF_8;
        String serverXmlContents = new String(Files.readAllBytes(serverXmlFile.toPath()), charset);
        serverXmlContents = "\n" + serverXmlContents;
        assertTrue(serverXmlContents,
            verifyLogMessageExists(GenerateFeaturesTask.FEATURES_FILE_MESSAGE, 100, serverXmlFile));
    }

    @Test
    public void serverXmlCommentFMTest() throws Exception {
        replaceString("<!--replaceable-->",
            "<!--Feature generation comment goes below this line-->\n" +
            "  <featureManager>\n" +
            "    <feature>jaxrs-2.1</feature>\n" +
            "  </featureManager>\n", serverXmlFile);

        // initially the expected comment is not found in server.xml
        assertFalse(verifyLogMessageExists(GenerateFeaturesTask.FEATURES_FILE_MESSAGE, 10, serverXmlFile));

        runCompileAndGenerateFeatures();

        // verify that generated features file was created
        assertTrue(newFeatureFile.exists());

        // verify expected comment found in server.xml
        Charset charset = StandardCharsets.UTF_8;
        String serverXmlContents = new String(Files.readAllBytes(serverXmlFile.toPath()), charset);
        serverXmlContents = "\n" + serverXmlContents;
        assertTrue(serverXmlContents,
            verifyLogMessageExists(GenerateFeaturesTask.FEATURES_FILE_MESSAGE, 100, serverXmlFile));
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

        // save and print process output
        Path path = logFile.toPath();
        Charset charset = StandardCharsets.UTF_8;
        processOutput = new String(Files.readAllBytes(path), charset);
        System.out.println("Process output: " + processOutput);
    }

    protected static boolean verifyLogMessageExists(String message, int timeout, File log)
        throws InterruptedException, FileNotFoundException, IOException {
        int waited = 0;
        int sleep = 10;
        while (waited <= timeout) {
            if (readFile(message, log)) {
                return true;
            }
            Thread.sleep(sleep);
            waited += sleep;
        }
        return false;
    }

    protected static boolean readFile(String str, File file) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = br.readLine();
        try {
            while (line != null) {
                if (line.contains(str)) {
                    return true;
                }
                line = br.readLine();
            }
        } finally {
            br.close();
        }
        return false;
    }

    /**
     * Given an configuration XML file return the features in the featureManager
     * element if any
     *
     * @param file configuration XML file
     * @return set of features, empty list if no features are found
     */
    private static Set<String> readFeatures(File configurationFile) throws Exception {
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

    private static void runCompileAndGenerateFeatures() throws IOException, InterruptedException, FileNotFoundException {
        runProcess("compileJava generateFeatures");
    }

    private static void runGenerateFeatures() throws IOException, InterruptedException, FileNotFoundException {
        runProcess("generateFeatures");
    }
}