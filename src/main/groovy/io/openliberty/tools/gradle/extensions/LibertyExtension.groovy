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
    String serverName = "defaultServer"

    String appsDirectory = "apps"
    boolean stripVersion = false
    boolean looseApplication = true

    File configDirectory
    File serverXmlFile
    File bootstrapPropertiesFile
    File jvmOptionsFile
    File serverEnvFile

    boolean clean = false
    String timeout
    String template
    boolean noPassword = false
    boolean embedded = false

    int verifyAppStartTimeout = 0

    String jspVersion
    int jspCompileTimeout = 40

    def numberOfClosures = 0

    FeatureExtension features = new FeatureExtension()
    UninstallFeatureExtension uninstallfeatures = new UninstallFeatureExtension()
    InstallExtension install = new InstallExtension()
    CleanExtension cleanDir = new CleanExtension()
    SpringBootExtension thin = new SpringBootExtension()

    DeployExtension deploy = new DeployExtension()
    UndeployExtension undeploy = new UndeployExtension()

    PackageExtension packageLiberty = new PackageExtension()
    DumpExtension dumpLiberty = new DumpExtension()
    DumpExtension javaDumpLiberty = new DumpExtension()

    NamedDomainObjectContainer<ServerExtension> servers

    ServerExtension server

    def uninstallfeatures(Closure closure) {
        ConfigureUtil.configure(closure, uninstallfeatures)
    }

    def features(Closure closure) {
        ConfigureUtil.configure(closure, features)
    }

    def install(Closure closure) {
        ConfigureUtil.configure(closure, install)
    }

    def deploy(Closure closure) {
        if (numberOfClosures > 0){
            deploy.listOfClosures.add(deploy.clone())
            deploy.file = null
        }
        ConfigureUtil.configure(closure, deploy)
        numberOfClosures++
    }

    def undeploy(Closure closure) {
        ConfigureUtil.configure(closure, undeploy)
    }

    def packageLiberty(Closure closure) {
        ConfigureUtil.configure(closure, packageLiberty)
    }

    def dumpLiberty(Closure closure) {
        ConfigureUtil.configure(closure, dumpLiberty)
    }

    def javaDumpLiberty(Closure closure) {
        ConfigureUtil.configure(closure, javaDumpLiberty)
    }

    def cleanDir(Closure closure) {
        ConfigureUtil.configure(closure, cleanDir)
    }

    def thin(Closure closure) {
       ConfigureUtil.configure(closure, thin)
    }

    def server(Closure closure){
        server = new ServerExtension()
        ConfigureUtil.configure(closure, server)
    }

    def servers(Closure configurationClosure){
        servers.configure(configurationClosure)
    }

}
