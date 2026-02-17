## toolchain

Gradle Java toolchains let you select a specific JDK for your project. When a toolchain is configured, the Liberty Gradle plugin will try to run Liberty server tasks with that JDK (by setting `JAVA_HOME`), even if Gradle itself is running on a different JVM.

For more details on toolchains, see the [Gradle Java Toolchains documentation](https://docs.gradle.org/current/userguide/toolchains.html).


### Overview

Gradle Java toolchains are configured through the `java` plugin using the `java { toolchain { ... } }` block.

The Liberty Gradle plugin does not add a Liberty-specific toolchain block. Instead, it uses the toolchain configuration from the `java` plugin.

* When a Java toolchain is configured, Liberty server tasks attempt to run with the selected toolchain JDK.
* In dev mode (`libertyDev`), the plugin:
  * Uses the configured toolchain JDK for the Liberty server JVM (via `JAVA_HOME`).
  * Triggers Gradle compile tasks (`compileJava`, `compileTestJava`).


### Configuration

Apply the Gradle `java` plugin and configure a Java toolchain in your build.

Example (`build.gradle`):

   ```groovy
   plugins {
       id 'java'
       id 'io.openliberty.tools.gradle.Liberty'
   }
   
   java {
       toolchain {
           // Required Java version for your project
           languageVersion = JavaLanguageVersion.of(11)
       }
   }
   ```

Make sure the Gradle version you use supports Java toolchains (Gradle 6.7+), and that the requested toolchain can be resolved (either from a locally installed JDK or via a toolchain provisioning plugin).


### Liberty server tasks

When you configure a Gradle Java toolchain, the Liberty Gradle plugin uses the resolved toolchain JDK home as `JAVA_HOME` when it runs Liberty server commands.

**NOTE:** If `JAVA_HOME` is already set in `server.env` or `jvm.options`, that configuration takes precedence. In that case, the plugin does not override `JAVA_HOME` with the toolchain JDK.

This includes cases where `JAVA_HOME` is set by the Liberty server configuration in your build, for example:
 
* `liberty { server { env = ['JAVA_HOME': '...'] } }`
* Gradle project properties such as `liberty.server.env.JAVA_HOME` (for example, `-Pliberty.server.env.JAVA_HOME=/path/to/jdk`)

When a toolchain is detected, you will see a message like:

`CWWKM4100I: Using toolchain from build context. JDK Version specified is 11`

When the toolchain JDK is applied for a task, you will see a message like:

`CWWKM4101I: The :libertyDev task is using the configured toolchain JDK located at /path/to/jdk-11`

If `JAVA_HOME` is set in `server.env` or `jvm.options`, you will see a warning like:

`CWWKM4101W: The toolchain JDK configuration for task :libertyDev is not honored because the JAVA_HOME property is specified in the server.env or jvm.options file`


### libertyDev

Dev mode uses the configured Gradle Java toolchain in two places.

#### Liberty server JVM

When a Java toolchain is configured, dev mode sets `JAVA_HOME` for the Liberty server process using the resolved toolchain JDK home (unless `JAVA_HOME` is already set in `server.env` or `jvm.options`).

To confirm which JDK the server started with, check `messages.log` for a `java.version` entry.

Example (`messages.log`):

```text
********************************************************************************
product = Open Liberty 25.0.0.12 (wlp-1.0.108.cl251220251117-0302)
wlp.install.dir = /path/to/project/build/wlp/
java.home = /path/to/java/semeru-11.0.28/Contents/Home
java.version = 11.0.28
java.runtime = IBM Semeru Runtime Open Edition (11.0.28+6)
os = Mac OS X (26.1; aarch64) (en_IN)
process = 62955@Device-Name.local
Classpath = /path/to/project/build/wlp/bin/tools/ws-server.jar
Java Library path = /path/to/java/semeru-11.0.28/Contents/Home/lib/default:/path/to/java/semeru-11.0.28/Contents/Home/lib:/usr/lib
********************************************************************************
```

#### Build recompilation and tests

When files change, dev mode triggers standard Gradle tasks:

* `compileJava`, `processResources`
* `compileTestJava`, `processTestResources`
* `cleanTest`, `test` (when dev mode runs tests)

The JDK used for compilation is controlled by the Gradle toolchain configuration.

To show which toolchain is being used when dev mode triggers compilation, dev mode logs messages like:

* `Using Java toolchain for dev mode compilation: version=11, javaHome=/path/to/jdk-11`
* `Using Java toolchain for dev mode test compilation: version=11, javaHome=/path/to/jdk-11`


### Rules and precedence

* **No Gradle toolchain configured (`java.toolchain` absent)**

  Server tasks and dev mode run using the JVM that runs Gradle and whatever configuration is present in `server.env` / `jvm.options`.
  If `JAVA_HOME` is not set in those files, Liberty uses the default `JAVA_HOME` from the system environment (or the JVM that is running Gradle).

* **Gradle toolchain configured, toolchain JDK resolved successfully**

  For Liberty server tasks, if `JAVA_HOME` is *not* specified in `server.env` or `jvm.options`, the plugin sets `JAVA_HOME` to the resolved toolchain JDK home. The build output includes `CWWKM4101I` indicating which toolchain JDK is used.

  For dev mode recompilation, the toolchain used for compilation is controlled by Gradle. Dev mode logs the toolchain version and the resolved `javaHome` when it triggers compilation.

* **Gradle toolchain configured, but toolchain JDK cannot be resolved**

  The plugin logs a warning indicating that the toolchain cannot be honored. Server tasks fall back to using the JVM that runs Gradle (or any existing `JAVA_HOME` settings).

* **`JAVA_HOME` set in `server.env` or `jvm.options`**
 
   `JAVA_HOME` takes precedence over the toolchain JDK for the Liberty server JVM. This includes `JAVA_HOME` provided through the `liberty { server { env ... } }` configuration or `liberty.server.env.*` Gradle project properties. The plugin logs a warning (`CWWKM4101W`) indicating the toolchain is not honored.


### Troubleshooting

**If you do not see toolchain log messages (`CWWKM4100I`, `CWWKM4101I`, or the dev mode toolchain compilation logs):**

* Verify that your project applies the `java` plugin.
* Verify that `java { toolchain { languageVersion = JavaLanguageVersion.of(11) } }` (or another version) is configured.
* Ensure that Gradle can resolve the requested toolchain JDK by using a locally installed JDK or by automatically downloading one via a [provisioning plugin](https://docs.gradle.org/current/userguide/toolchains.html#sec:provisioning).

 **If Liberty does not appear to run with the toolchain JDK (for example, `java.version` in `messages.log` does not match the configured toolchain):**
 
 * Check `server.env` and `jvm.options` for `JAVA_HOME`.
   * If `JAVA_HOME` is present there, it overrides the toolchain. Remove it if you want the toolchain JDK to be used.
 * Also check whether your build is setting `JAVA_HOME` through the `liberty` server configuration (for example, `server { env = ... }`) or through `liberty.server.env.*` Gradle project properties.
 * Check the build logs for warnings indicating that the toolchain is not honored.

**If dev mode recompilation does not show the toolchain compilation logs:**

* Make sure your changes are in `src/main/java` or `src/test/java` so that dev mode triggers `compileJava` / `compileTestJava`.
* Check `output.log` from dev mode for lines starting with:
  * `Using Java toolchain for dev mode compilation: ...`
  * `Using Java toolchain for dev mode test compilation: ...`
