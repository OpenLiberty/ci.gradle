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
        targetDir = new File(buildDir, "build");
    }

    @After
    public void cleanUp() throws Exception {
        cleanUpAfterTest();
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
        runCompileAndGenerateFeatures();

        // Verify BINARY_SCANNER_CONFLICT_MESSAGE2 error is thrown (BinaryScannerUtil.RecommendationSetException)
        Set<String> recommendedFeatureSet = new HashSet<String>();
        recommendedFeatureSet.addAll("servlet-4.0");
        // search log file instead of process output because warning message in process output may be interrupted
        verifyLogMessageExists(String.format(BINARY_SCANNER_CONFLICT_MESSAGE2, getCdi12ConflictingFeatures(), recommendedFeatureSet), 1000, logFile);
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
        runCompileAndGenerateFeatures();

        // Verify BINARY_SCANNER_CONFLICT_MESSAGE1 error is thrown (BinaryScannerUtil.FeatureModifiedException)
        Set<String> recommendedFeatureSet = new HashSet<String>();
        recommendedFeatureSet.addAll("cdi-2.0");
        recommendedFeatureSet.addAll("servlet-4.0");
        // search log file instead of process output because warning message in process output may be interrupted
        verifyLogMessageExists(String.format(BINARY_SCANNER_CONFLICT_MESSAGE1, getCdi12ConflictingFeatures(), recommendedFeatureSet), 1000, logFile);
    }

    // TODO add an integration test for feature conflict for API usage (BINARY_SCANNER_CONFLICT_MESSAGE3), ie. MP4 and EE9

    protected Set<String> getCdi12ConflictingFeatures() {
        // servlet-4.0 (EE8) conflicts with cdi-1.2 (EE7)
        Set<String> conflictingFeatures = new HashSet<String>();
        conflictingFeatures.add("servlet-4.0");
        conflictingFeatures.add("cdi-1.2");
        return conflictingFeatures;
    }

}
