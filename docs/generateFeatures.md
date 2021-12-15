## generateFeatures task
---
Scan the class files of your application and create a file containing the Liberty features that it requires.

This task is available as a tech preview in the 3.3.1-SNAPSHOT. Please provide feedback by opening an issue at https://github.com/OpenLiberty/ci.gradle.

Lifecycle

This task is not part of the lifecycle so to use it in your build you will need to understand its dependencies. Since it will scan the class files of your application it must be run after you have run the compile task. The list of features that it generates will be used by the `libertyCreate` and the `installFeature` tasks so run this task first.

If the task detects features which are not already in the Liberty configuration it will create the file `configDropins/overrides/generated-features.xml` in the `src/main/liberty/config` section of your project. The task will add the necessary features which are not already in the configuration. If the `generated-features.xml` file has been created in the past but all the Liberty features used by the application are already specified elsewhere in the configuration this file will be retained and will contain a comment explaining this situation.

The task examines the `build.gradle` dependencies to determine what version of Java EE and what version of MicroProfile you may be using. It will then generate features which are compatible. For Java EE the task looks for coordinates:
* javax:javaee-api:7.0
* javax:javaee-api:8.0
* jakarta:jakartaee-api:8.0

For MicroProfile it looks for `org.eclipse.microprofile:microprofile` and generates features according to the version number. The task uses these compile dependencies to determine the best Liberty features to use with your application. 

The task also considers the features you have already specified in `server.xml` or other files that Liberty will use e.g. `include` elements and `configDropins` files. The task will attempt to find a set of features that are valid to work together.

If there are conflicts with features specified in Liberty configuration files or features used in the application code, the task will print an error message. If available, the task will also print a list of suggested features with no conflicts.

Limitations
 
* for MicroProfile this task will generate the latest features available in a given major release. E.g. even if you specify `org.eclipse.microprofile:microprofile:3.2` and you use mpHealth APIs this task will generate the feature `mpHealth-2.2`, the latest feature available for MicroProfile 3.x
* Jakarta version 9 is not supported at this time
* if you use the `serverXmlFile` parameter and specify a file not in the directory `src/main/liberty/config` and that file uses relative paths to include other files, any features in those files will not be considered in generation
* for the snapshot release any features accessed using property variables e.g. `${custom.key}/configFile.xml` are not considered in generation

For the snapshot release you must include the Sonatype repository in `build.gradle`:
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

Example:

Compile the application code and generate Liberty features.

* `gradle compileJava generateFeatures`

