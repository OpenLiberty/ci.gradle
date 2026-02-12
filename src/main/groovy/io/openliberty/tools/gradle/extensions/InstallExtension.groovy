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
package io.openliberty.tools.gradle.extensions

class InstallExtension {
    
    // WebSphere Liberty server license code. See [installLiberty task](installLiberty.md#installliberty-task).
    String licenseCode

    // Exact or wildcard version of the WebSphere Liberty server to install. 
    // Available versions are listed in the [index.yml](http://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/index.yml) file. 
    // Only used if `runtimeUrl` is not set and `libertyRuntime` is not configured. By default, the latest stable release is used. 
    String version

    // URL to the WebSphere Liberty server's `.jar` or a `.zip` file on your repository or on the [Liberty repository](https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/). 
    // `runtimeUrl` can also point to an [Open Liberty](https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/) `.zip`. 
    // If not set and `libertyRuntime` is not configured, the Liberty repository will be used to find the Liberty runtime archive. 
    String runtimeUrl

    // Username needed for basic authentication. 
    String username

    // Password needed for basic authentication.
    String password

    // Maximum time in seconds the download can take. The default value is `0` (no maximum time).
    String maxDownloadTime

    // Liberty runtime type to download from the Liberty repository. 
    // Currently, the following types are supported: `kernel`, `webProfile6`, `webProfile7`, `webProfile8`, `javaee7`, and `javaee8`. 
    // Only used if `runtimeUrl` is not set and `libertyRuntime` is not configured. 
    // The default value is `webProfile7` if `useOpenLiberty` is `false`. 
    // If using Open Liberty and no type is specified, the default Open Liberty runtime is used.
    String type

    // Specifies whether to install Open Liberty or WebSphere Liberty runtime when `runtimeUrl` is not specified and `libertyRuntime` is not configured. 
    // The default value is `true`.
    String useOpenLiberty
}
