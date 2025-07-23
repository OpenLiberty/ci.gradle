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
package io.openliberty.tools.gradle.tasks
import org.gradle.api.logging.LogLevel

import org.gradle.api.tasks.TaskAction
import io.openliberty.tools.ant.ServerTask
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import io.openliberty.tools.common.plugins.util.OSUtil

class StopTask extends AbstractServerTask {

    StopTask() {
        configure({
            description = 'Stops the Liberty server.'
            group = 'Liberty'
        })
    }

    @TaskAction
    void stop() {
        if (isLibertyInstalledAndValid(project)) {
            File serverDir = getServerDir(project)
            if (serverDir.exists()) {
                File serverXmlFile = new File(serverDir,"server.xml")
                boolean defaultServerTemplateUsed = copyDefaultServerTemplate(getInstallDir(project),serverDir)
                if (serverXmlFile.exists()) {
                    ServerTask serverTaskStop = createServerTask(project, "stop");
                    serverTaskStop.setUseEmbeddedServer(server.embedded)
                    serverTaskStop.execute()
                    
                    // Verify server is fully stopped and resources are released
                    if (!verifyServerFullyStopped(serverDir)) {
                        // If normal stop verification fails, try forced cleanup
                        forceCleanupServerResources(serverDir)
                    }
                } else {
        	        logger.error ('The server cannot be stopped. There is no server.xml file in the server.')
                }

                if (defaultServerTemplateUsed) {
        	        logger.warn ('The server.xml file was missing in the server during the stop task. Copied the defaultServer template server.xml file into the server temporarily so the stop task could be completed.')
                    if (!serverXmlFile.delete()) {
                        logger.error('Could not delete the server.xml file copied from the defaultServer template after stopping the server.')
                    }
                }
            } else {
        	    logger.error ('There is no server to stop. The server has not been created.')
            }
        } else {
            logger.error ('There is no server to stop. The runtime has not been installed.')
        }
    }
    
    /**
     * Verifies that the server is fully stopped and all resources are released.
     * This method will wait until the server is confirmed to be fully stopped or until a timeout is reached.
     * 
     * @param serverDir The server directory
     * @return true if the server is fully stopped, false otherwise
     */
    private boolean verifyServerFullyStopped(File serverDir) {
        logger.lifecycle('Verifying Liberty server is fully stopped and resources are released...')
        
        // Define verification parameters
        int maxAttempts = 10
        long initialWaitMs = 500
        long maxWaitMs = 5000
        long waitMs = initialWaitMs
        long totalWaitTime = 0
        
        // Check for server process
        File workarea = new File(serverDir, "workarea")
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            boolean serverRunning = isServerRunning(serverDir)
            boolean resourcesLocked = areResourcesLocked(workarea)
            
            if (!serverRunning && !resourcesLocked) {
                logger.lifecycle("Server verified as fully stopped after ${totalWaitTime}ms")
                return true
            }
            
            if (serverRunning) {
                logger.lifecycle("Server process still running (attempt ${attempt}/${maxAttempts}), waiting ${waitMs}ms...")
            } else if (resourcesLocked) {
                logger.lifecycle("Server resources still locked (attempt ${attempt}/${maxAttempts}), waiting ${waitMs}ms...")
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
        identifyLockedResources(workarea)
        
        return false
    }
    
    /**
     * Checks if the Liberty server process is still running.
     * 
     * @param serverDir The server directory
     * @return true if the server is running, false otherwise
     */
    private boolean isServerRunning(File serverDir) {
        // Check for server process using OS-specific commands
        String serverName = serverDir.getName()
        boolean isRunning = false
        
        try {
            if (OSUtil.isWindows()) {
                String command = "tasklist /FI \"IMAGENAME eq java.exe\" /FO CSV"
                Process process = Runtime.getRuntime().exec(command)
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))
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
                Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command})
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))
                isRunning = reader.readLine() != null
                process.waitFor(5, TimeUnit.SECONDS)
            }
        } catch (Exception e) {
            logger.debug("Error checking if server is running: " + e.getMessage())
        }
        
        return isRunning
    }
    
    /**
     * Checks if resources in the workarea directory are locked.
     * 
     * @param workarea The workarea directory
     * @return true if resources are locked, false otherwise
     */
    private boolean areResourcesLocked(File workarea) {
        if (!workarea.exists()) {
            return false
        }
        
        // Try to access potentially locked files
        try {
            // Check if we can delete and recreate a test file in the workarea
            File testFile = new File(workarea, ".lock_test")
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
     */
    private void identifyLockedResources(File workarea) {
        if (!workarea.exists()) {
            return
        }
        
        logger.lifecycle("Attempting to identify locked resources:")
        
        // Check common problematic directories
        List<String> problematicPaths = [
            "org.eclipse.osgi",
            "com.ibm.ws.runtime.update",
            "com.ibm.ws.kernel.boot"
        ]
        
        for (String path : problematicPaths) {
            File dir = new File(workarea, path)
            if (dir.exists()) {
                checkDirectoryAccess(dir, 0)
            }
        }
    }
    
    /**
     * Recursively checks directory access to identify locked files.
     * 
     * @param dir The directory to check
     * @param depth Current recursion depth (to limit deep recursion)
     */
    private void checkDirectoryAccess(File dir, int depth) {
        if (depth > 3) {
            return // Limit recursion depth
        }
        
        File[] files = dir.listFiles()
        if (files == null) {
            logger.lifecycle("  - Cannot list files in: ${dir.getAbsolutePath()} (likely locked)")
            return
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                checkDirectoryAccess(file, depth + 1)
            } else {
                if (!file.canWrite()) {
                    logger.lifecycle("  - Locked file detected: ${file.getAbsolutePath()}")
                }
            }
        }
    }
    
    /**
     * Force cleanup of server resources when normal stop verification fails.
     * This is a more aggressive approach to ensure resources are released.
     * 
     * @param serverDir The server directory
     */
    private void forceCleanupServerResources(File serverDir) {
        logger.lifecycle("Performing forced cleanup of Liberty server resources...")
        
        // 1. Force kill any lingering server processes
        forceKillServerProcesses(serverDir.getName())
        
        // 2. Force release of file locks by using JVM's System.gc()
        logger.lifecycle("Requesting garbage collection to help release file locks...")
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
     */
    private void forceKillServerProcesses(String serverName) {
        logger.lifecycle("Force killing any lingering Liberty server processes...")
        
        try {
            if (OSUtil.isWindows()) {
                // Windows - use taskkill with /F (force) flag
                String findCmd = "tasklist /FI \"IMAGENAME eq java.exe\" /FO CSV"
                Process findProcess = Runtime.getRuntime().exec(findCmd)
                BufferedReader reader = new BufferedReader(new InputStreamReader(findProcess.getInputStream()))
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
                    logger.lifecycle("Killing process with PID: ${pid}")
                    Runtime.getRuntime().exec("taskkill /F /PID " + pid)
                }
                
            } else {
                // Unix-based systems (Linux, macOS)
                String findCmd = "ps -ef | grep " + serverName + " | grep -v grep"
                Process findProcess = Runtime.getRuntime().exec(new String[]{"sh", "-c", findCmd})
                BufferedReader reader = new BufferedReader(new InputStreamReader(findProcess.getInputStream()))
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
                    logger.lifecycle("Killing process with PID: ${pid}")
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", "kill -9 " + pid})
                }
            }
            
            // Wait a bit for processes to be killed
            Thread.sleep(1000)
            
        } catch (Exception e) {
            logger.warn("Error during force kill: " + e.getMessage())
        }
    }
}
