package io.openliberty.tools.gradle

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test early quit functionality during dev mode startup for issue #1638.
 * These tests verify that users can press 'q' to quit during server startup,
 * and that other hotkeys are properly restricted during startup.
 */
class DevEarlyQuitTest extends BaseDevTest {
    static final String projectName = "basic-dev-project";

    static File resourceDir = new File("build/resources/test/dev-test/" + projectName);
    static File buildDir = new File(integTestDir, "dev-early-quit-test/" + projectName + System.currentTimeMillis());

    @BeforeClass
    public static void setup() throws IOException, InterruptedException, FileNotFoundException {
        createDir(buildDir);
        createTestProject(buildDir, resourceDir, buildFilename);
    }

    @After
    public void cleanupAfterEachTest() throws Exception {
        // Ensure cleanup after each test
        if (process != null && process.isAlive()) {
            process.destroy();
            process.waitFor(10, TimeUnit.SECONDS);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Clean up process if still running
        if (process != null && process.isAlive()) {
            process.destroy();
            process.waitFor(30, TimeUnit.SECONDS);
        }
        
        // Clean up build directory
        if (buildDir != null && buildDir.exists()) {
            FileUtils.deleteQuietly(buildDir);
        }
    }

    /**
     * Helper method to wait for log file to be created and process to be ready
     */
    private static boolean waitForDevModeToInitialize(int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long timeout = timeoutSeconds * 1000;
        
        // Wait for log file to exist and have some content
        while (System.currentTimeMillis() - startTime < timeout) {
            if (logFile.exists() && logFile.length() > 0 && process.isAlive()) {
                // Give it a bit more time to ensure keyboard listener is ready
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @Test
    public void testEarlyQuitDuringStartup() throws Exception {
        // Start dev mode without waiting for full startup
        startDevModeWithoutWaiting(buildDir);
        
        // Wait for dev mode to initialize (log file created, process alive)
        boolean devModeStarted = waitForDevModeToInitialize(30);
        assertTrue("Dev mode should have started", devModeStarted);
        
        // Verify process is still running
        assertTrue("Process should be alive", process.isAlive());
        
        // Send 'q' to quit DURING startup (before server fully starts)
        System.out.println("Sending 'q' to quit during startup...");
        writer.write("q");
        writer.newLine();
        writer.flush();
        
        // Wait for process to terminate
        boolean terminated = process.waitFor(90, TimeUnit.SECONDS);
        assertTrue("Process should have terminated after 'q' during startup", terminated);
        
        // Verify process is no longer alive
        assertFalse("Process should not be alive after early quit", process.isAlive());
        
        System.out.println("Early quit during startup test passed");
    }

    @Test
    public void testOtherHotkeysIgnoredDuringStartup() throws Exception {
        // Start dev mode without waiting for full startup
        startDevModeWithoutWaiting(buildDir);
        
        // Wait for dev mode to initialize
        boolean devModeStarted = waitForDevModeToInitialize(30);
        assertTrue("Dev mode should have started", devModeStarted);
        
        // Verify process is running
        assertTrue("Process should be alive", process.isAlive());
        
        // Try various hotkeys that should be ignored during startup (excluding 'q' and 'h')
        String[] ignoredKeys = ["r", "g", "o", "t", "p", "\n"];
        
        System.out.println("Sending hotkeys that should be ignored during startup...");
        for (String key : ignoredKeys) {
            writer.write(key);
            if (!key.equals("\n")) {
                writer.newLine();
            }
            writer.flush();
            Thread.sleep(300);
        }
        
        // Wait a bit for messages to be logged
        Thread.sleep(2000);
        
        // Process should still be alive (none of the commands should have quit)
        assertTrue("Process should still be alive after ignored hotkeys", process.isAlive());
        
        // Check logs for the "command not available" message
        String logContent = logFile.exists() ? logFile.text : "";
        assertTrue("Should have message about command not being available during startup",
                logContent.contains("The requested command is not available during server startup"));
        
        // Verify that restart (r) didn't happen
        int restartCount = logContent.split("Restarting").length - 1;
        assertEquals("Should not have restarted during startup", 0, restartCount);
        
        System.out.println("Verified that other hotkeys are ignored during startup");
        
        // Now send 'q' to properly quit
        writer.write("q");
        writer.newLine();
        writer.flush();
        
        boolean terminated = process.waitFor(90, TimeUnit.SECONDS);
        assertTrue("Process should have terminated after 'q' command", terminated);
        
        System.out.println("Other hotkeys ignored during startup test passed");
    }

    @Test
    public void testHelpCommandDuringStartup() throws Exception {
        // Start dev mode without waiting for full startup
        startDevModeWithoutWaiting(buildDir);
        
        // Wait for dev mode to initialize
        boolean devModeStarted = waitForDevModeToInitialize(30);
        assertTrue("Dev mode should have started", devModeStarted);
        
        // Verify process is running
        assertTrue("Process should be alive", process.isAlive());
        
        // Send 'h' command to show help
        System.out.println("Sending 'h' to show help during startup...");
        writer.write("h");
        writer.newLine();
        writer.flush();
        
        // Wait a bit for help to be displayed
        Thread.sleep(2000);
        
        // Process should still be alive (help shouldn't quit)
        assertTrue("Process should still be alive after 'h' command", process.isAlive());
        
        System.out.println("Verified that 'h' command works during startup");
        
        // Now send 'q' to properly quit
        writer.write("q");
        writer.newLine();
        writer.flush();
        
        // Give time for graceful shutdown
        boolean terminated = process.waitFor(90, TimeUnit.SECONDS);
        assertTrue("Process should have terminated after 'q' command", terminated);
        
        System.out.println("Help command during startup test passed");
    }
}