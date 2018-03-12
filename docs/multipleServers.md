**Multiple Servers**

The Liberty Gradle plugin allows you to define multiple servers that will run on the same Liberty runtime inside of your build file. This is done in the same manner as configuring a single Liberty server, but the servers are added to a `servers` block inside of the liberty closure. 

```
liberty {
    servers {
        libertyServer1 {

            apps = [war1]

        }

        libertyServer2 {
            
            apps = [war2]

            features {
                name = ['mongodb-2.0']
                acceptLicense = true
            }
        }

        libertyServer3 {

            dropins = [file('app.war')]

        }
    }
}
```

