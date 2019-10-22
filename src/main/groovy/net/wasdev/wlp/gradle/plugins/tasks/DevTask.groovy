/**
 * (C) Copyright IBM Corporation 2014, 2019.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.wasdev.wlp.gradle.plugins.tasks

import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.SourceSet
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.options.Option;
import org.gradle.api.tasks.Input

import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.GradleConnector

import java.util.concurrent.ThreadPoolExecutor;

import io.openliberty.tools.ant.ServerTask

import io.openliberty.tools.common.plugins.util.DevUtil
import io.openliberty.tools.common.plugins.util.PluginExecutionException
import io.openliberty.tools.common.plugins.util.PluginScenarioException
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil

class DevTask extends AbstractServerTask {

    DevTask() {
        configure({
            description "Runs a Liberty dev server"
            logging.level = LogLevel.INFO
            group 'Liberty'
        })
    }

    DevTaskUtil util = null;

    /**
     * Hot tests
     */
    private boolean hotTests = false;

    @Option(option = 'hotTests', description = 'TODO')
    void setHotTests(boolean hotTests) {
        this.hotTests = hotTests;
    }

    /**
     * Skip tests
     */
    private boolean skipTests = false;

    @Option(option = 'skipTests', description = 'Skip tests.')
    void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }

    /**
     * Skip unit tests
     */
    private boolean skipUTs = false;

    @Option(option = 'skipUTs', description = 'Skip unit tests.')
    void setSkipUTs(boolean skipUTs) {
        this.skipUTs = skipUTs;
    }

    /**
     * Skip integration tests
     */
    private boolean skipITs = false;

    @Option(option = 'skipITs', description = 'Skip integration tests.')
    void setSkipITs(boolean skipITs) {
        this.skipITs = skipITs;
    }

    /**
     * Debug
     */
    private boolean libertyDebug = true;

    @Option(option = 'debug', description = 'TODO')
    void setLibertyDebug(boolean libertyDebug) {
        this.libertyDebug = libertyDebug;
    }

    /**
     * Debug Port
     */
    private int libertyDebugPort = 7777;

    @Option(option = 'debugPort', description = 'Liberty debug port.')
    void setLibertyDebugPort(String libertyDebugPort) {
        if (libertyDebugPort.isInteger()) {
            this.libertyDebugPort = libertyDebugPort.toInteger();
        }
    }

    /**
     * Time in seconds to wait before processing Java changes and deletions.
     */
    private double compileWait = 0.5;

    @Option(option = 'compileWait', description = 'Time in seconds to wait before processing Java changes and deletions.')
    void setCompileWait(String compileWait) {
        if (compileWait.isDouble()) {
            this.compileWait = compileWait.toDouble();
        }
    }

    private int runId = 0;

    private ServerTask serverTask = null;

    // private Plugin boostPlugin = null;

    /**
     * Time in seconds to wait while verifying that the application has started.
     */
    private int verifyTimeout = 30;

    @Option(option = 'verifyTimeout', description = 'Time in seconds to wait while verifying that the application has started.')
    void setVerifyTimeout(String verifyTimeout) {
        if (verifyTimeout.isInteger()) {
            this.verifyTimeout = verifyTimeout.toInteger();
        }
    }

    /**
     * Time in seconds to wait while verifying that the application has updated.
     */
    private int appUpdateTimeout = 5;

    @Option(option = 'appUpdateTimeout', description = 'Time in seconds to wait while verifying that the application has updated.')
    void setAppUpdateTimeout(String appUpdateTimeout) {
        if (appUpdateTimeout.isInteger()) {
            this.appUpdateTimeout = appUpdateTimeout.toInteger();
        }
    }

    /**
     * Time in seconds to wait while verifying that the server has started.
     */
    private int serverStartTimeout = 30;

    @Option(option = 'serverStartTimeout', description = 'Time in seconds to wait while verifying that the server has started.')
    void setServerStartTimeout(String serverStartTimeout) {
        if (serverStartTimeout.isInteger()) {
            this.serverStartTimeout = serverStartTimeout.toInteger();
        }
    }

    /**
     * comma separated list of app names to wait for
     */
    private String applications;

    @Option(option = 'applications', description = 'Comma separated list of app names to wait for')
    void setApplications(String applications) {
        this.applications = applications;
    }

    /**
     * Clean all cached information on server start up.
     */
    private boolean clean = false;

    @Option(option = 'clean', description = 'Clean all cached information on server start up.')
    void setClean(boolean clean) {
        this.clean = clean;
    }


    private class DevTaskUtil extends DevUtil {

        Set<String> existingFeatures;


        DevTaskUtil(File serverDirectory, File sourceDirectory, File testSourceDirectory, File configDirectory,
                           List<File> resourceDirs, boolean  hotTests, boolean  skipTests, boolean  skipUTs, boolean  skipITs, String artifactId, int verifyTimeout, int appUpdateTimeout, double compileWait, boolean libertyDebug) throws IOException {
            super(serverDirectory, sourceDirectory, testSourceDirectory, configDirectory, resourceDirs, hotTests,
                    skipTests, skipUTs, skipITs, artifactId, verifyTimeout, appUpdateTimeout,
                    ((long) (compileWait * 1000L)), libertyDebug);

            ServerFeature servUtil = getServerFeatureUtil();
            this.existingFeatures = servUtil.getServerFeatures(serverDirectory);
        }

        @Override
        public void debug(String msg) {
            logger.warn(msg); // TODO: Change these back to proper method
        }

        @Override
        public void debug(String msg, Throwable e) {
            logger.warn(msg, e);
        }

        @Override
        public void debug(Throwable e) {
            logger.warn(e);
        }

        @Override
        public void warn(String msg) {
            logger.warn(msg);
        }

        @Override
        public void info(String msg) {
            logger.warn(msg);
        }

        @Override
        public void error(String msg) {
            logger.error(msg);
        }

        @Override
        public void error(String msg, Throwable e) {
            logger.error(msg, e);
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isEnabled(LogLevel.DEBUG);
        }

        @Override
        public void stopServer() {
            if (isLibertyInstalled(project)) {
                if (getServerDir(project).exists()) {
                    ServerTask serverTaskStop = createServerTask(project, "stop");
                    serverTaskStop.setUseEmbeddedServer(server.embedded)
                    serverTaskStop.execute()
                } else {
                    logger.error ('There is no server to stop. The server has not been created.')
                }
            } else {
                logger.error ('There is no server to stop. The runtime has not been installed.')
            }
        }

        @Override
        public ServerTask getServerTask() throws Exception {
            ServerTask serverTaskStart = createServerTask(project, "start");
            serverTaskStart.setUseEmbeddedServer(server.embedded)
            serverTaskStart.setClean(server.clean)
            serverTaskStart.execute();

            return serverTaskStart;
        }

        @Override
        public List<String> getArtifacts() {
            // TODO:

        }

        @Override
        public boolean recompileBuildFile(File buildFile, List<String> artifactPaths, ThreadPoolExecutor executor) {
            // TODO:
        }

        @Override
        public void checkConfigFile(File configFile, File serverDir) {
            // TODO:
        }

        @Override
        public boolean compile(File dir) {
            ProjectConnection connection = GradleConnector.newConnector()
                    .forProjectDirectory(new File("."))
                    .connect();

            try {
                BuildLauncher gradleBuildLauncher = connection.newBuild()
                        .setStandardOutput(System.out)
                        .setStandardError(System.err);

                if (dir.equals(sourceDirectory)) {
                    runGradleTask(gradleBuildLauncher, 'compileJava');
                    runGradleTask(gradleBuildLauncher, 'processResources');
                }

                if (dir.equals(testSourceDirectory)) {
                    runGradleTask(gradleBuildLauncher, 'compileTestJava');
                    runGradleTask(gradleBuildLauncher, 'processTestResources');
                }

            } finally {
                connection.close();
            }
        }

        @Override
        public void runUnitTests() throws PluginExecutionException, PluginScenarioException {
            // TODO:
        }

        @Override
        public void runIntegrationTests() throws PluginExecutionException, PluginScenarioException {
            // TODO:
        }
    }

    void runGradleTask(BuildLauncher buildLauncher, String ... tasks) {
        buildLauncher.forTasks(tasks);
        buildLauncher.run();
    }

    @TaskAction
    void action() {
        // https://docs.gradle.org/current/userguide/embedding.html Tooling API docs
        // Represents a long-lived connection to a Gradle project.
        ProjectConnection connection = GradleConnector.newConnector()
            .forProjectDirectory(new File("."))
            .connect();

        try {
            // configure a gradle build launcher
            // you can reuse the launcher to launch additional builds.
            BuildLauncher gradleBuildLauncher = connection.newBuild()
                    .setStandardOutput(System.out)
                    .setStandardError(System.err);

            SourceSet mainSourceSet = project.sourceSets.main;
            SourceSet testSourceSet = project.sourceSets.test;

            File sourceDirectory = mainSourceSet.java.srcDirs.iterator().next()
            File testSourceDirectory = testSourceSet.java.srcDirs.iterator().next()
            File outputDirectory = mainSourceSet.java.outputDir;
            File testOutputDirectory = testSourceSet.java.outputDir;
            File serverDirectory = getServerDir(project);
            File configDirectory = new File(project.projectDir, "src/main/liberty/config");
            List<File> resourceDirs = Arrays.asList(mainSourceSet.resources.srcDirs.toArray());

            String artifactId = '' // TODO: Find where to get this

            println "Hot tests: " + this.hotTests
            println "Skip tests: " + this.skipTests
            println "SKip UTs: " + this.skipUTs
            println "Skip ITs: " + this.skipITs
            println "libertyDebug: " + this.libertyDebug
            println "libertyDebugPort: " + this.libertyDebugPort
            println "Compile wait: " + this.compileWait
            println "verifyTimeout: " + this.verifyTimeout
            println "appUpdateTimeout: " + this.appUpdateTimeout
            println "serverStartTimeout: " + this.serverStartTimeout
            println "applications" + this.applications
            println "clean: " + this.clean
            println "Server directory" + serverDirectory;
            println "Config directory" + configDirectory;
            println "Source directory: " + sourceDirectory;
            println "Output directory: " + outputDirectory;
            println"Test Source directory: " + testSourceDirectory;
            println"Test Output directory: " + testOutputDirectory;
            println"Resource directories" + resourceDirs;

            runGradleTask(gradleBuildLauncher, 'compileJava');
            runGradleTask(gradleBuildLauncher, 'processResources');
            runGradleTask(gradleBuildLauncher, 'compileTestJava');
            runGradleTask(gradleBuildLauncher, 'processTestResources');

//            final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
//                new ArrayBlockingQueue<Runnable>(1, true));

            util = new DevTaskUtil(serverDirectory, sourceDirectory, testSourceDirectory, configDirectory, resourceDirs, hotTests, skipTests, skipUTs, skipITs, artifactId, verifyTimeout, appUpdateTimeout, compileWait, libertyDebug);
//            util.addShutdownHook(executor);
            util.startServer(serverStartTimeout);

//          runGradleTask(gradleBuildLauncher, 'libertyStart');

        } finally {
            connection.close();
        }
    }


    private static ServerFeature serverFeatureUtil;

    private ServerFeature getServerFeatureUtil() {
        if (serverFeatureUtil == null) {
            serverFeatureUtil = new ServerFeature();
        }
        return serverFeatureUtil;
    }

    private class ServerFeature extends ServerFeatureUtil {

        @Override
        public void debug(String msg) {
            logger.debug(msg);
        }

        @Override
        public void debug(String msg, Throwable e) {
            logger.debug(msg, e);
        }

        @Override
        public void debug(Throwable e) {
            logger.debug(e);
        }

        @Override
        public void warn(String msg) {
            logger.warn(msg);
        }

        @Override
        public void info(String msg) {
            logger.info(msg);
        }

    }
}
