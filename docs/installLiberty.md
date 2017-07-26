## installLiberty task

The `installLiberty` task is used to download and install WebSphere Liberty server. The task can also upgrade your Liberty runtime from an ILAN to an IPLA license with a license JAR file [setup](#installing-your-upgrade-license) and [license configuration](#license-configuration).  

The task can download the WebSphere Liberty runtime archive in three ways: 
* from a specified location (via `runtimeUrl`)
* from the [Liberty repository](https://developer.ibm.com/wasdev/downloads/) based on a version and a runtime type
* from the Maven Central repository.  

When installing Liberty from a JAR file, the Liberty license code is needed to install the runtime. When you are installing Liberty from the Liberty repository, you can see the versions of Liberty available to install and find the link to their license using the index.yml file. After opening the license, look for the `D/N: <license code>` line. Otherwise, download the runtime archive and execute `java -jar wlp*runtime.jar --viewLicenseInfo` command and look for the `D/N: <license code>` line.

Note: Only one of the install or assemblyArtifact blocks should be used in the same Gradle project. Use the install block to install from the Liberty repository or from a local file. Use the assemblyArtifact block to install from the Maven repository. If both blocks are specified, the Maven repository with assemblyArtifact is used.

### Properties for install block

Use the `install` to specify the name of the Liberty server to install from the Liberty repository.

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

#### Examples

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

### Properties for assemblyArtifact closure

Use the `assemblyArtifact` to specify the name of the Maven artifact.

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| groupId | Set Maven groupId to  download Liberty runtime archive from the Maven repository. The default value is `com.ibm.websphere.appserver.runtime` | No |
| artifactId | Liberty runtime type to download from the Maven repository. Currently, the following types are supported: `wlp-javaee7`, `wlp-webProfile7`, `wlp-kernel`, `wlp-osgi` and `wlp-microProfile1`. The default value is `wlp-webProfile7`. | No |
| version | Exact version of the WebSphere Liberty server to install. If version is not specified, all assemblyArtifact attributes are ignored and the Liberty repository is used instead. | Yes |
| type | Liberty runtime type to download. The default type is `zip`. | No |

#### Example for using the assemblyArtifact :

1. Install using the Maven local repository. 
  ```groovy
    apply plugin: 'liberty'

    repositories {
        mavenLocal()
    }

    liberty {
        assemblyArtifact {
            groupId = "com.ibm.websphere.appserver.runtime"
            artifactId = "wlp-webProfile7"
            version = "17.0.0.2"
            type = "zip"  
        }
    }
  ```

2. Install default runtime archive using the Maven Central repository.
  ```groovy
    apply plugin: 'liberty'

    repositories {
        mavenCentral()
    }

    liberty {
        assemblyArtifact {
             version = "17.0.0.2"
        }
    }    
  ```

#### Using Maven artifact

Use the `assemblyArtifact` to specify the name of the Maven artifact that contains a custom Liberty server or use one of the provided on the [Maven Central repository](http://search.maven.org/).

The Maven Central repository includes the following Liberty runtime artifacts:

|Artifact ID | Versions | Description |
| --- | ----------------- | ----------- |
| [wlp-javaee7](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-javaee7/) | 17.0.0.2, 17.0.0.1, 16.0.0.4, 16.0.0.3, 16.0.0.2, 8.5.5.9, 8.5.5.8, 8.5.5.7, 8.5.5.6 | Liberty runtime with all Java EE 7 Full Platform features. |
| [wlp-webProfile7](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-webProfile7/) | 17.0.0.2, 17.0.0.1, 16.0.0.4, 16.0.0.3, 16.0.0.2, 8.5.5.9, 8.5.5.8, 8.5.5.7, 8.5.5.6 | Liberty runtime with Java EE 7 Web Profile features. |
| [wlp-kernel](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-kernel/) | 17.0.0.2, 17.0.0.1, 16.0.0.4, 16.0.0.3, 16.0.0.2, 8.5.5.9, 8.5.5.8 | Liberty runtime kernel. |
| [wlp-osgi](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-osgi/) | 17.0.0.2, 17.0.0.1, 16.0.0.4, 16.0.0.3, 16.0.0.2, 8.5.5.9, 8.5.5.8 | Liberty runtime with features that support OSGi applications. |
| [wlp-microProfile1](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-microProfile1/) | 17.0.0.2, 17.0.0.1, 16.0.0.4, 16.0.0.3 | Liberty with features for a MicroProfile runtime. |

### Installing your upgrade license
To upgrade the runtime license, the Liberty license JAR file, which is available to download from IBM Fix Central or the Passport Advantage website, must be installed into a local repository or a protected internal repository. After successful installation, add your license artifact to your Liberty block in your `build.gradle` file to upgrade the license during the `installLiberty` task.
  
You can install your Liberty license JAR file in an internal repository such as Artifactory or to a local Maven repository. The following examples show how you can install the JAR file to a local Maven repository:

#### If you have Maven installed
Got to the location of your license JAR file and enter the following command in the console:
```
mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=wlp-core-license.jar
```  

#### If you do not have Maven installed
Add these details to the `build.gradle` file before executing a `gradle publishToMavenLocal` command. This has the same effect as `maven-install-plugin` above.
##### build.gradle
```
apply-plugin: 'maven'
apply-plugin: 'maven-publish'

def licenseFile = file('wlp-core-license.jar')
artifacts {
    archives licenseFile
}

publishing {
    publications {
        licenseArtifact(MavenPublication) {
            groupId = 'com.ibm.websphere.appserver.license'
            artifactId = 'wlp-core-license'
            version = '17.0.0.2'
        
            artifact licenseFile
        }
    }
}
```

### License configuration
The `licenseArtifact` parameter defines the coordinates for the Liberty license JAR file that you added to an internal repository. The installLiberty task will only upgrade the license if this configuration is present.  
The `licenseArtifact` format is `'<groupId>:<artifactId>:<version>'`
```
repositories {
    ...
    mavenLocal()
}

liberty {
    ...
    licenseArtifact = 'com.ibm.websphere.appserver.license:wlp-core-license:17.0.0.2'
}
```

### Adding a custom repository
Add to your `build.gradle` this outline with your information. For more details, refer to Gradle's [documentation](https://docs.gradle.org/current/userguide/artifact_dependencies_tutorial.html#sec:repositories_tutorial).
```
repositories {
    maven {
        name 'Your Custom Repository'
        url 'https://your-url-here.me'
        credentials {
            username ''
            password ''
        }
    }
}
```
