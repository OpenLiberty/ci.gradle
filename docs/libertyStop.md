## libertyStop task  
Stop a Liberty server.

### Parameters

See the [Liberty server configuration](libertyExtensions.md#liberty-server-configuration) properties for common server configuration.

### Example  

```groovy
apply plugin: 'liberty'

liberty {
    server {
        name = 'myServer'
    }
}

```
