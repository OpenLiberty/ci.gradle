## libertyRun task  
Start a Liberty server in foreground. The server instance will be automatically created if it does not exist.  
**NOTE!** To view shutdown messages when terminating `libertyRun` with a Ctrl-C, run `libertyRun` like this:  
```
gradle libertyRun --no-daemon
```
  
### 0% Executing?
While running this task, Gradle will show in the console:  
```
<-------------> 0% EXECUTING  
> :libertyRun
```  
This is expected behavior because the task will neither progress nor stop as long as the server/process is running. The "build" will successfully finish with an external `libertyStop` or a Ctrl-C break (with `--no-daemon`).

### What is the Gradle Daemon and why --no-daemon?
The Gradle Daemon is a long-running background process designed to help speed up each build process. It does so by avoiding constant JVM startup costs while caching information about the project. This behavior is default until specified otherwise.  
Users who like to Ctrl-C their `libertyRun` task will find that this practice kills their daemon, ends `libertyRun` prematurely, and also leaves the server running in the background. In this state, it's best to restart your server. You will have to execute a separate `libertyStop` command to stop the server before starting it with either `libertyRun` or `libertyStart`. You may find that executing any new task with Gradle after killing the initial daemon will spawn another daemon in its absence, defeating the purpose of kiling the first.  
By running `libertyRun` with `--no-daemon`, this task will not benefit from the existing daemon, only suffering a slightly slower startup time. However, this protects the currently running daemon for future tasks and now ending the server with a Ctrl-C or an external `libertyStop` command should successfully end `libertyRun` without further problems.

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
