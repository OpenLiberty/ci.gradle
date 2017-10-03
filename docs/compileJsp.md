## InstallApps task
---
The `compileJsp` task compiles the JSP files in the src/main/webapp directory. This goal relies on a running server, so a Liberty server must be configured.

### Parameters

See the Liberty server configuration properties for common server configuration.

In particular, the jspVersion and the timeout properties are used for compiling JSP files.

### Example:

```groovy
apply plugin: 'liberty'
apply plugin: 'war'

liberty {
    server {
        ...
    }
}

installApps.dependsOn 'war'
war.dependsOn 'compileJSP'
libertyStart.dependsOn 'installApps'

```
