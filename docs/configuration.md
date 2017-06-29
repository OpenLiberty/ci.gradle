## Liberty Server Configuration

User defined configuration files can be specified within the build.gradle file. These files are copied to the appropriate server directory during the libertyCreate task.

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| configFile| Location of the server.xml file used by the server instance. The default value is `/src/main/liberty/config/server.xml`.| No|
| configDirectory| Location of a configuration directory that contains configuration files. Any files in this directory will take precedence over specified files of the same type.| No|
| bootstrapProperties| Inline `Map` of bootstrap properties that will be written to a bootstrap.properties file in the server directory. These will take precedence over a specified bootstrap.properties file.| No|
| bootstrapPropertiesFile| Location of the boostrap.properties file used by the server instance. The default value is `/src/main/liberty/config/bootstrap.properties`.| No |
| jvmOptions| Inline `List` of jvm options that will be written to a jvm.options file in the server directory. These will take precedence over a specified jvm.options file.| No|
| jvmOptionsFile| Location of the jvm.options file used by the server instance. The default value is `/src/main/liberty/config/jvm.options`.| No|
| serverEnv| Location of the server.env file to be used by the server instane. The default value is `/src/main/liberty/config/server.env`.| No |

These examples show you how to configure these properties in your scripts:

1. Configured using `configDirectory`.
```groovy
apply plugin: 'liberty'

liberty {
    configDirectory = 'src/main/liberty/config'
} 
```
2. Configured with inline properties.
```groovy
apply plugin: 'liberty'

liberty {
    bootstrapProperties = ['default.http.port':'9080', 'default.https.port':'9443']
    jvmOptions = ['-Xms128m', '-Xmx512m']
} 
```
3. Configured with user defined files.
```groovy
apply plugin: 'liberty'

liberty {
    configFile = 'src/main/liberty/config/server.xml'
    bootstrapPropertiesFile = 'src/main/liberty/config/bootstrap.properties'
    jvmOptionsFile = 'src/main/liberty/config/jvm.options'
    serverEnv = 'src/main/liberty/config/server.env'
} 
```