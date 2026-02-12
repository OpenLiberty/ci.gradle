/**
 * (C) Copyright IBM Corporation 2017, 2025.
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

    // The name of the server instance to create. The default value is `defaultServer`.
    String name = "defaultServer"

    // Value of the `${wlp_output_dir}` variable. The default value is `${installDir}/usr/servers/${serverName}`. 
    String outputDir

    // Application directory. Either "apps" or "dropins". This defaults to "apps" when there is 
    // some application configured in server XML config and "dropins" when there is not.
    String appsDirectory = "apps"

    // Remove the artifact version when copying the application to Liberty runtime's application directory. The default value is false.
    boolean stripVersion = false

    // Indicates to install application using loose application configuration so that war or ear files do not need to be regenerated for every application update. 
    // The loose application support for ear files requires Gradle 4.0 or higher.  The default is `true`.
    boolean looseApplication = true

    // Location of a server configuration directory to be used by the server instance. 
    // Configuration files and folder structure will be copied to the server instance. 
    // Files specified by other server extension properties will take precedence over files located in the configDirectory. 
    // The default value is `/src/main/liberty/config`.
    File configDirectory

    // Location of the `server.xml` file used by the server instance. This replaces the configFile parameter.
    File serverXmlFile

    // Location of the file containing server properties to copy to the bootstrap.properties file in the server instance.
    File bootstrapPropertiesFile

    // Location of the file containing JVM options to copy to the jvm.options file in the server instance.
    File jvmOptionsFile

    // Location of the file containing server environment variables to copy to the server.env file in the server instance. This replaces the serverEnv property
    File serverEnvFile

    // Merge the server environment properties from all specified sources with the default generated `server.env` file in the target server. 
    // Conflicts are resolved with the same precedence as the replacement policy when this attribute is set to `false`. 
    // The properties specified in the `env` attribute are highest precedence, followed by the `serverEnvFile` attribute, 
    // then the `server.env` file located in the `configDirectory`, and finally the default generated `server.env` file in the target server. The default value is `false`.
    boolean mergeServerEnv = false;

    // Inline bootstrap `Properties` that are written to the bootstrap.properties file in the server directory. 
    // These properties take precedence over a specified bootstrap.properties file. This was changed from a `Map` to a `Properties` object in 3.0.
    Properties bootstrapProperties = new Properties()

    // Inline `List` of jvm options that is written to the jvm.options file in the server directory. These properties take precedence over a specified jvm.options file.
    List<String> jvmOptions

    // Inline server environment variables that are written to the server.env file in the server directory. These properties take precedence over a specified server.env file.
    Properties env = new Properties()

    // Inline server variables that are written to the `configDropins/overrides/liberty-plugin-variable-config.xml` file in the server directory. 
    // The property name is used for the variable `name`, and the property value is used for the variable `value`.
    Properties var = new Properties()

    // Inline server variables that are written to the `configDropins/defaults/liberty-plugin-variable-config.xml` file in the server directory. 
    // The property name is used for the variable `name`, and the property value is used for the variable `defaultValue`.
    Properties defaultVar = new Properties()

    // You can verify your user features by providing the long key ID and key URL to reference your public key that is stored on a key server. 
    // For more information about generating a key pair, signing the user feature, and distributing your key, see [Working with PGP Signatures](https://central.sonatype.org/publish/requirements/gpg/#signing-a-file).
    Properties keys = new Properties()

    // Clean all cached information on server start up. It deletes every file in the `${wlp_output_dir}/logs`, `${wlp_output_dir}/workarea`, `${wlp_user_dir}/dropins` or `${wlp_user_dir}/apps`. 
    // The default value is `false`. Only used with the `libertyStart` and `libertyRun` tasks.
    boolean clean = false

    // Waiting time before the server starts. The default value is 30 seconds. The unit is seconds. Only used with `libertyStart` and `deploy` tasks.
    String timeout

    // Name of the template to use when creating a new server. Only used with the `libertyCreate` task.
    String template

    // Disable generation of the default keystore password by specifying the --no-password option when creating a new server. 
    // This option was added in 18.0.0.3. The default value is `false`. Only used with the `libertyCreate` task. 
    boolean noPassword = false

    // Whether the server is [embedded](https://www.ibm.com/support/knowledgecenter/SSD28V_9.0.0/com.ibm.websphere.wlp.core.doc/ae/twlp_extend_embed.html) in the Gradle JVM. 
    // If not, the server will run as a separate process. The default value is `false`. Only used with the `libertyStart`, `libertyRun` and `libertyStop` tasks.
    boolean embedded = false

    // Wait time for checking message logs for start of all applications installed with the `deploy` task. 
    // Only used with the `libertyStart` task. Default value is 0 seconds with no verification.
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
