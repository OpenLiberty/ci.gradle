/*
 * (C) Copyright IBM Corporation 2020, 2022.
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

class DevRecompileTest extends BaseDevTest {

    @BeforeClass
    public static void setup() throws IOException, InterruptedException, FileNotFoundException {
        createDir(buildDir);
        createTestProject(buildDir, resourceDir, buildFilename);
        runDevMode();
    }

    @Test
    /* simple double check. if failure, check parse in ci.common */
    public void verifyJsonHost() throws Exception {
        assertTrue(verifyLogMessage(2000, WEB_APP_AVAILABLE, errFile));   // Verify web app code triggered
        // TODO assertTrue(verifyLogMessage(2000, "http:\\/\\/"));  // Verify escape char seq passes
    }

    @Test
    public void generateFeatureRecompileTest() throws Exception {
        assertFalse(verifyLogMessage(10000, "batch-1.0", errFile)); // not present on server yet
        // Verify generate features runs when dev mode first starts
        assertTrue(verifyLogMessage(10000, RUNNING_GENERATE_FEATURES));
        int runGenerateFeaturesCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
        int installedFeaturesCount = countOccurrences(SERVER_INSTALLED_FEATURES, errFile);

        File newFeatureFile = new File(buildDir, "src/main/liberty/config/configDropins/overrides/"+GENERATED_FEATURES_FILE_NAME);
        File newTargetFeatureFile = new File(targetDir, "wlp/usr/servers/defaultServer/configDropins/overrides/"+GENERATED_FEATURES_FILE_NAME);
        File serverXmlFile = new File(buildDir, "src/main/liberty/config/server.xml");
        assertTrue(serverXmlFile.exists());

        String batchCode = """package com.demo;\n
        \n
        import javax.ws.rs.GET;\n
        import javax.ws.rs.Path;\n
        import javax.ws.rs.Produces;\n
        import javax.batch.api.Batchlet;\n
        \n
        import static javax.ws.rs.core.MediaType.TEXT_PLAIN;\n
        \n
        @Path("/batchlet")\n
        public class HelloBatch implements Batchlet {\n
        \n
            @GET\n
            @Produces(TEXT_PLAIN)\n
            public String process() {\n
                return "Batchlet.process()";\n
            }\n
            public void stop() {}\n
        }"""
        File helloBatchSrc = new File(buildDir, "src/main/java/com/demo/HelloBatch.java");
        Files.write(helloBatchSrc.toPath(), batchCode.getBytes());
        assertTrue(helloBatchSrc.exists());

        // Dev mode should now compile the new Java file...
        File helloBatchObj = new File(targetDir, "classes/com/demo/HelloBatch.class");
        verifyFileExists(helloBatchObj, 15000);
        // ... and run the proper task.
        assertTrue(verifyLogMessage(10000, RUNNING_GENERATE_FEATURES, ++runGenerateFeaturesCount));
        assertTrue(verifyFileExists(newFeatureFile, 5000)); // task created file
        assertTrue(verifyFileExists(newTargetFeatureFile, 5000)); // dev mode copied file
        assertTrue(verifyLogMessage(10000, "batch-1.0", newFeatureFile));
        assertTrue(verifyLogMessage(10000, NEW_FILE_INFO_MESSAGE, newFeatureFile));
        assertTrue(verifyLogMessage(10000, SERVER_XML_COMMENT, serverXmlFile));
        // should appear as part of the message "CWWKF0012I: The server installed the following features:"
        assertTrue(verifyLogMessage(123000, SERVER_INSTALLED_FEATURES, errFile, ++installedFeaturesCount));

        // When there is a compilation error the generate features process should not run
        final String goodCode = "import javax.ws.rs.GET;";
        final String badCode  = "import javax.ws.rs.GET";
        int errCount = countOccurrences(COMPILATION_ERRORS, logFile);
        replaceString(goodCode, badCode, helloBatchSrc);

        assertTrue(verifyLogMessage(10000, COMPILATION_ERRORS, errCount+1)); // wait for compilation
        // after failed compilation generate features is not run.
        int updatedgenFeaturesCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
        assertEquals(runGenerateFeaturesCount, updatedgenFeaturesCount);

        // after successful compilation run generate features. "Regenerate" message should appear after.
        int goodCount = countOccurrences(COMPILATION_SUCCESSFUL, logFile);
        int regenerateCount = countOccurrences(REGENERATE_FEATURES, logFile);
        replaceString(badCode, goodCode, helloBatchSrc);
        assertTrue(verifyLogMessage(10000, COMPILATION_SUCCESSFUL, goodCount+1));

        // TODO Restore these tests once issue 757 is fixed.
        // assertTrue(s, verifyLogMessage(10000, RUNNING_GENERATE_FEATURES, ++runGenerateFeaturesCount));
        // assertTrue(s, verifyLogMessage(10000, REGENERATE_FEATURES, ++regenerateCount));
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        String stdout = getContents(logFile, "Dev mode std output");
        System.out.println(stdout);
        String stderr = getContents(errFile, "Dev mode std error");
        System.out.println(stderr);
        cleanUpAfterClass(true);
    }
}
