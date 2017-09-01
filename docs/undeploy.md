## undeploy task

The `undeploy` task supports undeployment of one or more applications from the WebSphere Liberty server.

### Properties

See the [Liberty server configuration](libertyExtensions.md#liberty-server-configuration) properties for common server configuration.

The `undeploy` task uses a `undeploy` block within the `liberty` block to define task specific behavior.

| Attribute | Type  | Since | Description | Required |
| --------- | ----- | ----- | ----------- | -------- |
| application| String | 1.0 | Name of the application to be undeployed.| Yes, only when a single application will be undeployed. |
| include| String | 1.0| Comma- or space-separated list of patterns of files that must be included. All files are included when omitted.| No |
| exclude| String | 1.0 | Comma- or space-separated list of patterns of files that must be excluded. No files are excluded when omitted.| No |

### Examples

1. Undeploys a single application.
  ```groovy
    apply plugin: 'liberty'

    liberty {

        server {
            name = 'myServer'

            undeploy {
                application = 'app.war'
            }
        }
    }
  ```

2. Undeploy multiple applications. Specifically, undeploy `app.war` and `sample.war` but exclude `test-war.war`.
  ```groovy
    apply plugin: 'liberty'

    liberty {

        installDir = 'c:/wlp'
        serverName = 'myServer'

        undeploy {
            include = 'app.war, sample.war'
            exclude = 'test-war.war'
        }
    }
  ```

If no property is set for the `undeploy` block, but the EAR or WAR plugin is being used and their properties `destinationDir` and `archiveName` are declared, this will be the application that will be undeployed. Otherwise, all the applications available in the server at the moment of the execution will be undeployed.
