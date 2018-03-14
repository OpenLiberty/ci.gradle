**Multiple Servers**

The Liberty Gradle plugin allows you to define multiple servers that will run on the same Liberty runtime inside of your build file. This is done in the same manner as configuring a single Liberty server, but the servers are added to a `servers` block inside of the `liberty` closure.

***Defining Multiple Servers***

To define multiple servers add a `servers` block to your `liberty` closure. The plugin will use the name of each server closure inside of `servers` as the name for your liberty server. These names need to be unique.

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

***Multiple Server Tasks***

If you are using multiple servers you have the option to call the same task for every server, or to call the tasks individually. Calling a task for every server can be done by calling the task you wish to run just like a single server project.

```gradle libertyStart```

To call a task for a certain server you need to specify the server at the end of the task name.

```gradle libertyStart-libertyServer1```

To run the `libertyRun` or `libertyDebug` tasks there must be a server specified in the task name when multiple servers are defined.
The `installLiberty` task is not server specific and should not have a server name applied to the end of the task name.
