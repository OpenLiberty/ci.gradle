/**
 * (C) Copyright IBM Corporation 2018, 2025.
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
package io.openliberty.tools.gradle.extensions

class SpringBootExtension {
    // The path of the source application file to thin.
    String sourceAppPath

    // The directory path of the parent read-only library cache. The parent library cache is searched first to locate existing libraries. 
    // If the library is not found, the library is stored in the writable library cache that is specified by the `targetLibCachePath` option. 
    // If this option is not specified, no parent library cache is searched.
    String parentLibCachePath

    // The directory path that is used to save the library cache. If this option is not specified, a `lib.index.cache` directory is created in the parent directory of the source application.
    String targetLibCachePath

    // The path that is used to save the thin application file. If this option is not specified, a new file is created with the `.spring` extension in the parent directory of the source application.
    String targetThinAppPath
}
