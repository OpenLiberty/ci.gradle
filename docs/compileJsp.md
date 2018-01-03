## compileJsp task
---
The `compileJsp` task compiles the JSP files in the src/main/webapp directory so that they can be packaged with the application.

### dependsOn
`compileJsp` depends on `installLiberty` and `compileJava`.

### Parameters

See the [Liberty general runtime properties.](libertyExtensions.md#general-runtime-properties) properties for common server configuration.

In particular, the jspVersion and the jspCompileTimeout properties are used for compiling JSP files.

### Example:

```groovy
apply plugin: 'liberty'
apply plugin: 'war'

liberty {
    server {
      configFile = file("src/resources/server.xml")
      stripVersion = true
      jspVersion = 2.2
      jspCompileTimeout = 35
    }
}

installApps.dependsOn 'war'
war.dependsOn 'compileJSP'
libertyStart.dependsOn 'installApps'

```
