/*
 * (C) Copyright IBM Corporation 2025.
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

import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Utility class for process related operations and resource management
 */
class ProcessUtils {

    static BufferedReader createProcessReader(Process process) {
        return new BufferedReader(new InputStreamReader(process.getInputStream()))
    }

    static BufferedReader createProcessErrorReader(Process process) {
        return new BufferedReader(new InputStreamReader(process.getErrorStream()))
    }

    static void drainAndCloseProcessStream(Process process, boolean isErrorStream, Logger logger) {
        if (process == null) return
        
        try (BufferedReader reader = isErrorStream ? createProcessErrorReader(process) : createProcessReader(process)) {
            // Drain the stream
            while (reader.readLine() != null) {}
        } catch (IOException e) {
            logger.debug("Error draining process stream: " + e.getMessage())
        }
    }
}
