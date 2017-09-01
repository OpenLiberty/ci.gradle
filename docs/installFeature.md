## installFeature task
The `installFeature` task installs features packaged as a Subsystem Archive (ESA file) to the Liberty runtime. The `installFeature` task can install a list of features to the Liberty runtime, or it can install a set of features based on the server configuration file.

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

3. The following uses the server's `server.xml` file to install all configured features.
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
