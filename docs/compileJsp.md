## compileJsp task
---
The `compileJsp` task compiles the JSP files in the src/main/webapp directory so that they can be packaged with the application.

### dependsOn
`compileJsp` depends on `installLiberty` and `compileJava`.

### Parameters

| Attribute | Type | Since | Description | Required |
| --------- | ---- | ----- | ----------- | ---------|
| jspVersion | int | 2.0 | Maximum time to wait (in seconds) for all the JSP files to compile. The server is stopped and the goal ends after this specified time. The default value is 30 seconds. | No |
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
