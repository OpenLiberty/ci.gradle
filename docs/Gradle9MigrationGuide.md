# Gradle 8 to Gradle 9 Migration Guide for Liberty Gradle Plugin Users

This guide provides step-by-step instructions for migrating your projects that use the Liberty Gradle Plugin from Gradle 8 to Gradle 9. It focuses on the key changes that affect end users and provides practical examples to help you update your build scripts.

## Gradle 9 Prerequisites
Achieving compatibility with Gradle 9 requires the following updates:

1. Java Requirement: The minimum supported version is Java 17. Java 21 and Java 25 are also supported via toolchains.

2. Gradle Version: We recommend Gradle 9.1.0 or later (9.0.0 does not support Java 25).

3. Kotlin Update: An upgrade to Kotlin 2.0 is mandatory.

Please go through [gradle official documentation](https://docs.gradle.org/9.1.0/userguide/upgrading_major_version_9.html#changes_major_9) for details.

## Table of Contents

1. [Compatibility Requirements](#compatibility_requirements)
2. [Java Configuration Updates](#java-configuration-updates)
3. [Java Toolchain and Foojay Resolver](#java-toolchain-and-foojay-resolver)
4. [Build Configuration Changes](#build-configuration-changes)
5. [Multi-Project Build Changes](#multi-project-build-changes)
6. [EAR/WAR/JAR Configuration Changes](#earwarjar-configuration-changes)
7. [Project Dependencies](#project-dependencies)
8. [Known Issues and Workarounds](#known-issues-and-workarounds)

## Compatibility Requirements


To ensure a successful migration, please note the following version requirements:

* **Liberty Gradle Plugin 4.0.0+**: Starting with version 4.0.0, the Liberty Gradle Plugin requires Gradle 9.0 or later.

* **Java Requirement**: Gradle 9 requires Java 17 as the minimum runtime version. Java 21 and 25 are supported via toolchains.

* **Kotlin Update**: If your build uses Kotlin, an upgrade to Kotlin 2.0 is mandatory.

* **Recommended Version**: We recommend using Gradle 9.1.0 or later to ensure full support for Java 25.

Note: If you must remain on Gradle 8 or earlier, you must use a **3.x** version of Liberty Gradle Plugin. We recommend using the latest **3.x** version of Liberty Gradle Plugin available.

## Java Configuration Updates

Gradle 9 requires updates to how you specify Java compatibility in your build scripts.

### Update Java Compatibility Settings

**Before:**
```groovy
sourceCompatibility = 1.8
targetCompatibility = 1.8
```

**After:**
```groovy
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
```

This change is required in all your build scripts that specify Java compatibility. The old style is deprecated in Gradle 9 and will be removed in future versions.

**Why this change is necessary:** The Java toolchain approach provides better compatibility with Gradle 9's toolchain support and allows for more flexible Java version management. It also enables clearer error messages when Java version requirements are not met.

**Reference Documentation:**
- [Toolchains for JVM projects](https://docs.gradle.org/9.0.0/userguide/toolchains.html#toolchains)
- [Java Plugin - Compatibility](https://docs.gradle.org/current/userguide/java_plugin.html#toolchain_and_compatibility)

## Java Toolchain and Foojay Resolver

Gradle 9 has improved support for [Java toolchains](https://docs.gradle.org/current/userguide/toolchains.html). If your project needs to build or run with a Java version different from the one running Gradle (e.g., Java 21 or Java 25), configure a toolchain in your `build.gradle`:

```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
```

To allow Gradle to automatically download the required JDK when it is not installed locally, add the [Foojay Toolchain Resolver](https://github.com/gradle/foojay-toolchains) plugin to your `settings.gradle`:

```groovy
plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0'
}
```

**Why this is important:** Without the foojay resolver, Gradle will fail if the requested toolchain JDK is not already installed on the machine. This is especially relevant for CI environments and for projects that target Java 21 or Java 25.

## Build Configuration Changes

### Test Task Configuration

If you're using the test task in your build scripts, you may need to update your configuration for Gradle 9 compatibility:

**Before:**
```groovy
test {
    // Test configuration
}
```

**After:**
```groovy
test {
    // Add this line if you have conditional tests that might not always be present
    failOnNoDiscoveredTests = false
    
    // Your existing test configuration
}
```

**Why this change is necessary:** In Gradle 9, the `failOnNoDiscoveredTests` property is set to `true` by default. This means your build will fail if your test task doesn't find any tests to run. This can be problematic if you have:

- Conditional tests that might not always be present
- Test filtering that sometimes results in no tests
- Multi-project builds where some projects might not have tests

Setting `failOnNoDiscoveredTests = false` allows your build to continue even when no tests are found in a particular module or configuration.

**Reference Documentation:**
- [Gradle Test Task - failOnNoDiscoveredTests property](https://docs.gradle.org/9.0.0/dsl/org.gradle.api.tasks.testing.Test.html#org.gradle.api.tasks.testing.Test:failOnNoDiscoveredTests)
- [Gradle 9.0 Upgrading Guide - Test Task Changes](https://docs.gradle.org/9.0.0/userguide/upgrading_major_version_9.html#test_task_fails_when_no_tests_are_discovered)

### Liberty Server Task Ordering

Gradle 9 is more strict about task ordering and resource cleanup. If you're using Liberty server tasks in your build, ensure proper task ordering:

```groovy
// Ensure proper task ordering for Liberty server operations
// Groovy-style syntax
libertyStop.mustRunAfter libertyStart
clean.mustRunAfter libertyStop

// Java-style syntax
libertyStop.mustRunAfter(libertyStart)
clean.mustRunAfter(libertyStop)
```

**Why this change is necessary:** Gradle 9 is more strict about task ordering and resource management. Without proper task ordering, you may encounter file locking issues when the clean task tries to delete files that are still in use by the Liberty server. This ensures that the server is properly stopped before any cleanup tasks run, preventing file locking issues.

**Reference Documentation:**
- [Gradle Task Ordering](https://docs.gradle.org/9.0.0/userguide/controlling_task_execution.html#sec:ordering_tasks)
- [Gradle Understanding Tasks](https://docs.gradle.org/9.0.0/userguide/more_about_tasks.html)

### JSP Compilation

When using JSP compilation in Gradle 9, the compiler will automatically use the Java version specified in your project's configuration. No specific changes are needed to your JSP compilation tasks beyond updating the Java compatibility syntax as described in the [Java Configuration Updates](#java-configuration-updates) section.

## Multi-Project Build Changes

Gradle 9 introduces significant changes to how artifacts and configurations are handled in multi-project builds.

### Custom Configurations for Artifacts

In Gradle 9, the EAR and WAR plugins now contribute **all** artifacts to the `archives` configuration. This is a change from Gradle 8, where applying the EAR plugin excluded JAR and WAR artifacts from archives.

To maintain the same behavior as in Gradle 8, create custom configurations for your artifacts:

```groovy
// Create custom configurations for your artifacts
configurations {
    warOnly
    jarOnly
    earOnly
}

// Add artifacts to these configurations
artifacts {
    warOnly war
    jarOnly jar
    earOnly ear
}
```

**Why this change is necessary:** In Gradle 9, the behavior of the EAR and WAR plugins has changed to include all artifacts in the `archives` configuration. This can lead to unexpected artifacts being included in your builds. Creating custom configurations gives you precise control over which artifacts are included in your EAR/WAR files and avoids including unnecessary transitive dependencies.

**Reference Documentation:**
- [Gradle 9.0 Upgrading Guide - EAR and WAR plugins contribute all artifacts to the archives configuration](https://docs.gradle.org/9.0.0/userguide/upgrading_major_version_9.html#ear_and_war_plugins_contribute_all_artifacts_to_the_archives_configuration)
- [Gradle Configurations](https://docs.gradle.org/9.0.0/userguide/declaring_dependencies.html#sec:what-are-dependency-configurations)

## EAR/WAR/JAR Configuration Changes

### EAR Project Configuration

If you're building EAR projects, update your EAR configuration to use the custom configurations:

**Before:**
```groovy
ear {
    deploymentDescriptor {
        // EAR configuration
    }
    earlib project(path: ':myWebModule', configuration: 'archives')
}
```

**After:**
```groovy
ear {
    deploymentDescriptor {
        // EAR configuration
    }
    earlib project(path: ':myWebModule', configuration: 'warOnly')
}
```

**Why this change is necessary:** In Gradle 9, the `archives` configuration now includes all artifacts, which might lead to unexpected artifacts being included in your EAR file. Using custom configurations ensures that only the intended artifacts are included.

**Reference Documentation:**
- [Gradle 9.0 Upgrading Guide - EAR Plugin Changes](https://docs.gradle.org/9.0.0/userguide/upgrading_major_version_9.html#ear_and_war_plugins_contribute_all_artifacts_to_the_archives_configuration)
- [Gradle EAR Plugin](https://docs.gradle.org/9.0.0/userguide/ear_plugin.html)

### WAR Project Configuration

Similarly, update your WAR project configurations:

**Before:**
```groovy
war {
    // WAR configuration
    classpath project(path: ':myLibModule', configuration: 'archives')
}
```

**After:**
```groovy
war {
    // WAR configuration
    classpath project(path: ':myLibModule', configuration: 'jarOnly')
}
```

**Why this change is necessary:** Just like with EAR projects, the `archives` configuration in Gradle 9 now includes all artifacts. Using custom configurations ensures that only the intended artifacts are included in your WAR file.

**Reference Documentation:**
- [Gradle 9.0 Upgrading Guide - WAR Plugin Changes](https://docs.gradle.org/9.0.0/userguide/upgrading_major_version_9.html#ear_and_war_plugins_contribute_all_artifacts_to_the_archives_configuration)
- [Gradle WAR Plugin](https://docs.gradle.org/9.0.0/userguide/war_plugin.html)

## Project Dependencies

Gradle 9 has made significant changes to how project dependencies are handled, especially in multi-project builds.

### Project Dependency Configuration

When referencing project dependencies, you must now be explicit about which configuration to use:

**Before:**
```groovy
dependencies {
    implementation project(':jar')
}
```

**After:**
```groovy
dependencies {
    implementation project(path: ':jar', configuration: 'jarOnly')
}
```

**Why this change is necessary:** Gradle 9 is more strict about configuration resolution and requires explicit configuration references to avoid ambiguity. This change ensures that the correct dependencies are resolved and included in your project.

**Reference Documentation:**
- [Gradle 9.0 Upgrading Guide - EAR and WAR plugins contribute all artifacts to the archives configuration](https://docs.gradle.org/9.0.0/userguide/upgrading_major_version_9.html#ear_and_war_plugins_contribute_all_artifacts_to_the_archives_configuration)
- [Gradle Project Dependencies](https://docs.gradle.org/9.0.0/userguide/declaring_dependencies.html#2_project_dependencies)

### Task Dependencies

If you have explicit task dependencies between projects, update them to be more specific:

**Before:**
```groovy
ear.dependsOn ':myWebProject:jar', ':myWebProject:war'
```

**After:**
```groovy
ear.dependsOn ':myWebProject:war'
```

**Why this change is necessary:** In Gradle 9, task dependencies should be more explicit to avoid unnecessary task execution and improve build performance. This change ensures that only the required tasks are executed.

**Reference Documentation:**
- [Gradle Task Dependencies](https://docs.gradle.org/9.0.0/userguide/controlling_task_execution.html#sec:more_task_dependencies)

### Dependency Resolution Considerations

While migrating to Gradle 9, you may encounter dependency resolution issues due to Gradle 9's stricter dependency management. If you experience version conflicts or unexpected dependency behavior, refer to the following resources:

**Reference Documentation:**
- [Gradle Dependency Resolution](https://docs.gradle.org/9.0.0/userguide/dependency_resolution.html)
- [Gradle Dependency Constraints](https://docs.gradle.org/9.0.0/userguide/dependency_constraints.html)
- [Gradle Dependency Management Techniques](https://docs.gradle.org/9.0.0/userguide/dependency_management_for_java_projects.html)

## Known Issues and Workarounds

### Arquillian Tests

Arquillian support has not been validated with Gradle 9. If your project uses the Arquillian framework, continue using Gradle 8.x until compatibility is confirmed.

### Spring Boot Applications

**Spring Boot 3.x** is supported with Gradle 9 when using Java 17 or later. To ensure compatibility, update your Spring Boot project build files as follows:

1. Set `sourceCompatibility` and `targetCompatibility` to `JavaVersion.VERSION_17` (or later) using the `java {}` block syntax.
2. Use Spring Boot 3.4.1 or later.

```groovy
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
```

**Spring Boot 2.x** is not supported with Gradle 9. If your project depends on Spring Boot 2.x, continue using Gradle 8.x.

### File Locking Issues

If you encounter file locking issues during the build, especially when stopping Liberty servers and then trying to clean directories, add a small delay between stopping the server and cleaning:

```groovy
libertyStop.doLast {
    // Add a small delay to ensure file locks are fully released
    sleep(2000)
}
```

**Why this change is necessary:** Sometimes file locks aren't immediately released when stopping the Liberty server. Adding a small delay ensures that all file locks are fully released before any cleanup tasks run, preventing file locking issues.

### Deprecation Warnings

Gradle 9 shows deprecation warnings that will make it incompatible with Gradle 10. These warnings are informational at this point but will need to be addressed in future updates.

**Why this change is necessary:** Being aware of deprecation warnings helps you prepare for future Gradle versions. These warnings indicate features or APIs that will be removed in Gradle 10, so it's important to address them in future updates.

**Reference Documentation:**
- [Gradle Feature Lifecycle](https://docs.gradle.org/9.0.0/userguide/feature_lifecycle.html)

## Conclusion

This guide covers the most common changes needed when migrating from Gradle 8 to Gradle 9. While we've addressed the major compatibility issues, you may encounter project-specific challenges during your migration.

If you experience issues not covered in this guide:

1. Check the [Gradle 9.1 Release Notes](https://docs.gradle.org/9.1.0/release-notes.html) for additional information.
2. Review the [Gradle 9.1 Upgrading Guide](https://docs.gradle.org/9.1.0/userguide/upgrading_major_version_9.html) for more detailed explanations.
3. Consult the [Liberty Gradle Plugin documentation](https://github.com/OpenLiberty/ci.gradle) for Liberty-specific guidance.
4. Report issues or request help through our [GitHub Issues](https://github.com/OpenLiberty/ci.gradle/issues) if you encounter problems that aren't addressed by the documentation.

We're committed to supporting your migration to Gradle 9 and will continue to update this guide based on community feedback.
