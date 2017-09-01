## libertyDump task

The `libertyDump` task is used to dump diagnostic information from the server into an archive.

### Properties

See the [Liberty server configuration](libertyExtensions.md#Liberty-server-configuration) properties for common server configuration.

The `libertyDump` task uses a `dumpLiberty` block to define task specific behavior. 

| Attribute | Type  | Since | Description | Required |
| --------- | ----- | ----- | ----------- | -------- |
| include | String | 1.0 | Comma-separated list of diagnostic information to include in the dump. Valid values include `thread`, `system`, and `heap`.  | No |
| archive | String | 1.0 | Location of the target dump file.  | No |

### Examples

This example shows you how to configure these properties in your script:

```groovy
apply plugin: 'liberty'

liberty {

    server {
        name = 'myServer'

        //Example to create a thread and system dump for myServer
        dumpLiberty {
            archive = 'C:/MyServerDump.zip'
            include = 'thread, system'
        }
    }
}

```
