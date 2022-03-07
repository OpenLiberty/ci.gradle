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

class DevTest extends BaseDevTest {

    @BeforeClass
    public static void setup() throws IOException, InterruptedException, FileNotFoundException {
        createDir(buildDir);
        createTestProject(buildDir, resourceDir, buildFilename);
        runDevMode();
    }

    @Test
    /* simple double check. if failure, check parse in ci.common */
    public void verifyJsonHost() throws Exception {
        assertTrue(verifyLogMessage(2000, "CWWKT0016I", errFile));   // Verify web app code triggered
        // TODO assertTrue(verifyLogMessage(2000, "http:\\/\\/"));  // Verify escape char seq passes
    }

    @Test
    public void configChangeTest() throws Exception {
        int generateFeaturesCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
        // configuration file change
        File srcServerXML = new File(buildDir, "src/main/liberty/config/server.xml");
        File targetServerXML = new File(targetDir, "wlp/usr/servers/defaultServer/server.xml");
        assertTrue(srcServerXML.exists());
        assertTrue(targetServerXML.exists());

        replaceString("</feature>", "</feature>\n" + "    <feature>mpHealth-2.0</feature>", srcServerXML);

        // check that features have been generated
        assertTrue(verifyLogMessage(10000, RUNNING_GENERATE_FEATURES, ++generateFeaturesCount)); // task ran

        // check for server configuration was successfully updated message in messages.log
        File messagesLogFile = new File(targetDir, "wlp/usr/servers/defaultServer/logs/messages.log");
        assertTrue(verifyLogMessage(60000, "CWWKG0017I", messagesLogFile));
        assertTrue("Could not find the updated feature in the target server.xml file",
            verifyLogMessage(60000, "<feature>mpHealth-2.0</feature>", targetServerXML));
    }

    @Test
    public void configIncludesChangeTest() throws Exception {
        // add a feature to an <includes> server configuration file, ensure that
        // generate-features is called and the server configuration is updated
        int generateFeaturesCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
        File srcServerXMLIncludes = new File(buildDir, "src/main/liberty/config/extraFeatures.xml");
        File targetServerXMLIncludes = new File(targetDir, "wlp/usr/servers/defaultServer/extraFeatures.xml");
        assertTrue(srcServerXMLIncludes.exists());
        assertTrue(targetServerXMLIncludes.exists());

        // place previously generated feature in the includes extraFeatures.xml file
        replaceString("<!-- replace -->", "<feature>servlet-4.0</feature>", srcServerXMLIncludes);

        // check that features have been generated (no additional features generated)
        assertTrue(verifyLogMessage(10000, RUNNING_GENERATE_FEATURES, ++generateFeaturesCount)); // task ran

        // check for server configuration update
        File messagesLogFile = new File(targetDir, "wlp/usr/servers/defaultServer/logs/messages.log");
        assertTrue(verifyLogMessage(60000, "CWWKG0016I", messagesLogFile));
        assertTrue("Could not find the updated feature in the target extraFeatures.xml file",
                verifyLogMessage(60000, "<feature>servlet-4.0</feature>", targetServerXMLIncludes));
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

        // The resolution of File.lastModified() is 1000 ms so wait long enough for lastModified() to register the modification.
        Thread.sleep(2000);
        assertTrue(unitTestTargetFile.lastModified() > lastModified);

        // delete the test file
        assertTrue(unitTestSrcFile.delete());
        Thread.sleep(2000);
        assertFalse(unitTestTargetFile.exists());

    }

    @Test
    public void manualTestsInvocationTest() throws Exception {
        writer.write("\n");
        writer.flush();

        assertTrue(verifyLogMessage(10000,  "Tests finished."));
    }

    @Test
    public void generateFeatureTest() throws Exception {

        final String GENERATED_FEATURES_FILE_NAME = "generated-features.xml";
        final String SERVER_XML_COMMENT = "Plugin has generated Liberty features"; // the explanation added to server.xml
        final String NEW_FILE_INFO_MESSAGE = "This file was generated by the Liberty Gradle Plugin and will be overwritten"; // the explanation added to the generated features file

        // Verify generate features runs when dev mode first starts
        assertTrue(verifyLogMessage(10000, RUNNING_GENERATE_FEATURES));
        int generateFeaturesCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
        assertFalse(verifyLogMessage(10000, "batch-1.0", errFile)); // not present on server yet

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
        assertTrue(verifyLogMessage(10000, RUNNING_GENERATE_FEATURES, ++generateFeaturesCount));
        assertTrue(verifyFileExists(newFeatureFile, 5000)); // task created file
        assertTrue(verifyFileExists(newTargetFeatureFile, 5000)); // dev mode copied file
        assertTrue(verifyLogMessage(10000, "batch-1.0", newFeatureFile));
        assertTrue(verifyLogMessage(10000, NEW_FILE_INFO_MESSAGE, newFeatureFile));
        assertTrue(verifyLogMessage(10000, SERVER_XML_COMMENT, serverXmlFile));
        // should appear as part of the message "CWWKF0012I: The server installed the following features:"
        assertTrue(verifyLogMessage(10000, "batch-1.0", errFile));

        // When there is a compilation error the generate features process should not run
        final String goodCode = "import javax.ws.rs.GET;";
        final String badCode  = "import javax.ws.rs.GET";
        final String errMsg = "Source compilation had errors.";
        int errCount = countOccurrences(errMsg, logFile);
        replaceString(goodCode, badCode, helloBatchSrc);

        assertTrue(verifyLogMessage(10000, errMsg, errCount+1)); // wait for compilation
        int updatedgenFeaturesCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
        assertEquals(generateFeaturesCount, updatedgenFeaturesCount);

        // Need valid code for testing
        String goodCompile = "Source compilation was successful.";
        int goodCount = countOccurrences(goodCompile, logFile);
        replaceString(badCode, goodCode, helloBatchSrc);
        assertTrue(verifyLogMessage(10000, goodCompile, goodCount+1));

        final String autoGenOff = "Setting automatic generation of features to: [ Off ]";
        final String autoGenOn  = "Setting automatic generation of features to: [ On ]";
        // toggle off
        writer.write("g\n");
        writer.flush();
        assertTrue(autoGenOff, verifyLogMessage(10000, autoGenOff));
        // toggle on
        writer.write("g\n");
        writer.flush();
        assertTrue(autoGenOn, verifyLogMessage(20000, autoGenOn)); // allow time to run scanner

        // Remove a class and use 'optimize' to rebuild the generated features
        assertTrue(helloBatchSrc.delete());
        assertTrue(verifyFileDoesNotExist(helloBatchSrc, 15000));
        assertTrue(verifyFileDoesNotExist(helloBatchObj, 15000));
        assertTrue(verifyLogMessage(10000, "HelloBatch.class was deleted"));
        Thread.sleep(500); // let dev mode and the server finish
        // Just removing the class file does not remove the feature because the feature
        // list is built in an incremental way.
        assertTrue(verifyLogMessage(100, "batch-1.0", newFeatureFile, 1));
        writer.write("o\n");
        writer.flush();
        assertTrue(verifyLogMessage(10000, "batch-1.0", newFeatureFile, 0)); // exist 0 times
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
