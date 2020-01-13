## libertyDev task

Start a Liberty server in dev mode. This task also invokes the `libertyCreate`, `installFeature`, and `deploy` tasks before starting the server. **Note:** This task is designed to be executed directly from the Gradle command line.  To exit dev mode, press `Control-C`, or type `q` and press Enter.

Dev mode provides three key features. Code changes are detected, recompiled, and picked up by your running server. Tests are run on demand when you press Enter in the command terminal where dev mode is running, or optionally on every code change to give you instant feedback on the status of your code. Finally, it allows you to attach a debugger to the running server at any time to step through your code.

The following are dev mode supported code changes. Changes to your server such as changes to the port, server name, hostname, etc. will require restarting dev mode to be detected.  Changes other than those listed below may also require restarting dev mode to be detected.

* Java source file changes and Java test file changes are detected, recompiled, and picked up by your running server.  
* Added dependencies to your `build.gradle` are detected and added to your classpath.  Dependencies that are Liberty features will be installed via the `installFeature` task.  Any other changes to your `build.gradle` will require restarting dev mode to be detected.
* Resource file changes are detected and copied into your `target` directory. 
* Configuration directory and configuration file changes are detected and copied into your `target` directory.  Added features to your `server.xml` will be installed and picked up by your running server.  Adding a configuration directory or configuration file that did not previously exist while dev mode is running will require restarting dev mode to be detected.


### Examples

Start dev mode.
```
$ gradle libertyDev
```

Start dev mode and run tests automatically after every code change.
```
$ gradle libertyDev --hotTests
```

Start dev mode and listen on a specific port for attaching a debugger (default is 7777).
```
$ gradle libertyDev --libertyDebugPort=8787
```

Start dev mode without allowing to attach a debugger.
```
$ gradle libertyDev --libertyDebug=false
```

### Command Line Parameters

The following are optional command line parameters supported by this task.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| hotTests | If this option is enabled, run tests automatically after every change. The default value is `false`. | No |
| skipTests | If this option is enabled, do not run any tests in dev mode. The default value is `false`. | No |
| libertyDebug | Whether to allow attaching a debugger to the running server. The default value is `true`. | No |
| libertyDebugPort | The debug port that you can attach a debugger to. The default value is `7777`. | No |
| compileWait | Time in seconds to wait before processing Java changes. If you encounter compile errors while refactoring, increase this value to allow all files to be saved before compilation occurs. The default value is `0.5` seconds. | No |
| serverStartTimeout | Maximum time to wait (in seconds) to verify that the server has started. The value must be an integer greater than or equal to 0. The default value is `30` seconds. | No |
| verifyAppStartTimeout | Maximum time to wait (in seconds) to verify that the application has started or updated before running tests. The value must be an integer greater than or equal to 0. The default value is `30` seconds. | No |

### Properties

See the [Liberty server configuration](libertyExtensions.md#liberty-server-configuration) properties for common server configuration.

### System Properties for Tests

Tests can read the following system properties to obtain information about the Liberty server.

| Property | Description |
| --------  | ----------- |
| wlp.user.dir | The user directory location that contains server definitions and shared resources. |
| liberty.hostname | The host name of the Liberty server. |
| liberty.http.port | The port used for client HTTP requests. |
| liberty.https.port | The port used for client HTTP requests secured with SSL (HTTPS). |

The Liberty Gradle plugin automatically propagates the system properties in the table above from the Gradle JVM to the JVM(s) running your tests. If you wish to add your own additional system properties you must configure your `build.gradle` file to set the system properties for the test JVM(s).

This can be done by setting specific properties for the test JVM.
```groovy
test {
    systemProperty 'example.property.1', System.getProperty('example.property.1')
    systemProperty 'example.property.2', System.getProperty('example.property.2')
    systemProperty 'example.property.3', System.getProperty('example.property.3')
}
```

Or by propagating all system properties from the Gradle JVM to the test JVM.
```groovy
test {
    systemProperties = System.properties
}
```
