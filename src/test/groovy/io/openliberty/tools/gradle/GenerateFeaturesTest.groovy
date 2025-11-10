/*
 * (C) Copyright IBM Corporation 2022, 2025
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
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import static org.junit.Assert.*
import static io.openliberty.tools.common.plugins.util.BinaryScannerUtil.*;

class GenerateFeaturesTest extends BaseGenerateFeaturesTest {

    static File targetDir;

    @Before
    public void setUp() throws IOException, InterruptedException, FileNotFoundException {
        setUpBeforeTest("basic-dev-project");
        targetDir = new File(buildDir, targetDirName);
    }

    @After
    public void cleanUp() throws Exception {
        cleanUpAfterTest();
    }

    @Test
    public void basicTest() throws Exception {
        runCompileAndGenerateFeatures(null);
        executeBasicTests(newFeatureFile, "");
    }

    private void executeBasicTests(File featureFile, String options) {
        // verify that the target directory was created
        assertTrue(targetDir.exists());

        // verify that the generated features file was created
        assertTrue(formatOutput(getProcessOutput()), featureFile.exists());

        // verify that the correct features are in the generated-features.xml
        Set<String> features = readFeatures(featureFile);
        assertEquals(1, features.size());
        Set<String> expectedFeatures = new HashSet<String>(Arrays.asList("servlet-4.0"));
        assertEquals(expectedFeatures, features);

        // place generated features in server.xml
        replaceString("<!--replaceable-->",
            "<featureManager>\n" +
            "  <feature>servlet-4.0</feature>\n" +
            "</featureManager>\n", serverXmlFile);
        runGenerateFeatures(options);
        // no additional features should be generated
        assertTrue(featureFile.exists());
        features = readFeatures(featureFile);
        assertEquals(0, features.size());
    }

    @Test
    public void generateToSrcTest() throws Exception {
        String options = "--generateToSrc=true";
        newFeatureFile.delete(); // clean up from other tests but file may not be present so don't assert
        assertFalse(newFeatureFileSrc.exists()); // assuming no other test creates this file
        runCompileAndGenerateFeatures(options);

        executeBasicTests(newFeatureFileSrc, options);
        assertTrue(newFeatureFileSrc.delete()); // clean up the generated file
    }

    @Test
    public void internalDevModeTest() throws Exception {
        String options = "--internalDevMode=true";
        newFeatureFile.delete(); // clean up from other tests but file may not be present so don't assert
        assertFalse(newFeatureFileTmp.exists()); // assuming no other test creates this file
        runCompileAndGenerateFeatures(options);

        executeBasicTests(newFeatureFileTmp, options);
        assertTrue(newFeatureFileTmp.delete()); // clean up the generated file
    }

    @Test
    public void noClassFiles() throws Exception {
        // do not compile before running generateFeatures
        runGenerateFeatures("");

        // verify that generated features file was not created
        assertFalse(newFeatureFile.exists());

        // verify class files not found warning message
        assertTrue(processOutput.contains(GenerateFeaturesTask.NO_CLASS_FILES_WARNING));
    }

    /**
     * Verify a scanner log is generated when plugin logging is enabled.
     * 
     * @throws Exception
     */
    @Test
    public void scannerLogExistenceTest() throws Exception {
        File scannerLogDir = new File(targetDir, "logs");
        assertFalse(scannerLogDir.exists());

        runCompileAndGenerateFeatures(DEBUG_OPTION);
        assertTrue(scannerLogDir.exists());
        File[] logDirListing = scannerLogDir.listFiles();
        assertNotNull(logDirListing);
        boolean logExists = false;
        for (File child : logDirListing) {
            if (child.exists() && child.length() > 0) {
                logExists = true;
            }
        }
        assertTrue(logExists);
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
        runCompileAndGenerateFeatures(null);

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

        runCompileAndGenerateFeatures(null);

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

        runCompileAndGenerateFeatures(null);

        // verify that generated features file was created
        assertTrue(newFeatureFile.exists());

        // verify expected comment found in server.xml
        Charset charset = StandardCharsets.UTF_8;
        String serverXmlContents = new String(Files.readAllBytes(serverXmlFile.toPath()), charset);
        serverXmlContents = "\n" + serverXmlContents;
        assertTrue(serverXmlContents,
            verifyLogMessageExists(GenerateFeaturesTask.FEATURES_FILE_MESSAGE, 100, serverXmlFile));
    }

    /**
     * Conflict between user specified features.
     * Check for BINARY_SCANNER_CONFLICT_MESSAGE2 (conflict between configured features)
     *
     * @throws Exception
     */
    @Test
    public void userConflictTest() throws Exception {
        // app only uses servlet-4.0, servlet-4.0 conflicts with cdi-1.2
        replaceString("<!--replaceable-->",
            "<!--Feature generation comment goes below this line-->\n" +
            "  <featureManager>\n" +
            "    <feature>servlet-4.0</feature>\n" +
            "    <feature>cdi-1.2</feature>\n" +
            "  </featureManager>\n", serverXmlFile);
        runCompileAndGenerateFeatures(null);

        // Verify BINARY_SCANNER_CONFLICT_MESSAGE2 error is thrown (BinaryScannerUtil.RecommendationSetException)
        Set<String> recommendedFeatureSet = new HashSet<String>(Arrays.asList("servlet-4.0"));
        // search log file instead of process output because warning message in process output may be interrupted
        boolean b = verifyLogMessageExists(String.format(BINARY_SCANNER_CONFLICT_MESSAGE2, getCdi12ConflictingFeatures(), recommendedFeatureSet), 1000, logFile);
        assertTrue(formatOutput(getProcessOutput()), b);
    }

    /**
     * Conflict between user specified features and API usage.
     * Check for BINARY_SCANNER_CONFLICT_MESSAGE1 (conflict between configured features and API usage)
     *
     * @throws Exception
     */
    @Test
    public void userAndGeneratedConflictTest() throws Exception {
        // app only uses servlet-4.0 (which will be generated), cdi-1.2 conflicts with servlet-4.0
        replaceString("<!--replaceable-->",
            "<!--Feature generation comment goes below this line-->\n" +
            "  <featureManager>\n" +
            "    <feature>cdi-1.2</feature>\n" +
            "  </featureManager>\n", serverXmlFile);
        runCompileAndGenerateFeatures(null);

        // Verify BINARY_SCANNER_CONFLICT_MESSAGE1 error is thrown (BinaryScannerUtil.FeatureModifiedException)
        Set<String> recommendedFeatureSet = new HashSet<String>(Arrays.asList("cdi-2.0", "servlet-4.0"));
        // search log file instead of process output because warning message in process output may be interrupted
        boolean b = verifyLogMessageExists(String.format(BINARY_SCANNER_CONFLICT_MESSAGE1, getCdi12ConflictingFeatures(), recommendedFeatureSet), 1000, logFile);
        assertTrue(formatOutput(getProcessOutput()), b);
    }

    // TODO add an integration test for feature conflict for API usage (BINARY_SCANNER_CONFLICT_MESSAGE3), ie. MP4 and EE9

    /**
     * Conflict between required features in API usage or configured features and MP/EE level specified
     * Check for BINARY_SCANNER_CONFLICT_MESSAGE5 (feature unavailable for required MP/EE levels)
     *
     * @throws Exception
     */
    @Test
    public void featureUnavailableConflictTest() throws Exception {
        // use EE 8 and MP 1.2
        replaceString("org.eclipse.microprofile:microprofile:3.2", "org.eclipse.microprofile:microprofile:1.2", buildFile);

        // add mpOpenAPI-1.0 feature to server.xml, not available in MP 1.2
        replaceString("<!--replaceable-->",
                "<!--Feature generation comment goes below this line-->\n" +
                        "  <featureManager>\n" +
                        "    <feature>mpOpenAPI-1.0</feature>\n" +
                        "  </featureManager>\n", serverXmlFile);
        runCompileAndGenerateFeatures(null);

        // use just beginning of BINARY_SCANNER_CONFLICT_MESSAGE5 as error message in logFile may be interrupted with "1 actionable task: 1 executed"
        assertTrue("Could not find the feature unavailable conflict message in the process output.\n" + processOutput,
                verifyLogMessageExists("required features: [servlet-4.0, mpOpenAPI-1.0]" +
                        " and required levels of MicroProfile: mp1.2, Java EE or Jakarta EE: ee8", 1000, logFile));

    }

    /**
     * Test calling the scanner with both the EE umbrella dependency and the MP
     * umbrella dependency.
     * 
     * @throws Exception
     */
    @Test
    public void bothEEMPUmbrellaTest() throws Exception {
        runCompileAndGenerateFeatures(DEBUG_OPTION);
        // Check for "  targetJavaEE: null" in the debug output
        String line = findLogMessage(TARGET_EE_NULL, 5000, logFile);
        assertNull("Target EE:'" + line+"'", line);
        // Check for "  targetJavaMP: null" in the debug output
        line = findLogMessage(TARGET_MP_NULL, 5000, logFile);
        assertNull("Target MP:" + line, line);
    }

    /**
     * Test calling the scanner with just the EE umbrella dependency and no MP
     * umbrella dependency.
     * 
     * @throws Exception
     */
    @Test
    public void onlyEEUmbrellaTest() throws Exception {
        replaceString(UMBRELLA_MP, ESA_MP_DEPENDENCY, buildFile);
        removeUnusedUmbrellas(buildFile);
        runCompileAndGenerateFeatures(DEBUG_OPTION);
        // Check for "  targetJavaEE: null" in the debug output
        String line = findLogMessage(TARGET_EE_NULL, 5000, logFile);
        assertNull("Target EE:" + line, line);
        // Check for "  targetJavaMP: null" in the debug output
        line = findLogMessage(TARGET_MP_NULL, 5000, logFile);
        assertNotNull("Target MP:" + line, line);
    }

    /**
     * Test calling the scanner with just the MP umbrella dependency and no EE
     * umbrella dependency.
     * 
     * @throws Exception
     */
    @Test
    public void onlyMPUmbrellaTest() throws Exception {
        replaceString(UMBRELLA_EE, ESA_EE_DEPENDENCY, buildFile);
        removeUnusedUmbrellas(buildFile);
        runCompileAndGenerateFeatures(DEBUG_OPTION);
        // Check for "  targetJavaEE: null" in the debug output
        String line = findLogMessage(TARGET_EE_NULL, 5000, logFile);
        assertNotNull("Target EE:" + line, line);
        // Check for "  targetJavaMP: null" in the debug output
        line = findLogMessage(TARGET_MP_NULL, 5000, logFile);
        assertNull("Target MP:" + line, line);
    }

    /**
     * Test calling the scanner with no EE umbrella dependency and no MP
     * umbrella dependency.
     * 
     * @throws Exception
     */
    @Test
    public void noUmbrellaTest() throws Exception {
        replaceString(UMBRELLA_EE, ESA_EE_DEPENDENCY, buildFile);
        replaceString(UMBRELLA_MP, ESA_MP_DEPENDENCY, buildFile);
        removeUnusedUmbrellas(buildFile);
        runCompileAndGenerateFeatures(DEBUG_OPTION);
        // Check for "  targetJavaEE: null" in the debug output
        String line = findLogMessage(TARGET_EE_NULL, 5000, logFile);
        assertNotNull("Target EE:" + line, line);
        // Check for "  targetJavaMP: null" in the debug output
        line = findLogMessage(TARGET_MP_NULL, 5000, logFile);
        assertNotNull("Target MP:" + line, line);
    }

    protected Set<String> getCdi12ConflictingFeatures() {
        // servlet-4.0 (EE8) conflicts with cdi-1.2 (EE7)
        Set<String> conflictingFeatures = new HashSet<String>();
        conflictingFeatures.add("servlet-4.0");
        conflictingFeatures.add("cdi-1.2");
        return conflictingFeatures;
    }

    // The lower level, unused umbrella dependencies interfere with some tests.
    protected void removeUnusedUmbrellas(File file) {
        replaceString(UMBRELLA_EE_OLD, "", file);
        replaceString(UMBRELLA_MP_OLD, "", file);
    }
}
