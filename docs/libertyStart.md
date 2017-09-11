## libertyStart task  
Start a Liberty server in the background. The server instance is automatically created if it does not exist.  

### Parameters

See the [Liberty server configuration](libertyExtensions.md#liberty-server-configuration) properties for common server configuration.

### Example
`clean` is set to `false` by default unless specified in `build.gradle` as shown in this example.  
To set up verification of applications installed from `installApps`, set `verifyAppStartTimeout` to how many seconds the server will wait in the message logs.

```groovy
apply plugin: 'liberty'

liberty {
    server {
        name = 'myServer'
        
        // Clean logs, workarea, apps, dropins on server startup 
        clean = true
        
        // Wait 30 seconds to verify application start
        verifyAppStartTimeout = 30
    }
}

```
