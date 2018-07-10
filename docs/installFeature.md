## installFeature task
The `installFeature` task installs features packaged as a Subsystem Archive (ESA file) to the Liberty runtime. 

In Open Liberty and WebSphere Liberty runtime versions 18.0.0.2 and above, this task can install features specified in the following ways:
* Using the `libertyFeature` dependency configuration,
* features listed in the `name` attribute,
* features declared in the `server.xml` file, its `include` elements, and from additional configuration files in the `configDropins` directory.

In WebSphere Liberty runtime versions 18.0.0.1 and below, this task will install features specified in the `name` attribute. To install the missing features declared in the `server.xml` file (including its `include` elements, and from additional configuration files in the `configDropins` directory), set the `acceptLicense` attribute to `true` but do not specify any `name` attribute.

In Open Liberty runtime versions 18.0.0.1 and below, this task will be skipped. A warning message will be displayed. The Open Liberty runtime versions 18.0.0.1 and below are bundled with all applicable features. There is no need to install or uninstall additional features.

### Dependencies

The Liberty Gradle plugin defines the `libertyFeature` dependency configuration for installing features. If the `java` plugin is applied in the build, then the `libertyFeature` configuration extends from the `java` plugin's `compileOnly` configuration to provide WebSphere Application Server Liberty API, SPI, and  Java specification dependencies.

The `libertyFeature` dependency configuration can install features in Liberty runtime versions 18.0.0.2 and above. Use the `io.openliberty.features` group for Open Liberty features, or the `com.ibm.websphere.appserver.features` group for WebSphere Liberty features.

You need to include `group`, `name`, and `version` values that describes the artifacts to use. An `ext` value for the ESA file type is not required.

### dependsOn
`installFeature` depends on `installLiberty`. If no specific features are requested, `installFeature` depends on `libertyCreate` to evaluate the set of features in the server configuration file.

### Properties

See the [Liberty server configuration](libertyExtensions.md#liberty-server-configuration) properties for server related configuration.

The `installFeature` task uses a `features` extension to define task specific properties. The following properties can be defined in the `features` extension.

| Attribute | Type  | Since | Description | Required |
| --------- | ----- | ----- | ----------- | -------- |
| name | String[] | 1.0 | Specifies the list of feature names to be installed. This can be an ESA file, an IBM-Shortname or a Subsystem-SymbolicName of the Subsystem archive. The value can be a file name or a URL to the ESA file. If this list is not provided, the server must be created so that the configured feature list is used. | No |
| acceptLicense | boolean | 1.0 | Accept feature license terms and conditions. The default value is `false`.  | Yes |
| to | String | 1.0 | Specify where to install the feature. The feature can be installed to any configured product extension location, or as a user feature (usr, extension). If this option is not specified the feature will be installed as a user feature. | No |
| from | String | 1.0 | Specifies a single directory-based repository as the source of the assets. The default is to install from the online Liberty repository. | No |

### Examples

1. The following example shows the properties needed to install the [`mongodb-2.0`](https://developer.ibm.com/wasdev/downloads/#asset/features-com.ibm.websphere.appserver.mongodb-2.0) feature to your defaultServer:

```groovy
apply plugin: 'liberty'

liberty {

    server {
        features {
            name = ['mongodb-2.0']
            acceptLicense = true
        }
    }
}
```

2. It is possible to install multiple features using a single block:
```groovy
/* Install 'mongodb-2.0' and 'adminCenter-1.0' features using a single block. */
apply plugin: 'liberty'

liberty {

    server {
        features {
            name = ['mongodb-2.0', 'adminCenter-1.0']
            acceptLicense = true
        }
    }
}
```

3. Install features from dependencies in Liberty runtime versions 18.0.0.2 and above.

* Open Liberty features:
```groovy
apply plugin: 'liberty'

dependencies {
    libertyFeature 'io.openliberty.features:jaxrs-2.1:18.0.0.2'
    libertyFeature 'io.openliberty.features:jsonp-1.1:18.0.0.2'
}

liberty {

    server {
        features {
            acceptLicense = true
        }
    }
}
```

* WebSphere Liberty features:
```groovy
apply plugin: 'liberty'

dependencies {
    libertyFeature 'com.ibm.websphere.appserver.features:servlet-3.0:18.0.0.2'
    libertyFeature 'io.openliberty.features:localConnector-1.0:18.0.0.2'
}

liberty {

    server {
        features {
            acceptLicense = true
        }
    }
}
```

4. The following uses the server's `server.xml` file to install all configured features.
```groovy
/* Install all features configured by the server */
apply plugin: 'liberty'

liberty {

    server {
        features {
            acceptLicense = true
        }
    }
}
```
