## installLiberty task

The `installLiberty` task is used to download and install WebSphere Liberty server. The task can download the WebSphere Liberty runtime archive from a specified location (via `runtimeUrl`) or automatically resolve it from the [Liberty repository](https://developer.ibm.com/wasdev/downloads/) or from the Maven repository based on groupId. 

In certain cases, the Liberty license code may need to be provided in order to install the runtime. If the license code is required and if you are installing Liberty from the Liberty repository, you can obtain the license code by reading the [current license](https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/17.0.0.2/lafiles/runtime/en.html) and looking for the `D/N: <license code>` line. Otherwise, download the runtime archive and execute `java -jar wlp*runtime.jar --viewLicenseInfo` command and look for the `D/N: <license code>` line.

### Properties

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| licenseCode | WebSphere Liberty server license code. See [above](#installliberty-task). | Yes, if `type` is `webProfile6` or `runtimeUrl` specifies a `.jar` file. |
| version | Exact or wildcard version of the WebSphere Liberty server to install. Available versions are listed in the [index.yml](http://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/index.yml) file. Only used if `runtimeUrl` is not set and the Maven repository is not used. By default, the latest stable release is used. | No |
| runtimeUrl | URL to the WebSphere Liberty server's `.jar` or a `.zip` file. If not set, the Liberty repository will be used to find the Liberty runtime archive. | No |
| baseDir | The base installation directory. The actual installation directory of WebSphere Liberty server will be `${baseDir}/wlp`. The default value is `${project.buildDir}`. | No | 
| cacheDir | The directory used for caching downloaded files such as the license or `.jar` files. The default value is `${java.io.tmpdir}/wlp-cache`. | No | 
| username | Username needed for basic authentication. | No | 
| password | Password needed for basic authentication. | No | 
| maxDownloadTime | Maximum time in seconds the download can take. The default value is `0` (no maximum time). | No | 
| type | Liberty runtime type to download from the Liberty repository. Currently, the following types are supported: `kernel`, `webProfile6`, `webProfile7`, and `javaee7`. Only used if `runtimeUrl` is not set and the Maven repository is not used. The default value is `webProfile6`. | No |

### Examples

1. Install using Liberty repository.
  ```groovy
    apply plugin: 'liberty'

    liberty {
        install {
            licenseCode = "<license code>"
        }
    }
  ```

2. Install from a specific location.
  ```groovy
    apply plugin: 'liberty'

    liberty {
        install {
            licenseCode = "<license code>"
            runtimeUrl = "<url to runtime.jar>"
        }
    }
  ```

3. Install Liberty runtime with all Java EE 7 features using Liberty repository.
  ```groovy
    apply plugin: 'liberty'

    liberty {
        install {
            type = "javaee7"
        }
    }
  ```

4. Install from a specific location using a zip file.
  ```groovy
    apply plugin: 'liberty'

    liberty {
        install {
            runtimeUrl="<url to wlp*.zip>"
        }
    }
  ```

### Using Maven artifact

Use the `assemblyArtifact` to specify the name of the Maven artifact that contains a custom Liberty server or use one of the provided on the [Maven Central repository](http://search.maven.org/). 

The Maven Central repository includes the following Liberty runtime artifacts:

|Artifact ID | Versions | Description |
| --- | ----------------- | ----------- |
| [wlp-javaee7](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-javaee7/) | 17.0.0.2, 17.0.0.1, 16.0.0.4, 16.0.0.3, 16.0.0.2, 8.5.5.9, 8.5.5.8, 8.5.5.7, 8.5.5.6 | Liberty runtime with all Java EE 7 Full Platform features. |
| [wlp-webProfile7](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-webProfile7/) | 17.0.0.2, 17.0.0.1, 16.0.0.4, 16.0.0.3, 16.0.0.2, 8.5.5.9, 8.5.5.8, 8.5.5.7, 8.5.5.6 | Liberty runtime with Java EE 7 Web Profile features. |
| [wlp-kernel](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-kernel/) | 17.0.0.2, 17.0.0.1, 16.0.0.4, 16.0.0.3, 16.0.0.2, 8.5.5.9, 8.5.5.8 | Liberty runtime kernel. |
| [wlp-osgi](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-osgi/) | 17.0.0.2, 17.0.0.1, 16.0.0.4, 16.0.0.3, 16.0.0.2, 8.5.5.9, 8.5.5.8 | Liberty runtime with features that support OSGi applications. |
| [wlp-microProfile1](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-microProfile1/) | 17.0.0.2, 17.0.0.1, 16.0.0.4, 16.0.0.3 | Liberty with features for a MicroProfile runtime. |


Note: The group ID for these artifacts is: `com.ibm.websphere.appserver.runtime`.

```
### assemblyArtifact Properties

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| groupId | Set Maven groupId to `com.ibm.websphere.appserver.runtime` to  download Liberty runtime archive from the Maven repository. If not set, the [Liberty repository](https://developer.ibm.com/wasdev/downloads/) will be used by default. Only used if `runtimeUrl` is not set. | No |
| artifactId | Liberty runtime type to download from the Maven repository. Currently, the following types are supported: `wlp-javaee7`, `wlp-webProfile7`, `wlp-kernel`, `wlp-osgi` and `wlp-microProfile1`. The default value is `wlp-webProfile7`. Only used if `runtimeUrl` is not set.  | No |
| version | Exact version of the WebSphere Liberty server to install. The default version is '17.0.0.2'. Only used if `runtimeUrl` is not set. | No |
| type | Liberty runtime type to download from the Maven repository. The default value is `zip` for the Maven repository. | No |

### Example for using the `assemblyArtifact` parameter:

1. Install using Maven repository.
  ```groovy
    apply plugin: 'liberty'

    liberty {
        assemblyArtifact {
            groupId = "com.ibm.websphere.appserver.runtime"
            artifactId = "wlp-webProfile7"
            version = "17.0.0.2" 
            type = "zip"  
        }
    }
    
2. Install using Maven repository with default values.
  ```groovy
    apply plugin: 'liberty'

    liberty {
        assemblyArtifact {
            groupId = "com.ibm.websphere.appserver.runtime"
        }
    }    
  ```
