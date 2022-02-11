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

}
