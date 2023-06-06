# ci.gradle [![Maven Central Latest](https://maven-badges.herokuapp.com/maven-central/io.openliberty.tools/liberty-gradle-plugin/badge.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22io.openliberty.tools%22%20AND%20a%3A%22liberty-gradle-plugin%22) [![Build Status](https://github.com/OpenLiberty/ci.gradle/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/OpenLiberty/ci.gradle/actions?branch=main)

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

### Gradle Support

The Liberty Gradle Plugin supports running with Gradle 7.3+ and Gradle 8.x as of release 3.6. The 7.3 version of Gradle is when full support for Java 17 was introduced. When using a Gradle wrapper, ensure the wrapper version matches the version of Gradle being used.

### Java Support

The Liberty Gradle Plugin is tested with Long-Term-Support (LTS) releases of Java. The plugin, as of release 3.5, supports Java 8, 11 and 17. Prior to this version, the plugin is supported on Java 8 and 11.

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
        classpath 'io.openliberty.tools:liberty-gradle-plugin:3.6.1'
    }
}
```

To use the Liberty Gradle Plugin, include the following code in your build script:

```groovy
apply plugin: 'liberty'
```

Alternatively, you can apply the plugin through the `plugins` block. You'll need to add the plugin's runtime dependencies to the buildscript classpath when using this method.

```groovy
buildscript {
    repositories {
        mavenCentral()
        mavenLocal()
    }
    dependencies {
        classpath ('io.openliberty.tools:liberty-ant-tasks:1.9.12')
        classpath ('io.openliberty.tools:ci.common:1.8.25')
    }
}

plugins {
    id "io.openliberty.tools.gradle.Liberty" version "3.6.1"
}
```

## Plugin Configuration

### Liberty Installation Configuration

The Liberty Gradle Plugin must first be configured with the Liberty server installation information. The installation information can be specified as:

* A [Maven artifact](docs/installLiberty.md#using-maven-artifact)
* An [existing installation directory](docs/libertyExtensions.md#general-runtime-properties) - reference the `installDir` property
* A Liberty server from a [Liberty repository or other location](docs/installLiberty.md#install-block)

Installing from a Maven artifact is the default installation method. The default runtime artifact is the latest version of `io.openliberty:openliberty-kernel`. 

Example using `libertyRuntime` property to install an Open Liberty beta runtime:

```groovy
dependencies {
    libertyRuntime group: 'io.openliberty.beta', name: 'openliberty-runtime', version: '23.0.0.2-beta'
}
```

Example using `libertyRuntime` property to install a specific Open Liberty runtime version:

```groovy
dependencies {
    libertyRuntime group: 'io.openliberty', name: 'openliberty-kernel', version: '23.0.0.3'
}
```

In order to configure WebSphere Liberty for installation, specify the `libertyRuntime` with the `com.ibm.websphere.appserver.runtime` group and the specific `name` and `version` that is needed. For a full list of artifacts available, see the [installLiberty task](docs/installLiberty.md#using-maven-artifact) documentation. 

Example using the `libertyRuntime` property to install a WebSphere Liberty runtime from a Maven artifact:

```groovy
dependencies {
    libertyRuntime group: 'com.ibm.websphere.appserver.runtime', name: 'wlp-webProfile8', version: '22.0.0.12'
}
```

### Additional Configuration

See the [Liberty extension properties](docs/libertyExtensions.md#liberty-extension-properties) reference for the properties used to configure the Liberty plugin. See each task for additional configuration and examples.

## Tasks

The Liberty plugin provides the following tasks for your project:

| Task                                                               | Description |
|--------------------------------------------------------------------| ------------ |
| [cleanDirs](docs/clean.md#clean-task)                              | Cleans the Liberty server logs, workarea, and applications folders.|
| [compileJsp](docs/compileJsp.md)                                   | Compiles the JSP files from the src/main/webapp directory into the build/classes directory. |
| [deploy](docs/deploy.md#deploy-task)                               | Deploys one or more applications to a Liberty server. |
| [generateFeatures](docs/generateFeatures.md#generateFeatures-task) | Scan the class files of an application and create a Liberty configuration file in the source configuration directory that contains the Liberty features the application requires.* |
| [installFeature](docs/installFeature.md#installfeature-task)       | Installs an additional feature to the Liberty runtime. |
| [installLiberty](docs/installLiberty.md#installliberty-task)       | Installs the  Liberty runtime from a repository. |
| [libertyCreate](docs/libertyCreate.md#libertycreate-task)          | Creates a Liberty server. |
| [libertyDebug](docs/libertyDebug.md)                               | Runs the Liberty server in the console foreground after a debugger connects to the debug port (default: 7777). |
| [libertyDev](docs/libertyDev.md)                                   | Start a Liberty server in dev mode.* |
| [libertyDevc](docs/libertyDev.md#libertydevc-task-container-mode)  | Start a Liberty server in dev mode in a container.* |
| [libertyDump](docs/libertyDump.md#libertydump-task)                | Dumps diagnostic information from the Liberty server into an archive. |
| [libertyJavaDump](docs/libertyJavaDump.md#libertyjavadump-task)    | Dumps diagnostic information from the Liberty server JVM. |
| [libertyPackage](docs/libertyPackage.md#libertypackage-task)       | Packages a Liberty server. |
| [libertyRun](docs/libertyRun.md#libertyrun-task)                   | Runs a Liberty server in the Gradle foreground process. |
| [libertyStart](docs/libertyStart.md#libertystart-task)             | Starts the Liberty server in a background process. |
| [libertyStatus](docs/libertyStatus.md)                             | Checks to see if the Liberty server is running. |
| [libertyStop](docs/libertyStop.md#libertystop-task)                | Stops the Liberty server. |
| [prepareFeature](docs/prepareFeature.md#prepareFeature-task)       | Prepares a user feature for installation to the Liberty runtime. |
| [undeploy](docs/undeploy.md#undeploy-task)                         | Removes applications from the Liberty server. |
| [uninstallFeature](docs/uninstallFeature.md#uninstallfeature-task) | Remove a feature from the Liberty runtime. |

*The `libertyDev`, `libertyDevc`, and `generateFeatures` tasks have a runtime dependency on IBM WebSphere Application Server Migration Toolkit for Application Binaries, which is separately licensed under IBM License Agreement for Non-Warranted Programs. For more information, see the [license](https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/license/wamt).
Note:  The `libertyDev` and `libertyDevc` tasks have this dependency only when auto-generation of features is turned on. By default, auto-generation of features is turned off.

### Task ordering

The Liberty Gradle plugin defines a built-in task order to allow a user to call an end task without worrying about calling the necessary tasks in between. By having the plugin manage tasks and their order of execution we can easily avoid some simple human errors. For example, in order to have a majority of the tasks function, the principal task `installLiberty` must be called, which our plugin would do for you.  

The most appealing benefit from defining a task order is the ability to allow the user to call an end task directly. For example, if the user calls `libertyStart` out of the box, Gradle will recognize that it must call `installLiberty -> libertyCreate -> installFeature -> deploy` to get a server with features and apps properly running.

Click on a [task](#tasks) to view what it depends on.

## Extensions

Extensions are tasks that improve the compatibility or user experience of third party libraries used with Liberty. The `liberty-gradle-plugin` provides the following extensions:

| Extension | Description |
| --------- | ------------ |
| [configureArquillian](docs/configureArquillian.md) | Integrates `arquillian.xml` configuration for the Liberty Managed and Remote Arquillian containers in the `liberty-gradle-plugin`. Automatically configures required `arquillian.xml` parameters for the Liberty Managed container. |
| [Spring Boot Support](docs/spring-boot-support.md#spring-boot-support) | The Liberty Gradle Plugin supports thinning and installing Spring Boot applications onto the Liberty server. |
