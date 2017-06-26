## deploy task

The `deploy` task supports deployment of one or more applications to the WebSphere Liberty server.

### Properties

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| file| Location of a single application to be deployed. The application type can be war, ear, rar, eba, zip, or jar. | Yes, only when a single file will be deployed. |
| dir|  Location of the directory where are the applications to be deployed.| Yes, only when multiples files will be deployed and `file` is not specified.|
| include| Comma- or space-separated list of patterns of files that must be included. All files are included when omitted.| No |
| exclude| Comma- or space-separated list of patterns of files that must be excluded. No files are excluded when omitted.| No |

Deploy's properties must be set up in the `deploy` closure inside the `liberty` closure.

### Examples

1. Deploys a single file.
  ```groovy
    apply plugin: 'liberty'

    liberty {
        installDir = 'c:/wlp'
        serverName = 'myServer'
        
        deploy {
            file = 'c:/files/app.war'
        }
    }
  ```

2. Deploy multiple files. Specifically, deploy `app.war` and `sample.war` but exclude `test-war.war`.
  ```groovy
    apply plugin: 'liberty'

    liberty { 
        installDir = 'c:/wlp'
        serverName = 'myServer'
        
        deploy {
            dir = 'c:/files'
            include = 'app.war, sample.war'
            exclude = 'test-war.war'
        }
    }
  ```

3. Deploy multiple files using multiple closures.
  ```groovy
    apply plugin: 'liberty'

    liberty { 
        installDir = 'c:/wlp'
        serverName = 'myServer'
        
        deploy {
            file = 'c:/files/app.war'
        }

        deploy {
            file = 'c:/resources/test.war'
        }
        
        deploy {
            dir = 'c:/extras'
            include = 'sample.war, demo.war'
        }
    }
  ```

The following examples shows you how to deploy a file using the `WAR` or the `EAR` Gradle plugins:

```groovy
    /* Deploys 'sample.war' using the WAR plugin */
    apply plugin: 'war'

    war {
        destinationDir = new File ('C:/files')
        archiveName = 'sample.war'
    }
```

`destinationDir` and `archiveName` are native properties of Gradle's WAR plugin. For more information see [here.](https://gradle.org/docs/current/dsl/org.gradle.api.tasks.bundling.War.html)

```groovy
    /* Deploys 'test.ear' using the EAR plugin */
    apply plugin: 'ear'

    ear {
        destinationDir = new File ('C:/files')
        archiveName = 'test.ear'
    }
```

`destinationDir` and `archiveName` are native properties of Gradle's EAR plugin. For more information see [here.](https://gradle.org/docs/current/dsl/org.gradle.plugins.ear.Ear.html)
