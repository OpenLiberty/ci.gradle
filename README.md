# ci.gradle [![Build Status](https://travis-ci.org/WASdev/ci.gradle.svg?branch=master)](https://travis-ci.org/WASdev/ci.gradle) [![Maven Central Latest](https://maven-badges.herokuapp.com/maven-central/net.wasdev.wlp.gradle.plugins/liberty-gradle-plugin/badge.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22net.wasdev.wlp.gradle.plugins%22%20AND%20a%3A%22liberty-gradle-plugin%22)

A Gradle plugin to support the manipulation of WebSphere Liberty servers.

* [Build](#build)
* [Usage](#usage)
* [Tasks](#tasks)
* [Properties](#extension-properties)

## Build

Clone this repository and then, with a JRE on the path, execute the following command in the root directory.

```bash
$ gradlew build
```

This will download Gradle and then build the plugin `liberty-gradle-plugin-1.1-SNAPSHOT.jar` in to the `build\libs` directory. It is also possible to install the plugin in to your local Maven repository using `gradlew install`.

To build the plugin and run the integration tests execute the following commands in the root directory.

1. To run the integration tests against an existing WebSphere Liberty server installation.
 ```bash
 $ gradlew build -Prunit=offline -DwlpInstallDir=<liberty_install_directory>
 ```
   
2. Run the integration tests against automatically downloaded and installed WebSphere Liberty server.
 ```bash
 $ gradlew build -Prunit=online -DwlpLicense=<liberty_licesnse_code> -DwlpVersion=<liberty_version>
 ```

## Usage
### Configuring your dependencies

####  Adding the Ant plugin to the build script
This plugin needs the `wlp-anttasks.jar`file as a dependency, this file can be downloaded from the [snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/net/wasdev/wlp/ant/wlp-anttasks/) or the [Maven central repository](http://repo1.maven.org/maven2/net/wasdev/wlp/ant/wlp-anttasks/).

The following code snippet shows an example for how to set up your build script correctly.
```groovy
buildscript {
    dependencies {
        classpath files('gradle/wlp-anttasks.jar')
    }
}
```


### Adding the binary plugin to the build script

Within your Gradle build script, you need to set up the classpath to include the Liberty Gradle plugin. You also need to define the Maven Central repository to find the plugin or its dependencies. 

If you are using a snapshot version of the plugin make sure to define the Sonatype Nexus Snapshots repository in addition to the Maven Central repository.

Your build script should look like this:

```groovy
buildscript {
    repositories {
        mavenCentral()
        maven {
            name = 'Sonatype Nexus Snapshots'
            url = 'https://oss.sonatype.org/content/repositories/snapshots/'
        }
    }
    dependencies {
        classpath 'net.wasdev.wlp.gradle.plugins:liberty-gradle-plugin:1.1-SNAPSHOT'
    }
}
```

Alternatively, you might choose to include the plugin JAR file. For example:

```groovy
buildscript {
    dependencies {
        classpath files('gradle/liberty-gradle-plugin.jar')
        classpath files('gradle/wlp-anttasks.jar')
    }
}
```
To use the Liberty Gradle Plugin, include the following code in your build script:


```groovy
apply plugin: 'liberty'
```

## Tasks

The plugin will have made the following tasks available to your project:

| Task | Description |
| --------- | ------------ |
| [installLiberty](docs/installLiberty.md#installLiberty-task) | Installs WebSphere Liberty server from a repository. |
| libertyCreate | Creates a WebSphere Liberty server. |
| libertyStart | Starts the WebSphere Liberty server. |
| libertyStop | Stops the WebSphere Liberty server. |
| libertyRun | Runs a WebSphere Liberty server under the Gradle process. |
| [libertyPackage](docs/libertyPackage.md#libertypackage-task) | Package a WebSphere Liberty server. | 
| libertyDump | Dumps diagnostic information from the WebSphere Liberty server into an archive. | 
| libertyJavaDump | Dumps diagnostic information from the WebSphere Liberty server JVM. | 
| libertyDebug | Runs the WebSphere Liberty server in the console foreground after a debugger connects to the debug port (default: 7777). | 
| libertyStatus | Checks the WebSphere Liberty server is running. |
| [deploy](docs/deploy.md#deploy-task) | Deploys a supported file to the WebSphere Liberty server. |
| [undeploy](docs/undeploy.md#undeploy-task) | Removes an application from the WebSphere Liberty server. |
| [installFeature](docs/installFeature.md#installfeature-task) | Installs a new feature in the WebSphere Liberty server. |
| [uninstallFeature](docs/uninstallFeature.md#uninstallfeature-task) | Uninstall a feature in the WebSphere Liberty server. |
| [cleanDir](docs/clean.md#clean-task) | Deletes files from some directories in the WebSphere Liberty server. |

## Extension properties
The Liberty Gradle Plugin has some properties defined in the `Liberty` closure which will let you customize the different tasks.
These properties are divided in two groups, the general properties (Which need to be set for any task excluding `installLiberty` task) and the specific ones. (Which only must be set when a specific task will be kicked off).

### General properties

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| installDir | Location of the WebSphere Liberty server installation. | Yes |
| outputDir | Value of the `${wlp_output_dir}` variable. The default value is `${installDir}/usr/servers/${serverName}`.| No |
| userDir | Value of the `${wlp_user_dir}` variable. The default value is `${installDir}/usr/`. | No |
| serverName | Name of the WebSphere Liberty server instance. The default value is `defaultServer`. | No |


### Server Task Properties

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| clean | Clean all cached information on server start up. The default value is `false`. Only used with the `libertyStart` task. | No | 
| timeout | Waiting time before the server starts. The default value is 30 seconds. The unit is milliseconds. Only used with the `libertyStart` task. | No | 
| include | A comma-delimited list of values. The valid values vary depending on the task. For the `libertyDump` task the valid values are `heap`, `system`, and `thread` and must be declared in the `dumpLiberty` closure. For the `libertyJavaDump` task the valid values are `heap` and `system` and must be declared in the `javaDumpLiberty` closure. |  No |
| archive | Location of the target archive file. Only used with the `libertyPackage` or `libertyDump` tasks on their respective closures. | No |
| template | Name of the template to use when creating a new server. Only used with the `libertyCreate` task. | No |

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

    dumpLiberty {
        archive = "C:/Dump.zip"
        include = "heap, system"
    }
    javaDumpLiberty {
        archive = "JavaDump.zip"
        include = "system"
    }
}

```

Of the plugin configuration, only the `installDir` property is required. The default configuration is to use a server named `defaultServer` under the `build\wlp` directory of the Gradle project.
