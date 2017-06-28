*** Points to cover  

1. Liberty Run differences over Liberty Start  
2. 0% Executing and Daemon vs No-Daemon  
3. Setting Clean Parameter

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| clean |Specifies the name of the Subsystem Archive (ESA file) to be uninstalled. The value can be a feature name, a 
file name or a URL. | No |

4. Example  

```groovy
apply plugin: 'liberty'

liberty {
    serverName = 'myServer'
    clean = true
}

```
