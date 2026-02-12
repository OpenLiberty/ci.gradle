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

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory

import javax.inject.Inject

class LibertyExtension {

    // Path to the WebSphere Liberty server installation `wlp` directory. To use a pre-installed version of Liberty, set this property to the path of the Liberty `wlp` directory, including `wlp` in the path. Additionally, `installDir` can be specified in the `gradle.properties` file or from the [command line](installLiberty.md#override-installDir). 
    String installDir
    // Deprecated. Value of the `${wlp_output_dir}` variable. The default value is `${installDir}/usr/servers/${serverName}`. This parameter has moved to the `server` block.
    String outputDir
    // Value of the `${wlp_user_dir}` variable. The default value is `${installDir}/usr/`.
    String userDir

    // The base installation directory. The actual installation directory of WebSphere Liberty server will be `${baseDir}/wlp`. The default value is `${project.getLayout().getBuildDirectory().getAsFile().get()}`. This was moved from the properties in the `install` block in version 3.0.
    String baseDir
    // The directory used for caching downloaded files such as the license or `.jar` files. The default value is `${java.io.tmpdir}/wlp-cache`. This was moved from the properties in the `install` block in version 3.0.
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
