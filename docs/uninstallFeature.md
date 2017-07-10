## uninstallFeature task
The `uninstallFeature` task uninstalls a feature from the Liberty runtime.

### Properties

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| name | Specifies the feature name to be uninstalled. The name can a short name or a symbolic name of a Subsystem Archive (ESA file). | Yes |

### Examples

The following example shows what properties must be set up to uninstall the [`mongodb-2.0`](https://developer.ibm.com/wasdev/downloads/#asset/features-com.ibm.websphere.appserver.mongodb-2.0) 
Feature from your server:

```groovy
apply plugin: 'liberty'

liberty {
    installDir = "c:/wlp"

    uninstallfeatures {
        name = ['mongodb-2.0']
    } 
}
```
Also is possible uninstall multiple features in a single closure, for example:
```groovy
/* Uninstall 'mongodb-2.0', 'monitor-1.0' and 'oauth-2.0' features using a single closure. */
apply plugin: 'liberty'

liberty {
    installDir = "c:/wlp"

    uninstallfeatures {
        name = ['mongodb-2.0', 'monitor-1.0', 'oauth-2.0']
    } 
}
```