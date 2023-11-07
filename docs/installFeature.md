## installFeature task
The `installFeature` task installs features packaged as a Subsystem Archive (ESA file) to the Liberty runtime. 

In Open Liberty and WebSphere Liberty runtime versions 18.0.0.2 and above, this task can install features specified in the following ways:
* Using the `libertyFeature` dependency configuration,
* features listed in the `name` attribute,
* features declared in the `server.xml` file, its `include` elements, and from additional configuration files in the `configDropins` directory.

In WebSphere Liberty runtime versions 18.0.0.1 and below, this task will install features specified in the `name` attribute. To install the missing features declared in the `server.xml` file (including its `include` elements, and from additional configuration files in the `configDropins` directory), set the `acceptLicense` attribute to `true` but do not specify any `name` attribute.

In Open Liberty runtime versions 18.0.0.1 and below, this task will be skipped. A warning message will be displayed. The Open Liberty runtime versions 18.0.0.1 and below are bundled with all applicable features. There is no need to install or uninstall additional features.

In Open Liberty runtime versions 21.0.0.11 and above, you can install custom user features. Check this [blog](https://openliberty.io/blog/2022/07/06/user-feature-install.html) on how to build and install user feature using Maven plug-ins.

### Dependencies

The Liberty Gradle plugin defines the `libertyFeature` dependency configuration for installing features. If the `java` plugin is applied in the build, then the `libertyFeature` configuration extends from the `java` plugin's `compileOnly` configuration to provide Liberty API, SPI, and  Java specification dependencies.

The `libertyFeature` dependency configuration can install features in Liberty runtime versions 18.0.0.2 and above. Use the `io.openliberty.features` group for Open Liberty features, or the `com.ibm.websphere.appserver.features` group for WebSphere Liberty features.

The `featuresBom` dependency configuration is used to install user feature. See [prepareFeature.md](prepareFeature.md) for details.

You need to include `group`, `name`, and `version` values that describes the artifacts to use. An `ext` value for the ESA file type is not required.

### dependsOn
`installFeature` depends on `libertyCreate` to evaluate the set of features in the server configuration file.

If the `featuresBom` dependency is configured, then `installFeature` depends on `prepareFeature` to generate `features.json` file for the user feature.


### Properties

See the [Liberty server configuration](libertyExtensions.md#liberty-server-configuration) properties for server related configuration.

The `installFeature` task uses a `features` extension to define task specific properties. The following properties can be defined in the `features` extension.

| Attribute | Type  | Since | Description | Required |
| --------- | ----- | ----- | ----------- | -------- |
| name | String[] | 1.0 | Specifies the list of feature names to be installed. This can be a local ESA file, an IBM-Shortname or a Subsystem-SymbolicName of the Subsystem archive. If this list is not provided, the server must be created so that the configured feature list is used. | No |
| acceptLicense | boolean | 1.0 | Accept feature license terms and conditions. The default value is `false`, so you must add this property to get features installed if it is required.  | Required for runtime versions 18.0.0.1 and below, or for features that are not from Open Liberty. <p/> Not required for Open Liberty features on runtime versions 18.0.0.2 and above. |
| to | String | 1.0 | Specify where to install the feature. The feature can be installed to any configured product extension location, or as a user feature (usr, extension). If this option is not specified the feature will be installed as a user feature. | No |
| from | String | 1.0 | Specifies a single directory-based repository as the source of the assets. The default is to install from the online Liberty repository. | No |
| verify | String | X.X | Specifies how features must be verified during a process or an installation. Supported values are `enforce`, `skip`, `all`, and `warn`. If this option is not specified, the default value is enforce. <ul><li>`enforce`: Verifies the signatures of all Liberty features except for user features. It checks the integrity and authenticity of the features that are provided by the Liberty framework.</li><li>`skip`: Choosing this option skips the verification process altogether. No feature signatures are downloaded or checked. It expedites the installation process but must be used with caution, as it bypasses an important security step.</li></ul><ul><li>`all`: Verifies both the Liberty features and the user features. The features that are provided by the Liberty framework and any additional user features or components are checked for integrity.</li><li>`warn`: Similar to the all option, warn also verifies both the Liberty features and user features. This option allows the process to continue, even if some feature signatures cannot be validated. A verification failure does not immediately end the installation process, but it results in a warning message.</li></ul> | No |

Verify your user features by providing the long key ID and key URL to reference your public key that is stored on a key server. For more information about generating a key pair, signing the user feature, and distributing your key, see [Working with PGP Signatures](https://central.sonatype.org/publish/requirements/gpg/#signing-a-file).
 The following properties can be defined in the `server` extension.

 | Attribute | Type  | Since | Description | Required |
| --------- | ----- | ----- | ----------- | -------- |
| keys | Properties | X.x | The property name is used for the keyid, and the property value is used for the keyurl. <ul><li>`keyid`: Provide the long key ID for your public key. The long key ID is a 64-bit identifier that is used to uniquely identify a PGP key.</li><li>`keyurl`: Provide the full URL of your public key. The URL must be accessible and point to a location where your key can be retrieved. The supported protocols for the key URL are `HTTP`, `HTTPS`, and `file`.</li> | No |

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

5. Install user feature

    1. To install user feature using either the IBM-Shortname or the Subsystem-SymbolicName as the name reference, you first need to run [prepare-feature](prepare-feature.md) task to generate a `features.json` file. To bypass this task, you need to specify the local esa path.

    2. Keep the `featuresBom` dependency from the previous step for your user feature. To verify the signature of your user feature, provide the public key by specifying the `keys` properties in [keyid:keyurl] format. The user feature signature file must reside in the same directory as the corresponding feature esa file.
    
    ```xml
    dependencies {
        featuresBom 'my.user.features:features-bom:1.0'
    }
    liberty {
        server {
            features {
                name = ['myUserFeature-1.0']
                acceptLicense = true
                verify = 'all'
            }
            keys = ['0x05534365803788CE':'https://keyserver.ubuntu.com/pks/lookup?op=get&amp;options=mr&amp;search=0x05534365803788CE']
        }

    }
    
    ```

