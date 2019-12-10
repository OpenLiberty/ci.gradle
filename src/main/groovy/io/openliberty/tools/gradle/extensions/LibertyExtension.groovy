/**
 * (C) Copyright IBM Corporation 2014, 2019.
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

import org.gradle.util.ConfigureUtil
import org.gradle.api.NamedDomainObjectContainer

class LibertyExtension {

    String installDir
    String outputDir
    String userDir

    String baseDir
    String cacheDir

    // For overriding the group, name or version of the libertyRuntime installed from Maven Central repository.
    // Default is group 'io.openliberty', name 'openliberty-kernel' and version '[19.0.0.9,)' which gets the latest version.
    Properties runtime = new Properties()

    CompileJSPExtension jsp = new CompileJSPExtension()

    InstallExtension install = new InstallExtension()
    SpringBootExtension thin = new SpringBootExtension()

    ServerExtension server = server = new ServerExtension()

    def jsp(Closure closure) {
        ConfigureUtil.configure(closure, jsp)
    }

    def thin(Closure closure) {
       ConfigureUtil.configure(closure, thin)
    }

    def install(Closure closure) {
        ConfigureUtil.configure(closure, install)
    }

    def server(Closure closure){
        ConfigureUtil.configure(closure, server)
    }

}
