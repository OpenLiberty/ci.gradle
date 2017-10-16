## uninstallFeature task
The `uninstallFeature` task uninstalls a feature from the Liberty runtime.

### dependsOn
`uninstallFeature` depends on `libertyCreate` to make sure a server exists. 

### Properties

See the [Liberty server configuration](libertyExtensions.md#liberty-server-configuration) properties for common server configuration.

The `uninstallFeature` task uses a `uninstallfeatures` block to define task specific behavior.

| Attribute | Type  | Since | Description | Required |
| --------- | ----- | ----- | ----------- | -------- |
| name | String[] | 1.0 | Specifies the list of feature names to be uninstalled. The name can be a short or a symbolic name of a Subsystem Archive (ESA file). | Yes |

### Examples

The following example shows what properties must be set up to uninstall the [`mongodb-2.0`](https://developer.ibm.com/wasdev/downloads/#asset/features-com.ibm.websphere.appserver.mongodb-2.0)
Feature from your server:

```groovy
apply plugin: 'liberty'

liberty {
    installDir = "c:/wlp"

    server {
        uninstallfeatures {
            name = ['mongodb-2.0']
        }
    }
}
```
Also is possible uninstall multiple features in a single block, for example:
```groovy
/* Uninstall 'mongodb-2.0', 'monitor-1.0' and 'oauth-2.0' features using a single block. */
apply plugin: 'liberty'

liberty {
    installDir = "c:/wlp"

    server {
        uninstallfeatures {
            name = ['mongodb-2.0', 'monitor-1.0', 'oauth-2.0']
        }
    }
}
```
