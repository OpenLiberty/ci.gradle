## libertyDev task

Start a Liberty server in dev mode. This task also invokes the `libertyCreate`, `installFeature`, and `deploy` tasks before starting the server. **Note:** This task is designed to be executed directly from the Gradle command line.  To exit dev mode, press `Control-C`, or type `q` and press Enter.

To start the server in a container, see the [libertyDevc](#libertydevc-task-container-mode) section below. 

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
| serverStartTimeout | Maximum time to wait (in seconds) to verify that the server has started. The value must be an integer greater than or equal to 0. The default value is `90` seconds. | No |
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

----

## libertyDevc task, Container Mode

The following is a technology preview. The features and parameters described below may change in future milestones or releases of the Liberty Gradle plugin.

Start a Liberty server in a local container using the Dockerfile that you provide. An alternative to the `libertyDevc` task is to specify the `libertyDev` task with the `--container` option. 

The Dockerfile must copy the application .war file and the server configuration files that the application requires into the container. A sample Dockerfile is shown in [Building an application image](https://github.com/openliberty/ci.docker/#building-an-application-image). Note that the context of the `docker build` command used to generate the container image is the directory containing the Dockerfile. When dev mode exits the container is stopped and deleted and the logs are preserved as described below.

You need to install the Docker runtime (e.g. Docker Desktop) locally to use this Gradle task. You can examine the commands used to build and run the container by viewing the console output of dev mode.

When dev mode runs with container support it still provides the same features. It monitors files for changes and runs tests either automatically or on demand. This mode also allows you to attach a debugger to work on your application. You can review the logs generated by your server in the Liberty directory in your project e.g. build/wlp/usr/servers/defaultServer/logs.

This mode publishes the container ports 9080, 9443 and 7777 by default. If your application needs to publish other ports add them to the `dockerRunOpts` option either in the `build.gradle` file or on the `gradle` command line. E.g. `--dockerRunOpts="-p 9081:9081"`

### Limitations

For the current technology preview, the following limitations apply.

  - Supported on macOS and Windows with Docker Desktop installed.
  - Supported on Linux. Note the following.
    - Configuration files: the Open Liberty server runs on UID (user identifier) 1001 inside the container. Since dev mode makes the server configuration files from your project available inside the container they must be readable by that user. Therefore dev mode makes all the files in the server directory (`defaultServer`) in your project readable by all users. It also makes all the directories but `workarea` usable by all users.
    - Log files: the container will write the Open Liberty log files into the logs directory in the server directory in your project. When running with root permissions dev mode will change the owner of the logs directory and all the files inside to UID 1001 to match the id used by the server in the container. The GID will be 0 (root). When running as a non-root user the set GID bit will be set so that log files will be owned by user 1001 and the group id will be that of the current user.
    - Use of editors like `vim`: when you edit a configuration file with `vim` it will delete the file and rewrite it when you save. This necessitates a container restart. To avoid the restart edit your .vimrc file and add the line `set backupcopy=yes`

- Dockerfile limitations:
  - The Dockerfile must copy only one .war file for the application.  Other application archive formats or multiple .war files are not supported.
  - Hot deployment is only supported for individual configuration files that are specified as the source in the Dockerfile's COPY commands. Hot deployment is not supported for COPY commands with variable substitution, wildcard characters, spaces in paths, paths relative to WORKDIR, multi-stage builds, or entire directories specified as the source.

### Examples

Start dev mode with the server in a container using the Dockerfile in the project root.
```
$ gradle libertyDevc
```

Customizing the container configuration using `dev` extension properties in `build.gradle`.  Note that changing these while dev mode is running is not supported.
```
liberty {
    dev {
        container = true
        dockerRunOpts = '-p 9081:9081'
        dockerfile = file('myDockerfile')
    }
}
```

### Properties

The `dev` extension allows you to configure properties for the `libertyDevc` task.

These can also be specified as command line parameters in addition to the ones in the `libertyDev` section above.

| Attribute | Type  | Since | Description | Required |
| --------- | ----- | ----- | ----------- | -------- |
| container | boolean | 3.1-M1 (tech preview) | If set to `true`, run the server in the container specified by the `dockerfile` parameter. Setting this to `true` and using the `libertyDev` task is equivalent to using the `libertyDevc` task. The default value is `false` when the `libertyDev` task is used, and `true` when the `libertyDevc` task is used. | No |
| dockerRunOpts | String | 3.1-M1 (tech preview) | Specifies options to add to the `docker run` command when using dev mode to launch your server in a container. For example, `-e key=value` is recognized by `docker run` to define an environment variable with the name `key` and value `value`. Setting this parameter overrides the `container` parameter to `true`. | No |
| dockerfile | File | 3.1-M1 (tech preview) | Location of a Dockerfile to be used by dev mode to build the container that runs your server and to specify the context used to build the container image. The default location is `Dockerfile` in the project root. Setting this parameter overrides the `container` parameter to `true`. | No |
| dockerTimeout | integer | 3.1-M2 (tech preview) | Maximum time to wait (in seconds) for the completion of docker operations such as to build the container. The value must be an integer greater than 0. The default value is `60` seconds. | No |