#### install-apps
---
Copy applications specified as either Maven compile dependencies or the Maven project package to Liberty server's `dropins` or `apps` directory. Unlike the [deploy](deploy.md#deploy) goal, this goal only performs a simple copy operation. It does not require the server to be running and does not check if the application was successfully deployed. 

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](common-parameters.md#common-parameters).

| Parameter | Type | Description | Required |
| --------  | ---- | ----------- | -------  |
| appsDirectory | String | The server's `apps` or `dropins` directory where the application files should be copied. The default value is set to `apps` if the application is defined in the server configuration, otherwise it is set to `dropins`.  | No |
| stripVersion | boolean | Strip artifact version when copying the application to Liberty runtime's application directory. The default value is `false`. | No |
| installAppPackages | String | The Gradle? packages to copy to Liberty runtime's application directory. One of `dependencies`, `project` or `all`. The default is `dependencies`.<br>For an ear type project, this parameter is ignored and only the project package is installed. | No |

##### Example: 

```groovy
apply plugin: 'liberty'

liberty {
	installapps{
        appsDirectory = 'dropins'
        stripVersion = false
        installAppPackages = 'project'
    }
} 
```