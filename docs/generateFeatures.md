## generateFeatures task
---
Scan the class files of an application and create a new `generated-features.xml` Liberty configuration file containing the features the application requires.

This feature is best accessed through [dev mode](libertyDev.md). When you start `libertyDev` your application will be compiled and the class files will be scanned to verify that all the required Liberty features are included in your server configuration. Then as you work, dev mode will continue to monitor the project to confirm the Liberty features configured are up to date. If you implement a new interface in Java, the scanner will determine if that API is connected to a Liberty feature, then update the configuration and install the feature. If you remove a feature from `server.xml`, dev mode will determine if that feature is actually necessary, and if so, add it to the generated configuration file as described below.

Feature generation is enabled through dev mode by default. If you need to disable feature generation, you can start dev mode with the parameter `--generateFeatures=false`. When running dev mode, you can toggle the generation of features off and on by typing 'g' and pressing Enter. Normally dev mode only scans a class file that has just been updated, but you can tell dev mode to rescan all class files by typing 'o' and pressing Enter. This will optimize the feature list in the generated configuration file.

##### Lifecycle

This task is not part of the lifecycle, so to use it in your build you will need to understand its dependencies. Since it will scan the class files of your application, it must be run after the `compileJava` task. The list of features that it generates will be used by the `libertyCreate` and the `installFeature` tasks, so run this task first.

If this task detects Liberty features used in your project but not present in your Liberty configuration, it will create a new file `configDropins/overrides/generated-features.xml` in the `src/main/liberty/config` directory of your project. The `generated-features.xml` file will contain a list of features required for your project. If the `generated-features.xml` file has been created in the past and no additional features have been detected, this file will be retained.

If you are using [devc](libertyDev.md#libertydevc-task-container-mode), ensure that the `generated-features.xml` configuration file is copied via your Dockerfile.
```dockerfile
COPY --chown=1001:0  build/wlp/usr/servers/defaultServer/configDropins/overrides/generated-features.xml /config/configDropins/overrides/
```

The task examines the `build.gradle` dependencies to determine which version of Jakarta EE, MicroProfile or Java EE API you may be using. Compatible features will then be generated.

For Jakarta EE API, this task looks for `jakarta:jakartaee-api` dependency with version `8.0`.

For MicroProfile API, this task looks for a `org.eclipse.microprofile:microprofile` dependency and generates features according to the version number.

For Java EE API, this task looks for a `javax:javaee-api` dependency with versions `6.0`, `7.0` or `8.0`.

For example, if you have the following Jakarta EE and MicroProfile dependencies in your `build.gradle` file, features compatible with Jakarta EE `8.0` and MicroProfile `4.1` will be generated.
```groovy
dependencies {
    providedCompile 'jakarta.platform:jakarta.jakartaee-api:8.0.0'
    providedCompile 'org.eclipse.microprofile:microprofile:4.1'
}
```

This task also considers the features you have already specified in `server.xml` or other Liberty server configuration files (e.g. `include` elements and `configDropins` files). This task will attempt to find a working set of features that are compatible with each other.

If there are conflicts with features specified in Liberty configuration files or features used in the application code, this task will print an error message. If available, this task will also print a list of suggested features with no conflicts.

##### Example (outside of dev mode):

Compile the application code and generate Liberty features.
* `gradle compileJava generateFeatures`

##### Limitations

* MicroProfile 5 is not supported at this time
* Jakarta EE version 9 or 9.1 is not supported at this time
* When using the `serverXmlFile` parameter in the `build.gradle` file, if you specify a file not in the directory `src/main/liberty/config` and that file uses relative paths to include other files, any features in those files will not be considered for feature generation
* Any features accessed using property variables (e.g. `${custom.key}/configFile.xml`) are not considered for feature generation

See issues tagged with [`generateFeatures`](https://github.com/OpenLiberty/ci.gradle/issues?q=is%3Aissue+is%3Aopen+label%3AgenerateFeatures) for further information.
