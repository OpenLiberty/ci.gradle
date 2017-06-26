## libertyPackage task

Package a Liberty server.

### Properties

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| include | A comma-delimited list of values. The valid values vary depending on the task. For the libertyPackage task the valid values are all, usr, and minify and must be declared in the packageLiberty closure. | Yes, only when the `os` option is set. |
| archive | Location of the target archive file. Used with the libertyPackage task on its respective closure. | No |
| os| A comma-delimited list of operating systems that you want the packaged server to support. Used in the packageLiberty closure. The 'include' attribute __must__ be set to `minify`. | No |


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

    packageLiberty {
        archive = "MyServerPackage.zip"
        include = "usr"
    }
    
    //Example to package with 'os' parameter
    packageLiberty {
        archive = "MyServerPackage.zip"
        include = "minify"
        os = "Linux"
    }
    
    packageLiberty {
        archive = "MyServerPackage.jar"
        include = "runnable"
    }
    
    //Example to package in a target directory
    packageLiberty {
        archive = "MyTargetDir"
        include = "minify"
        os = "Linux"
    }
}

```
