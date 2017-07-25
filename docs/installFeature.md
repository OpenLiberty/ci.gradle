## installFeature task
The `installFeature` task installs a feature packaged as a Subsystem Archive (ESA file) to the Liberty runtime.

### Properties

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| name | Specifies the name of the Subsystem Archive (ESA file) to be installed. This can be an ESA file, an IBM-Shortname or a Subsystem-SymbolicName of the Subsystem archive. The value can be a file name or a URL to the ESA file. | Yes |
| acceptLicense | Accept feature license terms and conditions. The default value is `false`.  | No |
| to | Specify where to install the feature. The feature can be installed to any configured product extension location, or as a user feature (usr, extension). If this option is not specified the feature will be installed as a user feature. | No |
| from | Specifies a single directory-based repository as the source of the assets. | No |

### Examples

The following example shows what properties must be set up to install the [`mongodb-2.0`](https://developer.ibm.com/wasdev/downloads/#asset/features-com.ibm.websphere.appserver.mongodb-2.0) feature to your server:

```groovy
apply plugin: 'liberty'

liberty {
    installDir = "c:/wlp"

    features {
        name = ['mongodb-2.0']
        acceptLicense = true
    } 
}
```

Also is possible install multiple features in a single closure, for example:
```groovy
/* Install 'mongodb-2.0' and 'adminCenter-1.0' features using a single closure. */
apply plugin: 'liberty'

liberty {
    installDir = "c:/wlp"

    features {
        name = ['mongodb-2.0', 'adminCenter-1.0']
        acceptLicense = true
    } 
}
```