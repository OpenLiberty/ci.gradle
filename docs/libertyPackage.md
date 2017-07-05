## libertyPackage task

Package a Liberty server.

Starting with WebSphere Liberty 8.5.5.9, it is possible to package a server into an executable jar file by setting the include parameter to runnable. The created jar file can be executed using the java -jar command.

### Properties

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| include | Packaging type. Can be used with values `all`, `usr`, `minify`, `wlp`, `runnable`, `all,runnable` and `minify,runnable`. The default value is `all`. The `runnable`, `all,runnable` and `minify,runnable` values are supported beginning with 8.5.5.9 and works with `jar` type archives only. This must be declared in the `packageLiberty` closure. | Yes, only when the `os` option is set. |
| archive | Location of the target file or directory. If the 'archive' is set to an existing directory, the contents of the server instance will be compressed into `${archive}/${project.name}.zip`&#124;`jar` file. If the target file or directory doesn't exist, the contents of the server instance will be compressed into the specified file. If the `archive` option is not set, it defaults to `${buildDir}/libs/${project.name}.zip`&#124;`jar`. A jar file is created when the packaging type is either `runnable`,`all,runnable` or `minify,runnable`. A zip file is created for other packaging types. | No |
| os| A comma-delimited list of operating systems that you want the packaged server to support. To specify that an operating system is not to be supported, prefix it with a minus sign ("-"). The 'include' attribute __must__ be set to `minify`. | No |


This example shows you how to configure these properties in your script:

```groovy
apply plugin: 'liberty'

liberty {
    installDir = 'c:/wlp'
    serverName = 'myServer'
    userDir = 'c:/usr'
    outputDir = 'c:/usr'
    clean = true
    timeout = "10000"

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

```
