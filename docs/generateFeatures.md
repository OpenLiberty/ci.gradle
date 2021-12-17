## generateFeatures task
---
Scan the class files of your application and create a new Liberty configuration file containing the features your application requires.

This task is available as a tech preview in the 3.3.1-SNAPSHOT. Please provide feedback by opening an issue at https://github.com/OpenLiberty/ci.gradle.

This feature is best accessed through dev mode. When you start up `libertyDev` it will compile your application and scan the files to verify that all the required Liberty features are part of your configuration. Then as you work, dev mode will continue to monitor the project to confirm the Liberty features are up to date. If you implement a new interface in Java, the scanner will determine if that API is connected to a Liberty feature, then update the configuration and install the feature. If you remove a feature from `server.xml`, dev mode will determine if that feature is actually necessary, and if so, add it to the configuration file described below. For this snapshot you need to add the Sonatype repository to `build.gradle` (shown below), but in the future all the dependencies will be in Maven Central.

If you need to disable feature generation you can use the parameter `--generateFeatures=false`.

##### Lifecycle

This task is not part of the lifecycle, so to use it in your build you will need to understand its dependencies. Since it will scan the class files of your application, it must be run after the `compileJava` task. The list of features that it generates will be used by the `libertyCreate` and the `installFeature` tasks, so run this task first.

If this task detects Liberty features used in your project but not present in your Liberty configuration, it will create a new file `configDropins/overrides/generated-features.xml` in the `src/main/liberty/config` directory of your project. The `generated-features.xml` file will contain a list of features required for your project. If the `generated-features.xml` file has been created in the past and no additional features have been detected, this file will be retained and will contain a comment indicating that there are no additional features generated.

The task examines the `build.gradle` dependencies to determine what version of Java EE and what version of MicroProfile you may be using. It will then generate features which are compatible. For Java EE the task looks for coordinates:
* javax:javaee-api:6.0
* javax:javaee-api:7.0
* javax:javaee-api:8.0
* jakarta:jakartaee-api:8.0

For MicroProfile it looks for `org.eclipse.microprofile:microprofile` and generates features according to the version number. The task uses these compile dependencies to determine the best Liberty features to use with your application. 

The task also considers the features you have already specified in `server.xml` or other files that Liberty will use (e.g. `include` elements and `configDropins` files). The task will attempt to find a set of features that are valid to work together.

If there are conflicts with features specified in Liberty configuration files or features used in the application code, the task will print an error message. If available, the task will also print a list of suggested features with no conflicts.

##### Tech Preview Limitations
 
* For MicroProfile, this task will generate the latest features available in a given major release. (e.g. even if you specify `org.eclipse.microprofile:microprofile:3.2` and you use mpHealth APIs this task will generate the feature `mpHealth-2.2`, which is the latest version available for MicroProfile 3.x)
* Jakarta version 9 is not supported at this time
* When using the `serverXmlFile` parameter in build.gradle, if you specify a file not in the directory `src/main/liberty/config` and that file uses relative paths to include other files, any features in those files will not be considered for feature generation
* Any features accessed using property variables (e.g. `${custom.key}/configFile.xml`) are not considered for feature generation

For the tech preview snapshot you must include the Sonatype repository in `build.gradle`:
```
buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            url "https://plugins.gradle.org/m2"
            url "https://oss.sonatype.org/content/repositories/snapshots"
        }
    }
}
```

##### Example (outside of dev mode):

Compile the application code and generate Liberty features.

* `gradle compileJava generateFeatures`

