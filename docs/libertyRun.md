## libertyRun task  
Start a Liberty server in the foreground. The server instance will be automatically created if it does not exist.  
**NOTE:** For proper server shutdown and to view shutdown console messages when terminating `libertyRun` with a Ctrl-C, use the `--no-daemon` option on the `libertyRun` task:   
```
gradle libertyRun --no-daemon
```
  
### What is the Gradle Daemon and why --no-daemon?
The Gradle Daemon is a long-running background process designed to help speed up the build process. It does so by caching project information and staying alive, avoiding constant JVM startup costs. This behavior is default until specified otherwise.  
Gradle's current daemon design makes it difficult to use a `run` task, as a Ctrl-C would kill the daemon while simultaneously leaving the application running in the background. Therefore, we need `--no-daemon` so that the signal can be received and properly handled.  
In the event that a user does run a `libertyRun` with a daemon (default), an external `libertyStop` must be called in order to properly shut down the server. A Ctrl-C while the Gradle process is running will not stop the server. Use `libertyStatus` to confirm the state of your server.  

### 0% or 66% Executing?
While running this task, Gradle will show something like:  
```
<-------------> 0% EXECUTING
> :libertyRun
```  
or
```
<========-----> 66% EXECUTING
> :libertyRun
```
This is expected behavior because the task will neither progress nor stop as long as the server/process is running. The "build" will successfully finish with an external `libertyStop` or a Ctrl-C break.

### Additional parameters

| Parameter | Type | Description | Required |
| --------- | -----| ----------- | ---------|
| clean | Boolean | Setting the `clean` attribute clears all cached information on server start up. It deletes every file in the `${wlp_output_dir}/logs`, `${wlp_output_dir}/workarea`, `${wlp_user_dir}/dropins` or `${wlp_user_dir}/apps`. | No |

### Example  
clean is set to `false` by default unless specified in `build.gradle` as shown in this example.  

```groovy
apply plugin: 'liberty'

liberty {
    serverName = 'myServer'
    clean = true
}

```
