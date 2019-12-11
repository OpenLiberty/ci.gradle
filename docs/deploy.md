## deploy task
---
The `deploy` task copies applications to the Liberty server's `dropins` or `apps` directory. If an application is deployed while the server is running, the task will verify that the application has started successfully.

### dependsOn
`deploy` depends on all tasks of type `war` or `ear` so the package is ready before installation.  
`deploy` also depends on `libertyCreate` to ensure that the server exists.

### Parameters

The `deploy` task can be configured using the `deploy` block. It is located in the `server` block. Below are the properties that can be set in the `deploy` block.

| Attribute | Type  | Since | Description | Required |
| --------- | ----- | ----- | ----------- | -------- |
| apps | Object[] | 3.0 | Specifies which tasks/files should be deployed to the `apps` directory. Applications can be passed in as the task that generates the file or as an application file. | No |
| dropins | Object[] | 3.0 | Specifies which tasks/files should be deployed to the `dropins` directory. Applications can be passed in as the task that generates the file or as an application file. | No |

In addition to the `deploy` block, the `stripVersion` and `looseApplication` properties are used for application installation. More information on these properties can be found in the [Liberty server configuration](libertyExtensions.md#liberty-server-configuration).

For tasks of type ear, loose application requires Gradle 4.0 or higher.

When targeting an application to the `dropins` folder, the application will start automatically when the server is running. No additional configuration is needed. When targeting an application to the `apps` folder, the application must be configured in the `server.xml` file. Note that the `location` attribute is relative to the `apps` folder. If you configure an application to deploy to the `apps` folder and do not specify application configuration in the `server.xml` file, then default configuration is generated in the `configDropins` folder for the application.

Multiple applications can be installed to the `apps` or `dropins` directories. This can be done by adding application files, or the tasks that generate these files, to the respective list.

### Using the libertyApp configuration

The `libertyApp` dependency configuration can be used to pull in application artifacts as dependencies and then install them to the server's `apps` directory. You can specify the artifact as any type of object that can be resolved in to a Dependency object by Gradle. The application artifact still needs to be a supported application type of `war` or `ear` to be installed.

### Examples:

```groovy
apply plugin: 'liberty'
apply plugin: 'war'

task libertyWarTask(type:War){
    ...
}

liberty {
    server {
        name = 'myServer'
        deploy {
            apps = [file('build/libs/libertyApp.war'), libertyWarTask]
            dropins = [war]
        }
        stripVersion = true
    }
}
```

```groovy
apply plugin: 'liberty'
apply plugin: 'ear'


liberty {
    server {
        name = 'myServer'
        deploy {
            //assuming ear application is already configured in the server.xml
            apps = [ear]
        }
        stripVersion = true
    }
}
```

```groovy
apply plugin: 'liberty'

dependencies {
    libertyApp 'example:app:1.0'
}
```
