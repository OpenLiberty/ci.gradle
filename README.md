# ci.gradle

A Gradle plugin to support the manipulation of WebSphere Application Server Liberty Profile servers.

## Build

Clone this repository and then, with a JRE on the path, execute the following command in the root directory.

```bash
gradlew build
```

This will download Gradle and then build the plugin `liberty-gradle-plugin-1.0-SNAPSHOT.jar` in to the `build\libs` directory. It is also possible to install the plugin in to your local Maven repository using `gradlew install`.

## Usage

Within your Gradle build script, you need to set up the classpath to include the Liberty Gradle plugin and the `ws-server.jar` from a WebSphere Application Server Liberty Profile installation. You also need to define the Maven Central repository to find the plugin or its dependencies. If you are using a snapshot version of the plugin make sure to also define the Sonatype Nexus Snapshots repository.

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
        classpath 'net.wasdev.wlp.gradle.plugins:liberty-gradle-plugin:1.0-SNAPSHOT'
        classpath files('c:/wlp/bin/tools/ws-server.jar')
    }
}
```

Alternatively, you might choose to include the plugin JAR file as part of your build project. For example:

```groovy
buildscript {
    dependencies {
        classpath files('gradle/liberty-gradle-plugin.jar')
        classpath files('gradle/wlp-anttasks.jar')
        classpath files('c:/wlp/bin/tools/ws-server.jar')
    }
}
```

The dependent `wlp-anttasks.jar` file can be downloaded from the [snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/net/wasdev/wlp/ant/wlp-anttasks/) or the [Maven central repository](http://repo1.maven.org/maven2/net/wasdev/wlp/ant/wlp-anttasks/). 

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
