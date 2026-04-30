package io.openliberty.tools.gradle;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.FileWriter;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class DevNoDuplicateCompilationTest extends BaseDevTest {
    static final String projectName = "basic-dev-project";

    static File resourceDir = new File("build/resources/test/dev-test/" + projectName);
    static File buildDir = new File(integTestDir, "dev-test/" + projectName + System.currentTimeMillis());

    @BeforeClass
    public static void setup() throws IOException, InterruptedException, FileNotFoundException {
        createDir(buildDir);
        createTestProject(buildDir, resourceDir, buildFilename);
        runDevMode(buildDir);
    }

    @Test
    public void testNoDuplicateCompilation() throws Exception {
        assertTrue(verifyLogMessage(10000, "Liberty is running in dev mode."));
        assertTrue(verifyLogMessage(20000, WEB_APP_AVAILABLE, errFile));

        File srcHelloWorld = new File(buildDir, "src/main/java/com/demo/HelloWorld.java");
        File targetHelloWorld = new File(targetDir, "classes/java/main/com/demo/HelloWorld.class");
        assertTrue(srcHelloWorld.exists());
        assertTrue(targetHelloWorld.exists());

        // Count initial compilation messages
        int initialCompilationCount = countOccurrences(COMPILATION_SUCCESSFUL, logFile);
        int initialHotReloadCount = countOccurrences(SERVER_CONFIG_SUCCESS, errFile);

        waitLongEnough();
        long lastModified = targetHelloWorld.lastModified();

        String modification = "// Test modification for duplicate compilation check";
        BufferedWriter javaWriter = null;
        try {
            javaWriter = new BufferedWriter(new FileWriter(srcHelloWorld, true));
            javaWriter.append('\n');
            javaWriter.append(modification);
        } finally {
            if (javaWriter != null) {
                javaWriter.close();
            }
        }

        assertTrue("Class file was not recompiled", waitForCompilation(targetHelloWorld, lastModified, 10000));
        assertTrue("Source compilation message not found", 
            verifyLogMessage(10000, COMPILATION_SUCCESSFUL, ++initialCompilationCount));
        assertTrue("Liberty hot reload message (CWWKZ0003I) not found",
            verifyLogMessage(10000, SERVER_CONFIG_SUCCESS, errFile, ++initialHotReloadCount));

        Thread.sleep(3000);

        // Count final compilation messages
        int finalCompilationCount = countOccurrences(COMPILATION_SUCCESSFUL, logFile);
        int finalHotReloadCount = countOccurrences(SERVER_CONFIG_SUCCESS, errFile);

        assertEquals("Duplicate compilation detected - compilation happened more than once",
            initialCompilationCount, finalCompilationCount);
        assertEquals("Multiple hot reloads detected - should only reload once per change",
            initialHotReloadCount, finalHotReloadCount);
    }

    @Test
    public void testMultipleSequentialChanges() throws Exception {
        assertTrue(verifyLogMessage(10000, "Liberty is running in dev mode."));
        assertTrue(verifyLogMessage(20000, WEB_APP_AVAILABLE, errFile));

        File srcHelloWorld = new File(buildDir, "src/main/java/com/demo/HelloWorld.java");
        File targetHelloWorld = new File(targetDir, "classes/java/main/com/demo/HelloWorld.class");
        assertTrue(srcHelloWorld.exists());
        assertTrue(targetHelloWorld.exists());

        for (int i = 1; i <= 3; i++) {
            int compilationCountBefore = countOccurrences(COMPILATION_SUCCESSFUL, logFile);
            int hotReloadCountBefore = countOccurrences(SERVER_CONFIG_SUCCESS, errFile);

            waitLongEnough();
            long lastModified = targetHelloWorld.lastModified();

            String modification = "// Test modification #" + i + " for duplicate compilation check";
            BufferedWriter javaWriter = null;
            try {
                javaWriter = new BufferedWriter(new FileWriter(srcHelloWorld, true));
                javaWriter.append('\n');
                javaWriter.append(modification);
            } finally {
                if (javaWriter != null) {
                    javaWriter.close();
                }
            }

            assertTrue("Class file was not recompiled for change #" + i,
                waitForCompilation(targetHelloWorld, lastModified, 10000));
            assertTrue("Source compilation message not found for change #" + i,
                verifyLogMessage(10000, COMPILATION_SUCCESSFUL, ++compilationCountBefore));
            assertTrue("Liberty hot reload message (CWWKZ0003I) not found for change #" + i,
                verifyLogMessage(10000, SERVER_CONFIG_SUCCESS, errFile, ++hotReloadCountBefore));

            Thread.sleep(3000);

            // Count compilation messages after change
            int compilationCountAfter = countOccurrences(COMPILATION_SUCCESSFUL, logFile);
            int hotReloadCountAfter = countOccurrences(SERVER_CONFIG_SUCCESS, errFile);

            assertEquals("Duplicate compilation detected for change #" + i,
                compilationCountBefore, compilationCountAfter);
            assertEquals("Multiple hot reloads detected for change #" + i,
                hotReloadCountBefore, hotReloadCountAfter);
        }
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
