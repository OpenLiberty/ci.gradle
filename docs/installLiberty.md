## installLiberty task

The `installLiberty` task is used to download and install WebSphere Liberty. The task can also upgrade your Liberty runtime from an ILAN to an IPLA license with a license JAR file [setup](#installing-your-upgrade-license) and [license configuration](#license-configuration).  

The task can download the Liberty runtime archive in three ways:
* From [The Central Repository](http://search.maven.org/) using the `libertyRuntime` dependencies configuration or the `liberty.runtime` properties. This is the default installation method. The default runtime artifact is the latest version of io.openliberty:openliberty-kernel.
* From a specified location using `runtimeUrl`
* From the [Liberty repository](https://developer.ibm.com/wasdev/downloads/) based on a version and a runtime type.

When installing Liberty from a JAR file, the Liberty license code is needed to install the runtime. When you are installing Liberty from the Liberty repository, you can see the versions of Liberty available to install and find the link to their license using the [index.yml](https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/index.yml) file. After opening the license, look for the `D/N: <license code>` line. Otherwise, download the runtime archive and execute `java -jar wlp*runtime.jar --viewLicenseInfo` command and look for the `D/N: <license code>` line.

Note: Use the `libertyRuntime` dependency or the `liberty.runtime` properties to install Liberty from The Central Repository. Use the `install` block to install from the Liberty repository or from a local file. If both configurations are specified, the `libertyRuntime` dependency takes precedence.


### Dependencies

The Liberty Gradle plugin defines two dependency configurations for the `installLiberty` task: `libertyRuntime` and `libertyLicense`.  `libertyRuntime` defines which [Liberty runtime](#using-maven-artifact) to download from The Central Repository. `libertyLicense` [configures](#license-configuration) a license artifact so that your license JAR archive can be identified and used during the `installLiberty` task. Make sure to properly [setup](#installing-your-upgrade-license) your license JAR to prevent a missing dependency failure.

You need to include `group`, `name`, and `version` values that describe the artifacts to use. 

### Properties

Use the [general runtime properies](libertyExtensions.md#general-runtime-properties) for properties to configure the runtime installation location if you want to override the defaults.  By default, the runtime is installed in the `${project.buildDir}/wlp` folder.

You can also use the `runtime` properties object in the [general runtime properies](libertyExtensions.md#general-runtime-properties) to configure the `group`, `name`, or `version` values of the artifact to use. These will override any `libertyRuntime` dependency configuration and can also be specified in a gradle.properties file or from the command line as project properties.

### install block

Use the `install` block to specify the Liberty server to install from the Liberty repository. If any property is specified in the `install` block and `libertyRuntime` is not configured, the default runtime is installed from the Liberty repository.

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| licenseCode | WebSphere Liberty server license code. See [above](#installliberty-task). | Yes, if `type` is `webProfile6` or `runtimeUrl` specifies a `.jar` file. |
| version | Exact or wildcard version of the WebSphere Liberty server to install. Available versions are listed in the [index.yml](http://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/index.yml) file. Only used if `runtimeUrl` is not set and `libertyRuntime` is not configured. By default, the latest stable release is used. | No |
| runtimeUrl | URL to the WebSphere Liberty server's `.jar` or a `.zip` file on your repository or on the [Liberty repository](https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/). `runtimeUrl` can also point to an [Open Liberty](https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/) `.zip`. If not set and `libertyRuntime` is not configured, the Liberty repository will be used to find the Liberty runtime archive. | No |
| username | Username needed for basic authentication. | No |
| password | Password needed for basic authentication. | No |
| maxDownloadTime | Maximum time in seconds the download can take. The default value is `0` (no maximum time). | No |
| type | Liberty runtime type to download from the Liberty repository. Currently, the following types are supported: `kernel`, `webProfile6`, `webProfile7`, `webProfile8`, `javaee7`, and `javaee8`. Only used if `runtimeUrl` is not set and `libertyRuntime` is not configured. The default value is `webProfile7` if `useOpenLiberty` is `false`. If using Open Liberty and no type is specified, the default Open Liberty runtime is used. | No |
| useOpenLiberty | Specifies whether to install Open Liberty or WebSphere Liberty runtime when `runtimeUrl` is not specified and `libertyRuntime` is not configured. The default value is `true`. | No |

#### Example
```
dependencies {
    libertyRuntime group: 'com.ibm.websphere.appserver.runtime', name: 'wlp-webProfile8', version: '19.0.0.9'

    libertyLicense 'com.ibm.websphere.appserver.license:wlp-core-license:19.0.0.9'
}
```

You can define your dependencies with any of the following formats:
```  
libertyRuntime 'com.ibm.websphere.appserver.runtime:wlp-webProfile8:19.0.0.9'
```
```
libertyRuntime group: 'com.ibm.websphere.appserver.runtime', name: 'wlp-webProfile8', version: '19.0.0.9'
```
```
libertyRuntime(
    [group: 'com.ibm.websphere.appserver.runtime', name: 'wlp-webProfile8', version: '19.0.0.9']
)
```

You can override the dependency configuration or the default runtime artifact, which is the latest version of `io.openliberty:openliberty-kernel`, in any of the following ways:

In build.gradle:
```
liberty {
    runtime = ['group':'io.openliberty','name':'openliberty-javaee8']
}
```
This will get version 19.0.0.9 of `io.openliberty:openliberty-javaee8` when combined with the `libertyRuntime` dependency configuration shown above.

In gradle.properties:
```
liberty.runtime.name=wlp-javaee8
liberty.runtime.version=19.0.0.11
```
This will get version 19.0.0.11 of `com.ibm.websphere.appserver.runtime:wlp-javaee8` when combined with the `libertyRuntime` dependency configuration shown above.

From the command line:
```
gradle build -Pliberty.runtime.version=19.0.0.11
```
This will get version 19.0.0.11 of `com.ibm.websphere.appserver.runtime:wlp-webProfile8` when combined with the `libertyRuntime` dependency configuration shown above.

#### Examples

1. Install Liberty runtime with all Java EE 7 features using Liberty repository.
  ```groovy
    apply plugin: 'liberty'

    liberty {
        install {
            type = "javaee7"
        }
    }
  ```

2. Install from a specific location using a zip file.
  ```groovy
    apply plugin: 'liberty'

    liberty {
        install {
            runtimeUrl="<url to wlp*.zip>"
        }
    }
  ```

3. Install from a specific location using a jar file. `licenseCode` is required when installing from a jar file.
  ```groovy
    apply plugin: 'liberty'

    liberty {
        install {
            licenseCode = "<license code>"
            runtimeUrl = "<url to runtime.jar>"
        }
    }
  ```

#### Using Maven artifact

Use the [dependencies block](#dependencies) to specify the name of the repository artifact that contains your custom Liberty server or use one of the provided artifacts on [The Central Repository](http://search.maven.org/#search%7Cga%7C1%7Ccom.ibm.websphere.appserver.runtime).  

You can override the `group`, `name` or `version` values using the `runtime` Properties in the [general runtime properties](libertyExtensions.md#general-runtime-properties). These properties can also be specified in a gradle.properties file or from the command line.

The Maven Central repository includes the following Liberty runtime artifacts:

##### Open Liberty  
The `group` value for all the artifacts listed below is `io.openliberty`. For a list of versions available, follow the link in the `name` column to the artifact in Maven Central.

|`name` | Description |
| ----  | ----------- |
| [openliberty-runtime](https://repo1.maven.org/maven2/io/openliberty/openliberty-runtime/) | Open Liberty runtime. |
| [openliberty-javaee8](https://repo1.maven.org/maven2/io/openliberty/openliberty-javaee8/) | Open Liberty runtime with all Java EE 8 Full Platform features. |
| [openliberty-webProfile8](https://repo1.maven.org/maven2/io/openliberty/openliberty-webProfile8/) | Open Liberty runtime with Java EE 8 Web Profile features. |
| [openliberty-microProfile3](https://repo1.maven.org/maven2/io/openliberty/openliberty-microProfile3/) | Open Liberty runtime with MicroProfile 3 features. |
| [openliberty-kernel](https://repo1.maven.org/maven2/io/openliberty/openliberty-kernel/) | Open Liberty runtime kernel. |

##### WebSphere Liberty  
The `group` value for all the artifacts listed below is `com.ibm.websphere.appserver.runtime`. For a list of versions available, follow the link in the `name` column to the artifact in Maven Central.

|`name` | Description |
| ----  | ----------- |
| [wlp-javaee8](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-javaee8/) | WebSphere Liberty runtime with all Java EE 8 Full Platform features. |
| [wlp-javaee7](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-javaee7/) | WebSphere Liberty runtime with all Java EE 7 Full Platform features. |
| [wlp-webProfile8](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-webProfile8/) | WebSphere Liberty runtime with Java EE 8 Web Profile features. |
| [wlp-webProfile7](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-webProfile7/) | WebSphere Liberty runtime with Java EE 7 Web Profile features. |
| [wlp-microProfile2](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-microProfile2/) | WebSphere Liberty with MicroProfile 2 features. |
| [wlp-microProfile1](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-microProfile1/) | WebSphere Liberty with MicroProfile 1 features. |
| [wlp-kernel](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-kernel/) | WebSphere Liberty runtime kernel. |
| [wlp-osgi](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-osgi/) | WebSphere Liberty runtime with features that support OSGi applications. |

### Installing your upgrade license
To upgrade the runtime license, the Liberty license JAR file, which is available to download from IBM Fix Central or the Passport Advantage website, must be installed into a local repository or a protected internal repository. After successful installation, add your license artifact to your Liberty block in your `build.gradle` file to upgrade the license during the `installLiberty` task.

You can install your Liberty license JAR file in an internal repository such as Artifactory or to a local Maven repository. The following examples show how you can install the JAR file to a local Maven repository:

#### If you have Maven installed
Go to the location of your license JAR file and enter the following command in the console:
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
            version = '19.0.0.9'

            artifact licenseFile
        }
    }
}
```

### License configuration
The `libertyLicense` parameter defines the coordinates for the Liberty license JAR file that you added to an internal repository. The installLiberty task will only upgrade the license if this configuration is present.  
```
apply plugin: 'liberty'

repositories {
    mavenLocal()
}

dependencies {
    libertyLicense 'com.ibm.websphere.appserver.license:wlp-core-license:19.0.0.9'
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
