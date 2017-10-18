## cleanDirs task
The `cleanDirs` task deletes server specific files including:
* Applications in the `apps` folder
* Applications in the `dropins` folder
* Server log files  
* Server workarea files  
  
Note that the `cleanDirs` task is different from the `clean` task from the Java plugin. `cleanDirs` cleans the server directories whereas `clean` removes the `build` directory. If a server is running during a `clean`, some files may remain and require a second `clean`. Adding a ```clean dependsOn 'libertyStop'``` to your `build.gradle` file can help prevent this. The Liberty `cleanDirs` task automatically depends on the `libertyStop` task.

### dependsOn
`cleanDirs` depends on `libertyStop` to ensure a successful clean.

### Properties

See the [Liberty server configuration](libertyExtensions.md#liberty-server-configuration) properties for common server configuration.

The `cleanDirs` task uses a `cleanDir` block to define task specific behavior. The following properties can be defined in the `cleanDir` extension.

| Attribute | Type | Since | Description | Required |
| --------- | ---- | ----- | ----------- | ---------|
| apps | boolean | 1.0 | Delete all the files in the `${wlp_user_dir}/servers/<server name>/apps` directory. The default value is `false`. | No |
| dropins | boolean | 1.0 | Delete all the files in the `${wlp_user_dir}/servers/<server name>/dropins` directory. The default value is `false`. | No |
| logs | boolean | 1.0 | Delete all the files in the `${wlp_output_dir}/<server name>/logs` directory. The default value is `true`. | No |
| workarea | boolean | 1.0 | Delete all the files in the `${wlp_output_dir}/<server name>/workarea` directory. The default value is `true`. | No |

### Examples

The following example removes every app deployed to the server's `dropins` folder as well as the server's `workarea` and `logs` files.

```groovy
apply plugin: 'liberty'

liberty {

    server {
        name = 'server'

        cleanDir {
            dropins = true
        }
    }
}
```
Note: If you want to clean files from the server's `workarea` and `logs` folders, the server needs to be stopped.
