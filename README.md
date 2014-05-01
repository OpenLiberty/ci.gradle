# ci.gradle

A Gradle plugin to support the manipulation of WebSphere Application Server Liberty Profile servers.

## Build

Clone this repository and then, with a JRE on the path, execute the following command in the root directory.

```bash
gradlew build
```

This will download Gradle and then build the plugin `liberty-gradle-plugin-1.0.jar` in to the `build\libs` directory. It is also possible to install the plugin in to your local Maven repository using `gradlew install`.

## Usage

Within your Gradle build script, you need to set up the classpath to include the Liberty Gradle plugin and the `ws-server.jar` from a WebSphere Application Server Liberty Profile installation. If you have installed the plugin in to your local Maven repository, this would be achieved as follows:

```groovy
buildscript {
    repositories {
        mavenLocal()
    }	
    dependencies {
        classpath 'com.ibm.websphere.wlp.gradle.plugins:liberty-gradle-plugin:1.0'
        classpath files('c:/wlp/bin/tools/ws-server.jar')
    }
}
```

Alternatively, you might choose to include the plugin JAR file as part of your build project. For example:

```groovy
buildscript {
    dependencies {
        classpath files('gradle/liberty-gradle-plugin.jar')
        classpath files('c:/wlp/bin/tools/ws-server.jar')
    }
}
```

Within the script, then apply the plugin and specify its configuration as follows:

```groovy
apply plugin: 'liberty'

liberty {
    wlpDir = 'c:/wlp'
    serverName = 'myServer'
    userDir = 'c:/usr'
    outputDir = 'c:/usr'
}
```

Of the plugin configuration, only the `wlpDir` property is required. The default configuration is to use a server named `defaultServer` under the `build\wlp` directory of the Gradle project.

The plugin will have made the following tasks available to your project:

* libertyCreate - Creates a WebSphere Liberty Profile server.
* libertyStart - Starts the WebSphere Liberty Profile server.
* libertyRun - Runs a WebSphere Liberty Profile server under the Gradle process.
* libertyStatus - Checks the WebSphere Liberty Profile server is running.
* libertyPackage - Generates a WebSphere Liberty Profile server archive.
* deployWar - Deploys a WAR file to the WebSphere Liberty Profile server.
* undeployWar - Removes a WAR file from the WebSphere Liberty Profile server.