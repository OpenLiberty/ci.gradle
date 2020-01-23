## libertyCreate task

The `libertyCreate` task is used to create a named Liberty server instance.

### dependsOn

`libertyCreate` depends on `installFeature`.

### Properties

Server configuration parameters were added to the [server extension](libertyExtensions.md#liberty-server-configuration) for flexible configuration. Running `libertyCreate` will update the configuration files if the server already exists.

### Examples

These examples show you different ways to configure servers in your scripts:

1. Configure using `configDirectory`. This is useful when you need to copy multiple files such as `server.xml`, `bootstrap.properties`, and included configuration files. Alternatively, you can place your configuration files in the default location for `configDirectory` which is `src/main/liberty/config`.

```groovy
apply plugin: 'liberty'

liberty {
    server {
        configDirectory = file('src/resources/config')
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
        serverXmlFile = file('src/resources/config/server-test1.xml')
        bootstrapPropertiesFile = file('src/resources/config/bootstrap.test.properties')
        jvmOptionsFile = file('src/resources/config/jvm.test.options')
        serverEnvFile = file('src/resources/config/server.test.env')
    }
}
```

4. Override configuration specified above with an `ext` block in `build.gradle`, with a `gradle.properties` file or with project properties specified on the command line. The following server extension properties can be overridden.

* bootstrapProperties
* defaultVar
* env
* jvmOptions
* var

Those backed by a Properties object can be overridden as a whole or by specifying individual properties. The `jvmOptions` can only be overridden as a whole.

Examples of using build.gradle file:
```groovy
ext {
    liberty.server.env."another.serverenv.var" = "anotherValue"
    liberty.server.defaultVar.someDefaultVar = 'someDefaultValue'
    liberty.server.var.someVar = 'someValue'
    liberty.server.var."my.custom.var" = 'myCustomValue'
    liberty.server.bootstrapProperties."default.http.port" = '9083'
    liberty.server.jvmOptions=['-Xms128m','-Xmx2048m']
}
```

Examples of using gradle.properties file:
```xml
liberty.server.env."another.serverenv.var"=anotherValue
liberty.server.defaultVar.someDefaultVar=someDefaultValue
liberty.server.var={"someVar"\:"someValue","my.custom.var"\:"myCustomValue"}
liberty.server.bootstrapProperties."default.http.port"=9083
liberty.server.jvmOptions={"-Xms128m","-Xmx2048m"}
```

Examples of using the command line:

`gradle build -Pliberty.server.bootstrapProperties."default.http.port"=9083`

`gradle build -Pliberty.server.jvmOptions={"-Xms128m","-Xmx2048m"}`
