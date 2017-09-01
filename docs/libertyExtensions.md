## Liberty extension properties
The Liberty Gradle plugin defines properties in the `liberty` block to customize task behavior.
These properties are divided in two groups:
* The [general runtime properties](#general-runtime-properties) which control how the runtime is installed with the `installLiberty` task.
* The [Liberty server configuration](#liberty-server-configuration) properties control how a server is initialized and the applications that are installed. Starting in version 1.1, these properties are defined in a `server` block within the `liberty` block.

Some tasks have additional configuration blocks.

### General runtime properties

| Attribute | Type | Since | Description | Required |
| --------- | ---- | ----- | ----------- | ---------|
| installDir | String | 1.0 | Location of the WebSphere Liberty server installation. If you want to use a pre-installed version of Liberty, set this property to the location of the Liberty `wlp` directory. Otherwise, the specified version is downloaded and installed to this directory or the default location of ${project} | No |
| outputDir | String | 1.0 | Deprecated. Value of the `${wlp_output_dir}` variable. The default value is `${installDir}/usr/servers/${serverName}`. This parameter has moved to the `server` block. | No |
| userDir | String | 1.0 | Value of the `${wlp_user_dir}` variable. The default value is `${installDir}/usr/`. | No |
| serverName | String | 1.0 | Deprecated. Name of the WebSphere Liberty server instance. The default value is `defaultServer`. The server name is now defined by the `name` property in the `server` extension. | No |


### Liberty server configuration

The `server` extension is new in version 1.1 of the Liberty Gradle plugin and allows you to configure all the characteristics of a particular named server including configuration files, applications to install, and server specific properties. To configure the server, add the `server` block to the `liberty` block in your build.gradle file.

Some of the configuration properties of the `server` block were previously available in the `liberty` block. If you want to use new properties of the `server` block, move all the server related properties to the `server` block. If a `server` block exists, the same configuration in the `liberty` block is ignored. If you want to use your exiting build scripts as-is, you can leave the properties in the `liberty` block as long as you do not create a `server` block.

The following properties are supported for server configuration.

### Server extension properties

| Attribute | Type  | Since | Description | Required |
| --------- | ----- | ----- | ----------- | -------- |
| apps | List | 1.1 | List of `war` task objects used to create applications to copy to the `apps` folder. If no `apps` or `dropins` are configured and this project applys the `war` plugin, the default application is installed using the `installApps` task. If the application is not configured in the server.xml file, application configuration is added to the `configDropins` folder. | No |
| clean | boolean | 1.0 | Clean all cached information on server start up. It deletes every file in the `${wlp_output_dir}/logs`, `${wlp_output_dir}/workarea`, `${wlp_user_dir}/dropins` or `${wlp_user_dir}/apps`. The default value is `false`. Only used with the `libertyStart` and `libertyRun` tasks. | No |
| dropins | List | 1.1 | List of `war` objects used to create applications to copy to the `dropins` folder. | No |
| outputDir | String | 1.0 | Value of the `${wlp_output_dir}` variable. The default value is `${installDir}/usr/servers/${serverName}`. | No |
| bootstrapProperties| Map | 1.1 | Inline `Map` of bootstrap properties that is written to the bootstrap.properties file in the server directory. These properties take precedence over a specified bootstrap.properties file.| No|
| bootstrapPropertiesFile| File | 1.1 | Location of the file containing server properties to copy to the bootstrap.properties file in the server instance. The default value is `/src/main/liberty/config/bootstrap.properties`.| No |
| configDirectory| File | 1.1 | Location of a configuration directory that contains files copied to the server configuration directory. Any files in this directory take precedence over specified files of the same type. This attribute is useful for copying included configuration files or a set of configuration files. | No|
| configFile| File | 1.1 | Location of the `server.xml` file used by the server instance. The default value is `/src/main/liberty/config/server.xml`. After the | No|
| jvmOptions| List | 1.1 | Inline `List` of jvm options that is written to the jvm.options file in the server directory. These properties take precedence over a specified jvm.options file.| No|
| jvmOptionsFile| File | 1.1 | Location of the file containing JVM options to copy to the jvm.options file in the server instance. The default value is `/src/main/liberty/config/jvm.options`.| No|
| looseApplication | boolean | 1.1 | Indicates to install application using loose application configuration so that war files do not need to be regenerated for every application update. The default is `true`. | No |
| name | String | 1.1 | The name of the server instance to create. The default value is `defaultServer`. | No |
| serverEnv| File | 1.1 | Location of the file containing server environment variables to copy to the server.env file in the server instance. The default value is `/src/main/liberty/config/server.env`.| No |
| stripVersion | boolean | 1.1 | Remove the artifact version when copying the application to Liberty runtime's application directory. The default value is false.  | No |
| template | String | 1.0 | Name of the template to use when creating a new server. Only used with the `libertyCreate` task. | No |
| timeout | String | 1.0 | Waiting time before the server starts. The default value is 30 seconds. The unit is seconds. Only used with `libertyStart` and `deploy` tasks. | No |
| verifyAppStartTimeout | int | 1.1 | Wait time for checking message logs for start of all applications installed with the `installApps` task. Only used with the `libertyStart` task. Default value is 0 seconds with no verification. | No |
