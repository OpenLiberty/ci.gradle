/**
 * (C) Copyright IBM Corporation 2017, 2024.
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

class ServerExtension {
    //Server properties
    String name = "defaultServer"
    String outputDir

    String appsDirectory = "apps"
    boolean stripVersion = false
    boolean looseApplication = true

    File configDirectory
    File serverXmlFile
    File bootstrapPropertiesFile
    File jvmOptionsFile
    File serverEnvFile
    boolean mergeServerEnv = false;

    Properties bootstrapProperties = new Properties()
    List<String> jvmOptions
    Properties env = new Properties()
    Properties var = new Properties()
    Properties defaultVar = new Properties()
    Properties keys = new Properties()

    boolean clean = false
    String timeout
    String template
    boolean noPassword = false
    boolean embedded = false

    int verifyAppStartTimeout = 0

    FeatureExtension features
    UninstallFeatureExtension uninstallfeatures
    CleanExtension cleanDir

    DeployExtension deploy
    UndeployExtension undeploy

    PackageExtension packageLiberty
    DumpExtension dumpLiberty
    DumpExtension javaDumpLiberty

    @Inject
    ServerExtension(ObjectFactory objectFactory) {
        this.features = objectFactory.newInstance(FeatureExtension.class)
        this.uninstallfeatures = objectFactory.newInstance(UninstallFeatureExtension.class)
        this.cleanDir = objectFactory.newInstance(CleanExtension.class)
        this.deploy = objectFactory.newInstance(DeployExtension.class)
        this.undeploy = objectFactory.newInstance(UndeployExtension.class)
        this.packageLiberty = objectFactory.newInstance(PackageExtension.class)
        this.dumpLiberty = objectFactory.newInstance(DumpExtension.class)
        this.javaDumpLiberty = objectFactory.newInstance(DumpExtension.class)
    }

    def uninstallfeatures(Action action) {
        action.execute(uninstallfeatures)
    }

    def features(Action action) {
        action.execute(features)
    }

    def cleanDir(Action action) {
        action.execute(cleanDir)
    }

    def deploy(Action action) {
        action.execute(deploy)
    }

    def undeploy(Action action) {
        action.execute(undeploy)
    }

    def packageLiberty(Action action) {
        action.execute(packageLiberty)
    }

    def dumpLiberty(Action action) {
        action.execute(dumpLiberty)
    }

    def javaDumpLiberty(Action action) {
        action.execute(javaDumpLiberty)
    }

}
