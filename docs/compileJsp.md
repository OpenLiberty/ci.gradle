## compileJsp task
---
The `compileJsp` task compiles the JSP files in the src/main/webapp directory so that they can be packaged with the application. The Java version used for the compilation comes from either the `release` attribute on the `compileJava` task or the `sourceCompatibility` property with the first taking precedence.

Note: As of Liberty version 24.0.0.1, this task only works with Long Term Service (LTS) releases of Java. See the [documentation](https://openliberty.io/docs/latest/reference/config/jspEngine.html) for the valid values for the `javaSourceLevel` attribute on the `jspEngine` configuration element. Prior to version 24.0.0.1, the `jdkSourceLevel` attribute was used on the `jspEngine` [element](https://openliberty.io/docs/23.0.0.12/reference/config/jspEngine.html) and only supported up to and including Java 8 (specified as 18).

### dependsOn
`compileJsp` depends on `installLiberty` and `compileJava`.

### Parameters

| Attribute | Type | Since | Description | Required |
| --------- | ---- | ----- | ----------- | ---------|
| jspVersion | int | 2.0 | Sets the JSP version to use. Valid values are `2.2` or `2.3`. The default value is `2.3`. | No |
| jspCompileTimeout | int | 2.0 | Maximum time to wait (in seconds) for all the JSP files to compile. The server is stopped and the goal ends after this specified time. The default value is 30 seconds. | No |

The `jspVersion` and `jspCompileTimeout` properties are set in the `liberty.jsp` closure.

### Example:

```groovy
apply plugin: 'liberty'
apply plugin: 'war'

liberty {
    server {
        serverXmlFile = file("src/resources/server.xml")
        stripVersion = true
    }
    jsp {
        jspVersion = 2.2
        jspCompileTimeout = 35
    }
}

deploy.dependsOn 'war'
war.dependsOn 'compileJSP'
libertyStart.dependsOn 'deploy'

```
