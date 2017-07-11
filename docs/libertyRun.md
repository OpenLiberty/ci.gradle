## libertyRun task  
Start a Liberty server in the foreground. The server instance will be automatically created if it does not exist.  
**NOTE!** To view shutdown messages when terminating `libertyRun` with a Ctrl-C, run `libertyRun` like this:  
```
gradle libertyRun --no-daemon
```
  
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
This is expected behavior because the task will neither progress nor stop as long as the server/process is running. The "build" will successfully finish with an external `libertyStop` or a Ctrl-C break (if ran with `--no-daemon`).

### What is the Gradle Daemon and why --no-daemon?
The Gradle Daemon is a long-running background process designed to help speed up the build process. It does so by avoiding constant JVM startup costs while caching information about the project. This behavior is default until specified otherwise.  
If running with the daemon (default), a Ctrl-C on the `libertyRun` task will kill the daemon and end `libertyRun` prematurely. Although the server will eventually shut down, it does so silently and will require about ~8-12 seconds to complete in the background. Running `libertyRun` with `--no-daemon` should eliminate these difficulties.

### Additional parameters

| Parameter | Description | Required |
| --------- | ------------ | ----------|
| clean | Setting the `clean` attribute clears all cached information on server start up. It deletes every file in the `${wlp_output_dir}/logs`, `${wlp_output_dir}/workarea`, `${wlp_user_dir}/dropins` or `${wlp_user_dir}/apps`. | No |

### Example  
clean is set to `false` by default unless specified in `build.gradle` as shown in this example.  

```groovy
apply plugin: 'liberty'

liberty {
    serverName = 'myServer'
    clean = true
}

```
