## installFeature task
The `installFeature` task installs a feature packaged as a Subsystem Archive (ESA file) to the Liberty runtime.

### Properties

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| featureName |Specifies the name of the Subsystem Archive (ESA file) to be installed. The value can be a feature name, a file name or a URL. | Yes |
| acceptLicense | Accept feature license terms and conditions. The default value is `false`.  | No |
| whenFileExists | Specifies the action to take if a file to be installed already exits. Use `fail` to abort the installation, `ignore` to continue the installation and ignore the file that exists, and `replace` to overwrite the existing file.| No |
| to | Specifies feature installation location. Set to `usr` to install as a user feature. Otherwise, set it to any configured product extension location. The default value is `usr`.| No |

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
/* Install 'mongodb-2.0' and 'ejbLite-3.1' features using a single closure. */
apply plugin: 'liberty'

liberty {
    installDir = "c:/wlp"

    features {
        name = ['mongodb-2.0', 'ejbLite-3.1']
        acceptLicense = true
    } 
}
```