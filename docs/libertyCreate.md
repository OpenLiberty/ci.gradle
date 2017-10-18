## libertyCreate task

The `libertyCreate` task is used to create a named Liberty server instance.

### dependsOn

`libertyCreate` depends on `installLiberty`.

### Properties

Server configuration parameters were added to the [server extension](libertyExtensions.md#liberty-server-configuration) for flexible configuration. Running `libertyCreate` will update the configuration files if the server already exists.

### Examples

These examples show you different ways to configure servers in your scripts:

1. Configure using `configDirectory`. This is useful when you need to copy multiple files such as `server.xml`, `bootstrap.properties`, and included configuration files.

```groovy
apply plugin: 'liberty'

liberty {
    server {
        configDirectory = file('src/main/liberty/config')
    }
}
```
2. Configure with inline properties. The `bootstrap.properties` and `jvm.options` files are created with this content.

```groovy
apply plugin: 'liberty'

liberty {
    server {
        bootstrapProperties = ['default.http.port':'9080', 'default.https.port':'9443']
        jvmOptions = ['-Xms128m', '-Xmx512m']
    }
}
```
3. Configure with user defined files. For this specific configuration file copy, the file is renamed to the expected name in the target.

```groovy
apply plugin: 'liberty'

liberty {
    server {
        configFile = file('src/main/liberty/config/server-test1.xml')
        bootstrapPropertiesFile = file('src/main/liberty/config/bootstrap.properties')
        jvmOptionsFile = file('src/main/liberty/config/jvm.options')
        serverEnv = file('src/main/liberty/config/server.env')
    }
}
```
