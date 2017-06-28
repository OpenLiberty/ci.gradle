## libertyRun task  
Start a Liberty server in foreground. The server instance will be automatically created if it does not exist.  
**NOTE!** To view shutdown messages when terminating `libertyRun` with a Ctrl-C, run:  
```
gradle libertyRun --no-daemon
```
  
### 0% Executing?
While running this task, Gradle will show in the console:  
```
<-------------> 0% EXECUTING  
> :libertyRun
```  
which is expected behavior, because the task will not stop or progress as long as the server/process is running. The "build" will finish upon termination with an external `libertyStop` or a Ctrl-C break.

### What is the Gradle Daemon and why --no-daemon?
The Gradle Daemon runs in the background to help speed up the build process by avoiding constant JVM startup costs while caching additional information about the project. This behavior is default until specified otherwise. Users who like to Ctrl-C their `libertyRun` task will find that this practice not only kills the daemon and ends `libertyRun` prematurely, but that this would also leave the server still running in the background, requiring a separate `libertyStop` command to stop the server. Any new task in Gradle would spawn a new daemon, defeating the purpose of the earlier daemon.  
By running `libertyRun` with `--no-daemon`, this task will not benefit from the existing daemon. However, this protects the currently running daemon for future tasks and now ending the server with a Ctrl-C or an external `libertyStop` command should successfully end `libertyRun` without further problems.

### Additional parameters

| Parameter | Description | Required |
| --------- | ------------ | ----------|
| clean | Setting the `clean` attribute clears all cached information on server start up. It deletes every file in the `${wlp_output_dir}/logs`, `${wlp_output_dir}/workarea`, `${wlp_user_dir}/dropins` or `${wlp_user_dir}/apps`. | No |

### Example  
clean is set to `false` by default unless specified in `build.groovy` as shown in this example.  

```groovy
apply plugin: 'liberty'

liberty {
    serverName = 'myServer'
    clean = true
}

```
