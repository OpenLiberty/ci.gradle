## libertyPackage task

Package a Liberty server.

The `libertyPackage` task is used to create a ZIP or JAR archive of your Liberty runtime and server.
Starting with WebSphere Liberty 8.5.5.9, it is possible to package a server into an executable jar file by setting the include parameter to runnable. The created JAR file can be executed using the `java -jar` command.

### Properties

See the [Liberty server configuration](libertyExtensions.md#liberty-server-configuration) properties for common server configuration.

The `libertyPackage` task uses a `packageLiberty` block to define task specific behavior.

| Attribute | Type  | Since | Description | Required |
| --------- | ----- | ----- | ----------- | -------- |
| include | String | 1.0 | Packaging type. Can be used with values `all`, `usr`, `minify`, `wlp`, `runnable`, `all,runnable` and `minify,runnable`. The default value is `all`. The `runnable`, `all,runnable` and `minify,runnable` values are supported beginning with 8.5.5.9 and works with `jar` type archives only. This must be declared in the `packageLiberty` block. | Yes, only when the `os` option is set. |
| archive | String | 1.0 | Location of the target file or directory. If the 'archive' is set to an existing directory, the contents of the server instance will be compressed into `${archive}/${project.name}.zip`&#124;`jar` file. If the target file or directory doesn't exist, the contents of the server instance will be compressed into the specified file. If the `archive` option is not set, it defaults to `${buildDir}/libs/${project.name}.zip`&#124;`jar`. A jar file is created when the packaging type is either `runnable`,`all,runnable` or `minify,runnable`. A zip file is created for other packaging types. | No |
| os| String | 1.0 | A comma-delimited list of operating systems that you want the packaged server to support. To specify that an operating system is not to be supported, prefix it with a minus sign ("-"). The 'include' attribute __must__ be set to `minify`. | No |


This example shows you how to configure these properties in your script:

```groovy
apply plugin: 'liberty'

liberty {

    server {
        name = 'myServer'
        clean = true

        //Example to package '${buildDir}/MyPackage.zip'
        packageLiberty {
            archive = "MyPackage.zip"
            include = "minify"
            os = "Linux"
        }

        //Example to package '${buildDir}/MyPackage.jar'
        packageLiberty {
            archive = "MyPackage.jar"
            include = "runnable"
        }   
    }
}

```
