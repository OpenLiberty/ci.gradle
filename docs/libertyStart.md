## libertyStart task  
Start a Liberty server in the background. The server instance is automatically created if it does not exist.  

### Parameters

See the [Liberty server configuration](libertyExtensions.md#liberty-server-configuration) properties for common server configuration.

### Example  
clean is set to `false` by default unless specified in `build.gradle` as shown in this example.  

```groovy
apply plugin: 'liberty'

liberty {
    server {
        name = 'myServer'
        clean = true
    }
}

```
