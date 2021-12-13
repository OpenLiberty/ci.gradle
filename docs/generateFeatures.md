## generateFeatures task
---
Scan the class files of your application and create a file containing the Liberty features that it requires.

Available in the 3.3.1-SNAPSHOT

This task will scan the class files of your application so you should run it after you have run the compile task. If the task detects features which are not already in the Liberty configuration it will create the file `configDropins/overrides/generated-features.xml`. The task will add the necessary features which are not already in the configuration.

The task examines the `build.gradle` dependencies to determine what version of Java EE and what version of MicroProfile you may be using. It will then generate features which are compatible. For Java EE the task looks for group IDs `io.openliberty.features` and `jakarta.platform` and one of the following artifact IDs:
* javaee-7.0
* javaeeClient-7.0
* javaee-8.0
* javaeeClient-8.0
* jakartaee-8.0
* jakarta.jakartaee-api (version 8.0.0)

For MicroProfile it looks for group ID `org.eclipse.microprofile` and artifact ID `microprofile` and generates features according to the version number. The task uses these compile dependencies to determine the best Liberty features to use with your application. 

Limitations: 
* for MicroProfile this goal will generate the latest features available in a given major release. E.g. even if you specify `org.eclipse.microprofile:microprofile:3.2` and you use mpHealth APIs this goal will generate the feature `mpHealth-2.2`, the latest feature available for MicroProfile 3.x
* Jakarta version 9 is not supported at this time

The task also considers the features you have already specified in `server.xml` or other files that Liberty will use e.g. `include` elements and `configDropins` files. The task will attempt to find a set of features that are valid to work together.

Note: for the snapshot release any features accessed using property variables e.g. `${custom.key}/configFile.xml` are not considered in generation.

If there are conflicts in the features among the Liberty configuration files or if they conflict with the features scanned from the application class files the task will print an error message. The task may also print a list of features which would work better when such a list is available.

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

