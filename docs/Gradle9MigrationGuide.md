# Gradle 8 to Gradle 9 Migration Guide for Liberty Gradle Plugin Users

This guide provides step-by-step instructions for migrating your Liberty Gradle Plugin projects from Gradle 8 to Gradle 9. It focuses on the key changes that affect end users and provides practical examples to help you update your build scripts.

## Table of Contents

1. [Java Configuration Updates](#java-configuration-updates)
2. [Build Configuration Changes](#build-configuration-changes)
3. [Multi-Project Build Changes](#multi-project-build-changes)
4. [EAR/WAR/JAR Configuration Changes](#earwarjar-configuration-changes)
5. [Project Dependencies](#project-dependencies)
6. [Testing Considerations](#testing-considerations)
7. [Known Issues and Workarounds](#known-issues-and-workarounds)

## Java Configuration Updates

Gradle 9 requires updates to how you specify Java compatibility in your build scripts.

### Update Java Compatibility Settings

**Before (Gradle 8):**
```groovy
sourceCompatibility = 1.8
targetCompatibility = 1.8
```

**After (Gradle 9):**
```groovy
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
```

This change is required in all your build scripts that specify Java compatibility. The old style is deprecated in Gradle 9 and will be removed in future versions.

**Why this change is necessary:** The Java toolchain approach provides better compatibility with Gradle 9's toolchain support and allows for more flexible Java version management. It also enables clearer error messages when Java version requirements are not met.

**Reference Documentation:**
- [Gradle 9.0 Upgrading Guide - Java Toolchain](https://docs.gradle.org/current/userguide/upgrading_major_version_9.html#java_toolchain)
- [Java Plugin - Compatibility](https://docs.gradle.org/current/userguide/java_plugin.html#sec:java_cross_compilation)

## Build Configuration Changes

### Test Task Configuration

If you're using the test task in your build scripts, you may need to update your configuration for Gradle 9 compatibility:

**Before (Gradle 8):**
```groovy
test {
    // Test configuration
}
```

**After (Gradle 9):**
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
- [Gradle Test Task - failOnNoDiscoveredTests property](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html#org.gradle.api.tasks.testing.Test:failOnNoDiscoveredTests)
- [Gradle 9.0 Upgrading Guide - Test Task Changes](https://docs.gradle.org/current/userguide/upgrading_major_version_9.html#test_task_fails_when_no_tests_are_discovered)

### Liberty Server Task Ordering

Gradle 9 is more strict about task ordering and resource cleanup. If you're using Liberty server tasks in your build, ensure proper task ordering:

```groovy
// Ensure proper task ordering for Liberty server operations
libertyStop.mustRunAfter libertyStart
clean.mustRunAfter libertyStop
```

**Why this change is necessary:** Gradle 9 is more strict about task ordering and resource management. Without proper task ordering, you may encounter file locking issues when the clean task tries to delete files that are still in use by the Liberty server. This ensures that the server is properly stopped before any cleanup tasks run, preventing file locking issues.

**Reference Documentation:**
- [Gradle Task Ordering](https://docs.gradle.org/current/userguide/more_about_tasks.html#sec:ordering_tasks)
- [Gradle Task Execution](https://docs.gradle.org/current/userguide/build_lifecycle.html#sec:task_execution)

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
- [Gradle 9.0 Upgrading Guide - EAR and WAR plugins contribute all artifacts to the archives configuration](https://docs.gradle.org/current/userguide/upgrading_major_version_9.html#ear_and_war_plugins_contribute_all_artifacts_to_the_archives_configuration)
- [Gradle Configurations](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sec:what-are-dependency-configurations)

## EAR/WAR/JAR Configuration Changes

### EAR Project Configuration

If you're building EAR projects, update your EAR configuration to use the custom configurations:

**Before (Gradle 8):**
```groovy
ear {
    deploymentDescriptor {
        // EAR configuration
    }
    earlib project(path: ':myWebModule', configuration: 'archives')
}
```

**After (Gradle 9):**
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
- [Gradle 9.0 Upgrading Guide - EAR Plugin Changes](https://docs.gradle.org/current/userguide/upgrading_major_version_9.html#ear_and_war_plugins_contribute_all_artifacts_to_the_archives_configuration)
- [Gradle EAR Plugin](https://docs.gradle.org/current/userguide/ear_plugin.html)

### WAR Project Configuration

Similarly, update your WAR project configurations:

**Before (Gradle 8):**
```groovy
war {
    // WAR configuration
    classpath project(path: ':myLibModule', configuration: 'archives')
}
```

**After (Gradle 9):**
```groovy
war {
    // WAR configuration
    classpath project(path: ':myLibModule', configuration: 'jarOnly')
}
```

**Why this change is necessary:** Just like with EAR projects, the `archives` configuration in Gradle 9 now includes all artifacts. Using custom configurations ensures that only the intended artifacts are included in your WAR file.

**Reference Documentation:**
- [Gradle 9.0 Upgrading Guide - WAR Plugin Changes](https://docs.gradle.org/current/userguide/upgrading_major_version_9.html#ear_and_war_plugins_contribute_all_artifacts_to_the_archives_configuration)
- [Gradle WAR Plugin](https://docs.gradle.org/current/userguide/war_plugin.html)

## Project Dependencies

Gradle 9 has made significant changes to how project dependencies are handled, especially in multi-project builds.

### Project Dependency Configuration

When referencing project dependencies, you must now be explicit about which configuration to use:

**Before (Gradle 8):**
```groovy
dependencies {
    implementation project(':myLibProject')
}
```

**After (Gradle 9):**
```groovy
dependencies {
    implementation project(path: ':myLibProject', configuration: 'default')
}
```

**Why this change is necessary:** Gradle 9 is more strict about configuration resolution and requires explicit configuration references to avoid ambiguity. This change ensures that the correct dependencies are resolved and included in your project.

**Reference Documentation:**
- [Gradle 9.0 Upgrading Guide - Project Dependencies API Changes](https://docs.gradle.org/current/userguide/upgrading_major_version_9.html#project_dependencies_api_changes)
- [Gradle Project Dependencies](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sub:project_dependencies)

### Task Dependencies

If you have explicit task dependencies between projects, update them to be more specific:

**Before (Gradle 8):**
```groovy
ear.dependsOn ':myWebProject:jar', ':myWebProject:war'
```

**After (Gradle 9):**
```groovy
ear.dependsOn ':myWebProject:war'
```

**Why this change is necessary:** In Gradle 9, task dependencies should be more explicit to avoid unnecessary task execution and improve build performance. This change ensures that only the required tasks are executed.

**Reference Documentation:**
- [Gradle Task Dependencies](https://docs.gradle.org/current/userguide/more_about_tasks.html#sec:adding_dependencies_to_tasks)

### Dependency Resolution Strategy

If you're experiencing dependency resolution issues after migrating to Gradle 9, consider adding a resolution strategy:

```groovy
configurations.all {
    resolutionStrategy {
        // Force specific versions if needed
        force 'org.example:library:1.0.0'
        
        // Fail on version conflict
        failOnVersionConflict()
    }
}
```

**Why this change is necessary:** Gradle 9's stricter dependency resolution might expose version conflicts that were previously hidden. Adding a resolution strategy helps resolve these conflicts explicitly and ensures that the correct dependencies are used.

**Reference Documentation:**
- [Gradle Dependency Management](https://docs.gradle.org/current/userguide/dependency_management.html)

## Testing Considerations

### JSP Compilation Tests

If you're using JSP compilation in your projects, be aware that the Java source level may need to be updated:

**Before (Gradle 8):**
```groovy
compileJsp {
    jspVersion = '2.3'
    // JSP compilation configuration
}
```

**After (Gradle 9):**
```groovy
compileJsp {
    jspVersion = '2.3'
    // JSP compilation configuration will use the Java version from your project
}
```

**Why this change is necessary:** Gradle 9 has changed how Java source levels are handled, especially with the new Java toolchain approach. This change ensures that JSP compilation uses the correct Java source level from your project configuration.

### Test Output Verification

If your tests verify output messages from Liberty or Gradle tasks, update your verification code to check for key parts of messages instead of exact format:

**Before (Gradle 8):**
```groovy
assert output.contains("Expected exact message format")
```

**After (Gradle 9):**
```groovy
assert output.contains("Expected keyword")
assert output.contains("Another important term")
```

**Why this change is necessary:** Gradle 9 has changed how it formats output messages, including line breaks and indentation. Exact string matching is now brittle and prone to failure. Checking for key parts of messages is more robust against formatting changes.

**Reference Documentation:**
- [Gradle Testing](https://docs.gradle.org/current/userguide/java_testing.html)
- [Gradle Test Task](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html)

## Known Issues and Workarounds

### Arquillian Tests

If you're using the Arquillian framework with your Liberty projects, be aware that Gradle 9 is not fully supported with the Arquillian framework. You have two options:

1. Continue using Gradle 8.5 for projects that depend on Arquillian
2. If you must use Gradle 9, specify Gradle 8.5 for Arquillian-specific tasks:

```groovy
GradleRunner.create()
    .withProjectDir(buildDir)
    .forwardOutput()
    .withGradleVersion("8.5") // Specify compatible Gradle version for dependency management plugin
    .withArguments("build", "-x", "test", "-i", "-s")
    .build()
```

**Why this change is necessary:** The Arquillian framework has not been fully updated for Gradle 9 compatibility. Using Gradle 8.5 for Arquillian-specific tasks ensures that the tests can still run correctly while the rest of the project migrates to Gradle 9.

**Reference Documentation:**
- [Gradle TestKit](https://docs.gradle.org/current/userguide/test_kit.html#sub:gradle-runner-gradle-versionl)

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
- [Gradle Deprecation Handling](https://docs.gradle.org/current/userguide/feature_lifecycle.html#sec:deprecation_handling)

## Conclusion

By following this guide, you should be able to successfully migrate your Liberty Gradle Plugin projects from Gradle 8 to Gradle 9. The key changes involve:

1. Updating Java configuration syntax
2. Adjusting build configuration settings
3. Creating custom configurations for artifacts
4. Updating project dependency references
5. Modifying task dependencies
6. Updating test verification methods

If you encounter any issues not covered in this guide, please refer to the [Gradle 9 Release Notes](https://docs.gradle.org/9.0/release-notes.html) or the [Liberty Gradle Plugin documentation](https://github.com/OpenLiberty/ci.gradle).
