/**
 * (C) Copyright IBM Corporation 2014, 2025.
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
package io.openliberty.tools.gradle.utils

import org.gradle.api.logging.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import io.openliberty.tools.common.plugins.util.OSUtil
import io.openliberty.tools.gradle.utils.ProcessUtils

/**
 * Utility class for Liberty server operations
 */
class ServerUtils {

    /**
     * Verifies that the server is fully stopped and all resources are released.
     * 
     * @param serverDir The server directory
     * @param logger The logger to use for output
     * @return true if the server is fully stopped, false otherwise
     */
    static boolean verifyServerFullyStopped(File serverDir, Logger logger) {
        logger.debug('Verifying Liberty server is fully stopped and resources are released...')
        
        // Define verification parameters
        int maxAttempts = 5
        long initialWaitMs = 500
        long maxWaitMs = 4000
        long waitMs = initialWaitMs
        long totalWaitTime = 0
        
        // Check for server process
        File workarea = new File(serverDir, "workarea")
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            boolean serverRunning = isServerRunning(serverDir, logger)
            boolean resourcesLocked = areResourcesLocked(workarea, logger)
            
            if (!serverRunning && !resourcesLocked) {
                logger.debug("Server verified as fully stopped after ${totalWaitTime}ms")
                return true
            }
            
            if (serverRunning) {
                logger.debug("Server process still running (attempt ${attempt}/${maxAttempts}), waiting ${waitMs}ms...")
            } else if (resourcesLocked) {
                logger.debug("Server resources still locked (attempt ${attempt}/${maxAttempts}), waiting ${waitMs}ms...")
            }
            
            try {
                Thread.sleep(waitMs)
                totalWaitTime += waitMs
                // Exponential backoff with cap
                waitMs = Math.min(waitMs * 2, maxWaitMs)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt()
                logger.warn("Interrupted while waiting for server to stop")
                return false
            }
        }
        
        // If we get here, we've exceeded max attempts
        logger.warn("Server stop verification timed out after ${maxAttempts} attempts (${totalWaitTime}ms)")
        logger.warn("Some resources may still be locked, which could cause issues with subsequent tasks")
        
        // Try to identify locked resources for debugging
        identifyLockedResources(workarea, logger)
        
        return false
    }
    
    /**
     * Checks if the Liberty server process is still running.
     * 
     * @param serverDir The server directory
     * @param logger The logger to use for output
     * @return true if the server is running, false otherwise
     */
    static boolean isServerRunning(File serverDir, Logger logger) {
        // Check for server process using OS-specific commands
        String serverName = serverDir.getName()
        boolean isRunning = false
        Process process = null
        BufferedReader reader = null
        
        try {
            if (OSUtil.isWindows()) {
                String command = "tasklist /FI \"IMAGENAME eq java.exe\" /FO CSV"
                process = Runtime.getRuntime().exec(command)
                reader = ProcessUtils.createProcessReader(process)
                String line
                while ((line = reader.readLine()) != null) {
                    if (line.toLowerCase().contains(serverName.toLowerCase())) {
                        isRunning = true
                        break
                    }
                }
                process.waitFor(5, TimeUnit.SECONDS)
            } else {
                // Unix-based systems (Linux, macOS)
                String command = "ps -ef | grep " + serverName + " | grep -v grep"
                process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command})
                reader = ProcessUtils.createProcessReader(process)
                isRunning = reader.readLine() != null
                process.waitFor(5, TimeUnit.SECONDS)
            }
        } catch (Exception e) {
            logger.debug("Error checking if server is running: " + e.getMessage())
        } finally {
            ProcessUtils.closeQuietly(reader, logger, "reader")
            if (process != null) {
                ProcessUtils.drainAndCloseProcessStream(process, true, logger)
            }
        }
        
        return isRunning
    }
    
    /**
     * Checks if resources in the workarea directory are locked.
     * 
     * @param workarea The workarea directory
     * @param logger The logger to use for output
     * @return true if resources are locked, false otherwise
     */
    static boolean areResourcesLocked(File workarea, Logger logger) {
        if (!workarea.exists()) {
            return false
        }
        
        // Try to access potentially locked files
        try {
            // Check if we can delete and recreate a test file in the workarea
            File testFile = new File(workarea, ".liberty_lock_test")
            if (testFile.exists()) {
                if (!testFile.delete()) {
                    return true // Can't delete, likely locked
                }
            }
            
            // Try to create the test file
            if (!testFile.createNewFile()) {
                return true // Can't create, likely locked
            }
            
            // Clean up
            testFile.delete()
            
            // Check if we can access the OSGi directories which are commonly locked
            File osgiDir = new File(workarea, "org.eclipse.osgi")
            if (osgiDir.exists()) {
                File[] osgiFiles = osgiDir.listFiles()
                if (osgiFiles != null) {
                    for (File file : osgiFiles) {
                        if (!file.canWrite()) {
                            return true // Can't write, likely locked
                        }
                    }
                }
            }
            
            return false // No locks detected
        } catch (Exception e) {
            logger.debug("Error checking for locked resources: " + e.getMessage())
            return true // Assume locked if we encounter an error
        }
    }
    
    /**
     * Attempts to identify which resources are locked in the workarea.
     * This is primarily for debugging purposes.
     * 
     * @param workarea The workarea directory
     * @param logger The logger to use for output
     */
    static void identifyLockedResources(File workarea, Logger logger) {
        if (!workarea.exists()) {
            return
        }
        
        logger.debug("Attempting to identify locked resources:")
        
        // Check common problematic directories
        List<String> problematicPaths = [
            "org.eclipse.osgi",
            "com.ibm.ws.runtime.update",
            "com.ibm.ws.kernel.boot"
        ]
        
        for (String path : problematicPaths) {
            File dir = new File(workarea, path)
            if (dir.exists()) {
                checkDirectoryAccess(dir, 0, logger)
            }
        }
    }
    
    /**
     * Recursively checks directory access to identify locked files.
     * 
     * @param dir The directory to check
     * @param depth Current recursion depth (to limit deep recursion)
     * @param logger The logger to use for output
     */
    static void checkDirectoryAccess(File dir, int depth, Logger logger) {
        if (depth > 3) {
            return // Limit recursion depth
        }
        
        File[] files = dir.listFiles()
        if (files == null) {
            logger.debug("  - Cannot list files in: ${dir.getAbsolutePath()} (likely locked)")
            return
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                checkDirectoryAccess(file, depth + 1, logger)
            } else {
                if (!file.canWrite()) {
                    logger.debug("  - Locked file detected: ${file.getAbsolutePath()}")
                }
            }
        }
    }
    
    /**
     * Force cleanup of server resources when normal stop verification fails.
     * 
     * @param serverDir The server directory
     * @param logger The logger to use for output
     */
    static void forceCleanupServerResources(File serverDir, Logger logger) {
        logger.lifecycle("Performing forced cleanup of Liberty server resources...")
        
        // 1. Force kill any lingering server processes
        forceKillServerProcesses(serverDir.getName(), logger)
        
        // 2. Force release of file locks by using JVM's System.gc()
        logger.debug("Requesting garbage collection to help release file locks...")
        System.gc()
        System.runFinalization()
        
        // 3. Wait a bit more to allow OS to release resources
        try {
            Thread.sleep(2000)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt()
        }
        
        // 4. Create marker file to indicate server was force-stopped
        try {
            File marker = new File(serverDir, ".force_stopped")
            marker.createNewFile()
        } catch (Exception e) {
            logger.debug("Could not create force-stopped marker: " + e.getMessage())
        }
    }
    
    /**
     * Force kill any lingering server processes.
     * 
     * @param serverName The name of the server
     * @param logger The logger to use for output
     */
    static void forceKillServerProcesses(String serverName, Logger logger) {
        logger.lifecycle("Force killing any lingering Liberty server processes...")
        
        Process findProcess = null
        BufferedReader reader = null
        
        try {
            if (OSUtil.isWindows()) {
                // Windows - use taskkill with /F (force) flag
                String findCmd = "tasklist /FI \"IMAGENAME eq java.exe\" /FO CSV"
                findProcess = Runtime.getRuntime().exec(findCmd)
                reader = ProcessUtils.createProcessReader(findProcess)
                String line
                List<String> pidsToKill = new ArrayList<>()
                
                while ((line = reader.readLine()) != null) {
                    if (line.toLowerCase().contains(serverName.toLowerCase())) {
                        // Extract PID from CSV format
                        String[] parts = line.split(",")
                        if (parts.length >= 2) {
                            String pid = parts[1].replaceAll("\"", "").trim()
                            pidsToKill.add(pid)
                        }
                    }
                }
                
                // Kill each process
                for (String pid : pidsToKill) {
                    logger.debug("Killing process with PID: ${pid}")
                    Process killProcess = null
                    try {
                        killProcess = Runtime.getRuntime().exec("taskkill /F /PID " + pid)
                        killProcess.waitFor(5, TimeUnit.SECONDS)
                    } finally {
                        // Drain and close both streams
                        ProcessUtils.drainAndCloseProcessStream(killProcess, false, logger)
                        ProcessUtils.drainAndCloseProcessStream(killProcess, true, logger)
                    }
                }
                
            } else {
                // Unix-based systems (Linux, macOS)
                String findCmd = "ps -ef | grep " + serverName + " | grep -v grep"
                findProcess = Runtime.getRuntime().exec(new String[]{"sh", "-c", findCmd})
                reader = ProcessUtils.createProcessReader(findProcess)
                String line
                List<String> pidsToKill = new ArrayList<>()
                
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split("\\s+")
                    if (parts.length >= 2) {
                        pidsToKill.add(parts[1])
                    }
                }
                
                // Kill each process
                for (String pid : pidsToKill) {
                    logger.debug("Killing process with PID: ${pid}")
                    Process killProcess = null
                    try {
                        killProcess = Runtime.getRuntime().exec(new String[]{"sh", "-c", "kill -9 " + pid})
                        killProcess.waitFor(5, TimeUnit.SECONDS)
                    } finally {
                        // Drain and close both streams
                        ProcessUtils.drainAndCloseProcessStream(killProcess, false, logger)
                        ProcessUtils.drainAndCloseProcessStream(killProcess, true, logger)
                    }
                }
            }
            
            // Wait a bit for processes to be killed
            Thread.sleep(1000)
            
        } catch (Exception e) {
            logger.warn("Error during force kill: " + e.getMessage())
        } finally {
            ProcessUtils.closeQuietly(reader, logger, "reader")
            if (findProcess != null) {
                ProcessUtils.drainAndCloseProcessStream(findProcess, true, logger)
            }
        }
    }
}
