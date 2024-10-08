/**
 * (C) Copyright IBM Corporation 2014, 2024.
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

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory

import javax.inject.Inject

class LibertyExtension {

    String installDir
    String outputDir
    String userDir

    String baseDir
    String cacheDir

    // For overriding the group, name or version of the libertyRuntime installed from Maven Central repository.
    // Default is group 'io.openliberty', name 'openliberty-kernel' and version '[19.0.0.9,)' which gets the latest version.
    Properties runtime = new Properties();

    CompileJSPExtension jsp

    InstallExtension install
    SpringBootExtension thin
    ServerExtension server

    DevExtension dev;

    @Inject
    LibertyExtension(ObjectFactory objectFactory) {
        this.jsp = objectFactory.newInstance(CompileJSPExtension.class)
        this.install = objectFactory.newInstance(InstallExtension.class)
        this.thin = objectFactory.newInstance(SpringBootExtension.class)
        this.server = objectFactory.newInstance(ServerExtension.class)
        this.dev = objectFactory.newInstance(DevExtension.class)
    }

    def jsp(Action action) {
       action.execute(jsp)
    }

    def thin(Action action) {
        action.execute(thin)
    }

    def install(Action action) {
        action.execute(install)
    }

    def server(Action action) {
        action.execute(server)
    }

    def dev(Action action) {
        action.execute(dev)
    }

}
