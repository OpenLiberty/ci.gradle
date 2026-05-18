package io.openliberty.tools.gradle;

import static org.junit.Assert.*;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test early quit functionality during dev mode startup for issue #1638.
 * This test uses startDevModeWithoutWaiting() to test pressing 'q' during startup.
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

    @AfterClass
    public static void tearDown() throws Exception {
        // Clean up process if still running
        if (process != null && process.isAlive()) {
            process.destroy();
            process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
        }
        
        // Clean up build directory
        if (buildDir != null && buildDir.exists()) {
            FileUtils.deleteQuietly(buildDir);
        }
    }

    @Test
    public void testEarlyQuitDuringStartup() throws Exception {
        // Start dev mode without waiting for startup
        startDevModeWithoutWaiting(buildDir);
        
        // Wait a short time for dev mode to start initializing (but not complete)
        Thread.sleep(3000);
        
        // Send 'q' to quit DURING startup
        System.out.println("Sending 'q' to quit during startup...");
        writer.write("q");
        writer.newLine();
        writer.flush();
        
        // Wait for process to terminate
        boolean terminated = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue("Process should have terminated after 'q' during startup", terminated);
        
        // Verify process is no longer alive
        assertFalse("Process should not be alive after early quit", process.isAlive());
        
        // Give time for logs to be written
        Thread.sleep(2000);
        
        // Check that early quit was detected in logs
        String logContent = logFile.exists() ? logFile.text : "";
        String errContent = errFile.exists() ? errFile.text : "";
        
        // Verify early quit was detected - look for the exception message or debug messages
        boolean foundEarlyQuit = logContent.contains("Server startup aborted by user") ||
                                 errContent.contains("Server startup aborted by user") ||
                                 logContent.contains("Early quit detected") ||
                                 errContent.contains("Early quit detected");
        
        assertTrue("Early quit should be detected in logs: " + foundEarlyQuit, foundEarlyQuit);
        
        System.out.println("Early quit during startup test passed");
    }

    @Test
    public void testOtherHotkeysIgnoredDuringStartup() throws Exception {
        // Start dev mode without waiting for startup
        startDevModeWithoutWaiting(buildDir);
        
        // Wait a short time for dev mode to start initializing (but not complete)
        Thread.sleep(2000);
        
        // Try various hotkeys that should be ignored during startup (excluding 'q' and 'h')
        String[] ignoredKeys = ["r", "g", "o", "t", "p", "\n"];
        
        System.out.println("Sending hotkeys that should be ignored during startup...");
        for (String key : ignoredKeys) {
            writer.write(key);
            if (!key.equals("\n")) {
                writer.newLine();
            }
            writer.flush();
            Thread.sleep(200); // Small delay between commands
        }
        
        // Wait a bit to see if any of those commands triggered actions
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
        
        boolean terminated = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue("Process should have terminated after 'q' command", terminated);
        
        System.out.println("Other hotkeys ignored during startup test passed");
    }

    @Test
    public void testHelpCommandDuringStartup() throws Exception {
        // Start dev mode without waiting for startup
        startDevModeWithoutWaiting(buildDir);
        
        // Wait a short time for dev mode to start initializing (but not complete)
        Thread.sleep(2000);
        
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
        
        // Give more time for graceful shutdown
        boolean terminated = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue("Process should have terminated after 'q' command", terminated);
        
        System.out.println("Help command during startup test passed");
    }
}