# ci.gradle [![Maven Central Latest](https://maven-badges.herokuapp.com/maven-central/net.wasdev.wlp.gradle.plugins/liberty-gradle-plugin/badge.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22net.wasdev.wlp.gradle.plugins%22%20AND%20a%3A%22liberty-gradle-plugin%22) [![Build Status](https://travis-ci.com/WASdev/ci.gradle.svg?branch=master)](https://travis-ci.com/WASdev/ci.gradle) [![Build status](https://ci.appveyor.com/api/projects/status/ebq1a5qtt8ndhc57/branch/master?svg=true)](https://ci.appveyor.com/project/wasdevb1/ci-gradle-6hm2g) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/9fa7d434945c452cae1c4958bfda8010)](https://www.codacy.com/app/wasdevb1/ci.gradle?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=WASdev/ci.gradle&amp;utm_campaign=Badge_Grade)

The Liberty Gradle plugin supports install and operational control of Liberty runtime and servers. Use it to manage your application on Liberty for integration test and to create Liberty server packages.

* [Build](#build)
* [Usage](#usage)
* [Plugin Configuration](#plugin-configuration)
* [Tasks](#tasks)
* [Extensions](#extensions)


## Build

Clone this repository and then, with a JRE on the path, execute the following command in the root directory.

```bash
$ ./gradlew build
```

This will download Gradle, build the plugin, and install it in to the `build\libs` directory. It is also possible to install the plugin in to your local Maven repository using `./gradlew install`.

To build the plugin and run the integration tests execute the following commands in the root directory. The `runtime` and `runtimeVersion` parameters are used to select the Liberty runtime that will be used to run the tests. The `wlpLicense` parameter is only needed for Liberty packaged as a JAR file.

 ```bash
 $ ./gradlew install check -Druntime=<wlp|ol> -DruntimeVersion=<runtime_version> -DwlpLicense=<liberty_license_code>
 ```

## Usage

### Adding the plugin to the build script

Within your Gradle build script, you need to set up the classpath to include the Liberty Gradle plugin. You also need to define the Maven Central repository to find the plugin and its dependencies.

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
        classpath 'io.openliberty.tools:liberty-gradle-plugin:3.0-SNAPSHOT'
    }
}
```

To use the Liberty Gradle Plugin, include the following code in your build script:

```groovy
apply plugin: 'liberty'
```

## Plugin Configuration

See the [Liberty extension properties](docs/libertyExtensions.md#liberty-extension-properties) reference for the properties used to configure the Liberty plugin. See each task for additional configuration and examples.

## Tasks

The Liberty plugin provides the following tasks for your project:

| Task | Description |
| --------- | ------------ |
| [cleanDirs](docs/clean.md#clean-task) | Cleans the Liberty server logs, workarea, and applications folders.|
| [compileJsp](docs/compileJsp.md) | Compiles the JSP files from the src/main/webapp directory into the build/classes directory. |
| [deploy](docs/deploy.md#deploy-task) | Deploys one or more applications to a Liberty server. |
| [installFeature](docs/installFeature.md#installfeature-task) | Installs an additional feature to the Liberty runtime. |
| [installLiberty](docs/installLiberty.md#installliberty-task) | Installs the  Liberty runtime from a repository. |
| [libertyCreate](docs/libertyCreate.md#libertycreate-task) | Creates a Liberty server. |
| [libertyDebug](docs/libertyDebug.md) | Runs the Liberty server in the console foreground after a debugger connects to the debug port (default: 7777). |
| [libertyDev](docs/libertyDev.md) | Start a Liberty server in dev mode. |
| [libertyDump](docs/libertyDump.md#libertydump-task) | Dumps diagnostic information from the Liberty server into an archive. |
| [libertyJavaDump](docs/libertyJavaDump.md#libertyjavadump-task) | Dumps diagnostic information from the Liberty server JVM. |
| [libertyPackage](docs/libertyPackage.md#libertypackage-task) | Packages a Liberty server. |
| [libertyRun](docs/libertyRun.md#libertyrun-task) | Runs a Liberty server in the Gradle foreground process. |
| [libertyStart](docs/libertyStart.md#libertystart-task) | Starts the Liberty server in a background process. |
| [libertyStatus](docs/libertyStatus.md) | Checks to see if the Liberty server is running. |
| [libertyStop](docs/libertyStop.md#libertystop-task) | Stops the Liberty server. |
| [undeploy](docs/undeploy.md#undeploy-task) | Removes applications from the Liberty server. |
| [uninstallFeature](docs/uninstallFeature.md#uninstallfeature-task) | Remove a feature from the Liberty runtime. |

### Task ordering

The Liberty Gradle plugin defines a built-in task order to allow a user to call an end task without worrying about calling the necessary tasks in between. By having the plugin manage tasks and their order of execution we can easily avoid some simple human errors. For example, in order to have a majority of the tasks function, the principal task `installLiberty` must be called, which our plugin would do for you.  

The most appealing benefit from defining a task order is the ability to allow the user to call an end task directly. For example, if the user calls `libertyStart` out of the box, Gradle will recognize that it must call `installLiberty -> libertyCreate -> installFeature -> deploy` to get a server with features and apps properly running.

Click on a [task](#tasks) to view what it depends on.

## Extensions

Extensions are tasks that improve the compatibility or user experience of third party libraries used with Liberty. The `liberty-gradle-plugin` provides the following extensions:

| Extension | Description |
| --------- | ------------ |
| [configureArquillian](docs/configureArquillian.md) | Integrates `arquillian.xml` configuration for the Liberty Managed and Remote Arquillian containers in the `liberty-gradle-plugin`. Automatically configures required `arquillian.xml` parameters for the Liberty Managed container. |
