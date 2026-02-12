/**
 * (C) Copyright IBM Corporation 2019, 2025.
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

class PackageExtension {
    
    // Controls the package contents. Can be used with values `all`, `usr`, `minify`, `wlp`, `runnable`, `all,runnable` and `minify,runnable`. 
    // The default value is `all`. The `runnable`, `all,runnable` and `minify,runnable` values are supported beginning with 8.5.5.9 and works with `jar` type packages only. 
    String include

    // Directory of the packaged file. The default value is `${project.getLayout().getBuildDirectory().getAsFile().get()}/libs`. 
    // If the directory is not absolute, it is created in `${project.getLayout().getBuildDirectory().getAsFile().get()}/libs`.
    String packageDirectory

    // Name of the packaged file. The default value is `${project.name}`. 
    String packageName

    // Type of package. Can be used with values `zip`, `jar`, `tar`, or `tar.gz`. Defaults to `jar` if `runnable` is specified for the `include` property. 
    // Otherwise the default value is `zip`.
    String packageType

    // Specifies the root server folder name in the packaged file.
    String serverRoot

    // A comma-delimited list of operating systems that you want the packaged server to support. 
    // To specify that an operating system is not to be supported, prefix it with a minus sign ("-"). 
    // The 'include' attribute __must__ be set to `minify`.
    String os
}
