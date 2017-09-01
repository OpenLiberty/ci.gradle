## libertyJavaDump task

The `libertyJavaDump` task dumps diagnostic information from the server JVM.

### Properties

See the [Liberty server configuration](libertyExtensions.md#Liberty-server-configuration) properties for common server configuration.

The `libertyJavaDump` task uses a `javaDumpLiberty` block to define task specific behavior. 

| Attribute | Type  | Since | Description | Required |
| --------- | ----- | ----- | ----------- | -------- |
| include | String | 1.0 | Comma-separated list of diagnostic information to include in the dump. Valid values include `system` and `heap`.  | No |
| archive | String | 1.0 | Location of the target dump file.  | No |

### Examples

This example shows you how to configure these properties in your script:

```groovy
apply plugin: 'liberty'

liberty {

    server {
        name = 'myServer'

        javaDumpLiberty {
            archive = "MyServerJavaDump.zip"
            include = "system"
        }
    }
}

```
