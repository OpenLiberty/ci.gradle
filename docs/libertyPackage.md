## libertyPackage task

Package a Liberty server.

The `libertyPackage` task is used to create a ZIP, TAR, TAR.GZ or JAR archive of your Liberty runtime and server.
In Open Liberty and WebSphere Liberty versions since 8.5.5.9, it is possible to package a server into an executable jar file by setting the `include` parameter to `runnable`. The created JAR file can be executed using the `java -jar` command.

### dependsOn
`libertyPackage` depends on `installLiberty`.  
`libertyPackage` also depends on `deploy` and `installFeature` if configured.  
  
### Properties

See the [Liberty server configuration](libertyExtensions.md#liberty-server-configuration) properties for common server configuration.

The `libertyPackage` task uses a `packageLiberty` block to define task specific behavior.

| Attribute | Type  | Since | Description | Required |
| --------- | ----- | ----- | ----------- | -------- |
| include | String | 1.0 | Controls the package contents. Can be used with values `all`, `usr`, `minify`, `wlp`, `runnable`, `all,runnable` and `minify,runnable`. The default value is `all`. The `runnable`, `all,runnable` and `minify,runnable` values are supported beginning with 8.5.5.9 and works with `jar` type packages only. | Yes, only when the `os` option is set. |
| os| String | 1.0 | A comma-delimited list of operating systems that you want the packaged server to support. To specify that an operating system is not to be supported, prefix it with a minus sign ("-"). The 'include' attribute __must__ be set to `minify`. | No |
| packageDirectory |  String | 3.0 | Directory of the packaged file. The default value is `${project.buildDir}/libs`. If the directory is not absolute, it is created in `${project.buildDir}/libs`.| No |
| packageName |  String | 3.0 | Name of the packaged file. The default value is `${project.name}`. | No |
| packageType | String | 3.0 | Type of package. Can be used with values `zip`, `jar`, `tar`, or `tar.gz`. Defaults to `jar` if `runnable` is specified for the `include` property. Otherwise the default value is `zip`. | No |
| serverRoot |  String | 3.0 | Specifies the root server folder name in the packaged file. | No |


This example shows you how to package a minified ZIP archive.

```groovy
apply plugin: 'liberty'

liberty {

    server {
        name = 'myServer'

        packageLiberty {
            packageName = "MyPackage"
            include = "minify"
            os = "Linux"
        }
    }
}

```

This example shows you how to package a runnable JAR file.

```groovy
apply plugin: 'liberty'

liberty {

    server {
        name = 'myServer'

        packageLiberty {
            packageName = "MyPackage"
            include = "runnable"
        }   
    }
}

```
