/**
 * (C) Copyright IBM Corporation 2019.
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
package io.openliberty.tools.gradle.tasks

import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.SourceSet
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.options.Option
import org.gradle.tooling.BuildException
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.GradleConnector

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor

import io.openliberty.tools.ant.ServerTask

import io.openliberty.tools.common.plugins.util.DevUtil
import io.openliberty.tools.common.plugins.util.PluginExecutionException
import io.openliberty.tools.common.plugins.util.PluginScenarioException
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil
import io.openliberty.tools.common.plugins.util.ServerStatusUtil

import java.util.concurrent.TimeUnit

class DevTask extends AbstractServerTask {

    DevTask() {
        configure({
            description 'Runs a Liberty server in dev mode'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })
    }

    DevTaskUtil util = null;

    // Default DevMode argument values
    // DevMode uses CLI Arguments if provided, otherwise it uses ServerExtension properties if one exists, fallback to default value if neither are provided.
    private static final int DEFAULT_VERIFY_TIMEOUT = 30;
    private static final int DEFAULT_SERVER_TIMEOUT = 30;
    private static final double DEFAULT_COMPILE_WAIT = 0.5;
    private static final int DEFAULT_DEBUG_PORT = 7777;
    private static final boolean DEFAULT_HOT_TESTS = false;
    private static final boolean  DEFAULT_SKIP_TESTS = false;
    private static final boolean DEFAULT_LIBERTY_DEBUG = true;


    private Boolean hotTests;

    @Option(option = 'hotTests', description = 'If this option is enabled, run tests automatically after every change. The default value is false.')
    void setHotTests(boolean hotTests) {
        this.hotTests = hotTests;
    }

    private Boolean skipTests;

    @Option(option = 'skipTests', description = 'If this option is enabled, do not run any tests in dev mode. The default value is false.')
    void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }

    Boolean libertyDebug;

    // Need to use a string value to allow someone to specify --libertyDebug=false
    // bool @Options only allow you to specify "--libertyDebug" or nothing.
    // So there is no way to explicitly set libertyDebug to false if we want the default behavior to be true
    @Option(option = 'libertyDebug', description = 'Whether to allow attaching a debugger to the running server. The default value is true.')
    void setLibertyDebug(String libertyDebug) {
        this.libertyDebug = Boolean.parseBoolean(libertyDebug)
    }

    Integer libertyDebugPort;

    @Option(option = 'libertyDebugPort', description = 'The debug port that you can attach a debugger to. The default value is 7777.')
    void setLibertyDebugPort(String libertyDebugPort) {
        try {
            this.libertyDebugPort = libertyDebugPort.toInteger();
        } catch (NumberFormatException e) {
            logger.error(String.format("Unexpected value: %s for dev mode option libertyDebugPort. libertyDebugPort should be a valid integer.", libertyDebugPort));
            throw e;
        }
    }

    private Double compileWait;

    @Option(option = 'compileWait', description = 'Time in seconds to wait before processing Java changes and deletions. The default value is 0.5 seconds.')
    void setCompileWait(String compileWait) {
        try {
            this.compileWait = compileWait.toDouble();
        } catch (NumberFormatException e) {
            logger.error(String.format("Unexpected value: %s for dev mode option compileWait. compileWait should be a valid number.", compileWait));
            throw e;
        }
    }

    private Integer verifyAppStartTimeout;

    @Option(option = 'verifyAppStartTimeout', description = 'Maximum time to wait (in seconds) to verify that the application has started or updated before running tests. The default value is 30 seconds.')
    void setVerifyAppStartTimeout(String verifyAppStartTimeout) {
        try {
            this.verifyAppStartTimeout = verifyAppStartTimeout.toInteger();
        } catch (NumberFormatException e) {
            logger.error(String.format("Unexpected value: %s for dev mode option verifyAppStartTimeout. verifyAppStartTimeout should be a valid integer.", verifyAppStartTimeout));
            throw e;
        }
    }

    private Integer serverStartTimeout;

    @Option(option = 'serverStartTimeout', description = 'Time in seconds to wait while verifying that the server has started. The default value is 30 seconds.')
    void setServerStartTimeout(String serverStartTimeout) {
        try {
            this.serverStartTimeout = serverStartTimeout.toInteger();
        } catch (NumberFormatException e) {
            logger.error(String.format("Unexpected value: %s for dev mode option serverStartTimeout. serverStartTimeout should be a valid integer.", serverStartTimeout));
            throw e;
        }
    }

    Boolean clean;

    @Option(option = 'clean', description = 'Clean all cached information on server start up. The default value is false.')
    void setClean(boolean clean) {
        this.clean = clean;
    }

    File sourceDirectory;

    File testSourceDirectory;

    private class DevTaskUtil extends DevUtil {

        Set<String> existingFeatures;

        private ServerTask serverTask = null;

        DevTaskUtil(File serverDirectory, File sourceDirectory, File testSourceDirectory,
                    File configDirectory, List<File> resourceDirs, boolean  hotTests,
                    boolean  skipTests, String artifactId, int serverStartTimeout,
                    int verifyAppStartTimeout, int appUpdateTimeout, double compileWait, boolean libertyDebug
        ) throws IOException {
            super(serverDirectory, sourceDirectory, testSourceDirectory, configDirectory, resourceDirs,
                    hotTests, skipTests, false, false, artifactId,  serverStartTimeout,
                    verifyAppStartTimeout, appUpdateTimeout, ((long) (compileWait * 1000L)), libertyDebug, true);

            ServerFeature servUtil = getServerFeatureUtil();
            this.existingFeatures = servUtil.getServerFeatures(serverDirectory);
        }

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
                    serverTaskStop.execute();
                } else {
                    logger.error('There is no server to stop. The server has not been created.');
                }
            } else {
                logger.error('There is no server to stop. The runtime has not been installed.');
            }
        }

        @Override
        public ServerTask getServerTask() throws Exception {
            if (serverTask != null) {
                return serverTask;
            }

            copyConfigFiles();

            if (libertyDebug) {
                serverTask = createServerTask(project, "debug");
                setLibertyDebugPort(libertyDebugPort);
                serverTask.setEnvironmentVariables(getDebugEnvironmentVariables());
            } else {
                serverTask = createServerTask(project, "run");
            }

            serverTask.setClean(clean);

            return serverTask;
        }

        @Override
        public List<String> getArtifacts() {
            // https://docs.gradle.org/current/userguide/java_plugin.html#sec:java_plugin_and_dependency_management
            String[] dependencyConfigurationNames = ['compileClasspath', 'testCompileClasspath'];

            Set<String> artifactPaths = new HashSet<String>();

            dependencyConfigurationNames.each { name ->
               def configuration = project.configurations.getByName(name);
                configuration.resolvedConfiguration.resolvedArtifacts.each { artifact ->
                    artifactPaths.add(artifact.file.getCanonicalPath());
               }
            }

            return artifactPaths.toList();
        }

        @Override
        public boolean recompileBuildFile(File buildFile, List<String> artifactPaths, ThreadPoolExecutor executor) {
            // TODO:
        }

        @Override
        public void checkConfigFile(File configFile, File serverDir) {
            ServerFeature servUtil = getServerFeatureUtil();
            Set<String> features = servUtil.getServerFeatures(serverDir);

            if (features == null) {
                return;
            }

            features.removeAll(existingFeatures);

            if (!features.isEmpty()) {
                logger.info("Configuration features have been added");

                // Call the installFeature gradle task using the temporary serverDir directory that DevMode uses
                ProjectConnection gradleConnection = initGradleProjectConnection();
                BuildLauncher gradleBuildLauncher = gradleConnection.newBuild();
                try {
                    runGradleTask(gradleBuildLauncher, "installFeature", "--serverDir=${serverDir.getAbsolutePath()}");
                    this.existingFeatures.addAll(features);
                } catch (BuildException e) {
                    logger.error('Failed to install features from configuration file', e);
                } finally {
                    gradleConnection.close();
                }
            }
        }

        @Override
        public boolean compile(File dir) {
            ProjectConnection gradleConnection = initGradleProjectConnection();
            BuildLauncher gradleBuildLauncher = gradleConnection.newBuild();

            try {
                if (dir.equals(sourceDirectory)) {
                    runGradleTask(gradleBuildLauncher, 'compileJava', 'processResources');
                }

                if (dir.equals(testSourceDirectory)) {
                    runGradleTask(gradleBuildLauncher, 'compileTestJava', 'processTestResources');
                }
                return true;
            } catch (BuildException e) {
                logger.error('Unable to compile', e);
                return false;
            } finally {
                gradleConnection.close();
            }
        }

        @Override
        public void runUnitTests() throws PluginExecutionException, PluginScenarioException {
            // Not needed for gradle.
        }

        @Override
        public void runIntegrationTests() throws PluginExecutionException, PluginScenarioException {
            ProjectConnection gradleConnection = initGradleProjectConnection();
            BuildLauncher gradleBuildLauncher = gradleConnection.newBuild();

            try {
                // Force tests to run by calling cleanTest first
                // otherwise tests may be skipped with an UP-TO-DATE message
                // https://docs.gradle.org/current/userguide/java_testing.html#sec:forcing_java_tests_to_run
                runGradleTask(gradleBuildLauncher, 'cleanTest', 'test');
            } catch (BuildException e) {
                // Gradle throws a build exception if tests fail
                // catch it and do nothing
            } finally {
                gradleConnection.close();
            }
        }

        @Override
        public void redeployApp() {
            // TODO:
        }

        @Override
        public void libertyInstallFeature() {
            // TODO: 
        }

        @Override
        public void libertyDeploy() {
            // TODO: 
        }

        @Override
        public void libertyCreate() {
            // TODO: 
        }
    }

    // If a argument has not been set using CLI arguments set a default value
    // Using the ServerExtension properties if available, otherwise use hardcoded defaults
    private void initializeDefaultValues() {
        if (verifyAppStartTimeout == null) {
            if (server.verifyAppStartTimeout != 0) {
                verifyAppStartTimeout = server.verifyAppStartTimeout;
            } else {
                verifyAppStartTimeout = DEFAULT_VERIFY_TIMEOUT;
            }
        }

        if (serverStartTimeout == null) {
            if (server.timeout != null && server.timeout.isInteger()) {
                serverStartTimeout = server.timeout.toInteger();
            } else {
                serverStartTimeout = DEFAULT_SERVER_TIMEOUT;
            }
        }

        if (clean == null) {
            clean = server.clean;
        }

        if (compileWait == null) {
            compileWait = DEFAULT_COMPILE_WAIT;
        }

        if (libertyDebugPort == null) {
            libertyDebugPort = DEFAULT_DEBUG_PORT;
        }

        if (hotTests == null) {
            hotTests = DEFAULT_HOT_TESTS;
        }

        if (skipTests == null) {
            skipTests = DEFAULT_SKIP_TESTS;
        }

        if (libertyDebug == null) {
            libertyDebug = DEFAULT_LIBERTY_DEBUG;
        }
    }

    @TaskAction
    void action() {
        initializeDefaultValues();

        SourceSet mainSourceSet = project.sourceSets.main;
        SourceSet testSourceSet = project.sourceSets.test;

        sourceDirectory = mainSourceSet.java.srcDirs.iterator().next()
        testSourceDirectory = testSourceSet.java.srcDirs.iterator().next()
        File outputDirectory = mainSourceSet.java.outputDir;
        File testOutputDirectory = testSourceSet.java.outputDir;
        List<File> resourceDirs = mainSourceSet.resources.srcDirs.toList();

        File serverDirectory = getServerDir(project);
        String serverName = server.name;
        File serverInstallDir = getInstallDir(project);
        // make sure server.configDirectory is set before accessing it
        initializeConfigDirectory();
        File configDirectory = server.configDirectory;
        // getOutputDir returns a string
        File serverOutputDir = new File(getOutputDir(project));

        if (serverDirectory.exists()) {
            if (ServerStatusUtil.isServerRunning(serverInstallDir, serverOutputDir, serverName)) {
                throw new Exception("The server " + serverName
                        + " is already running. Terminate all instances of the server before starting dev mode.");
            }
        }

        if (resourceDirs.isEmpty()) {
            File defaultResourceDir = new File(project.getRootDir() + "/src/main/resources");
            logger.info("No resource directory detected, using default directory: " + defaultResourceDir);
            resourceDirs.add(defaultResourceDir);
        }

        String artifactId = project.getName();

        // create an executor for tests with an additional queue of size 1, so
        // any further changes detected mid-test will be in the following run
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(1, true));

        ProjectConnection gradleConnection = initGradleProjectConnection();
        BuildLauncher gradleBuildLauncher = gradleConnection.newBuild();
        try {
            /*
            Running the deploy task runs all tasks it depends on:
                :libertyStop
                :clean
                :installLiberty
                :libertyCreate
                :compileJava
                :processResources
                :classes
                :war
                :deploy
             */
            runGradleTask(gradleBuildLauncher, 'libertyCreate');
            runGradleTask(gradleBuildLauncher, 'installFeature');
            runGradleTask(gradleBuildLauncher, 'deploy');
        } finally {
            gradleConnection.close();
        }

        util = new DevTaskUtil(
                serverDirectory, sourceDirectory, testSourceDirectory, configDirectory,
                resourceDirs, hotTests.booleanValue(), skipTests.booleanValue(), artifactId, serverStartTimeout.intValue(),
                verifyAppStartTimeout.intValue(), verifyAppStartTimeout.intValue(), compileWait.doubleValue(), libertyDebug.booleanValue()
        );

        util.addShutdownHook(executor);

        util.startServer();

        List<String> artifactPaths = util.getArtifacts();
        File buildFile = project.getBuildFile();
        File serverXMLFile = server.serverXmlFile;

        if (hotTests && testSourceDirectory.exists()) {
        // if hot testing, run tests on startup and then watch for keypresses
        util.runTestThread(false, executor, -1, false, false);
        } else {
            // else watch for key presses immediately
            util.runHotkeyReaderThread(executor);
        }

        // Note that serverXMLFile can be null. DevUtil will automatically watch
        // all files in the configDirectory,
        // which is where the server.xml is located if a specific serverXmlFile
        // configuration parameter is not specified.
        try {
            util.watchFiles(buildFile, outputDirectory, testOutputDirectory, executor, artifactPaths, serverXMLFile);
        } catch (PluginScenarioException e) { // this exception is caught when the server has been stopped by another process
            logger.info(e.getMessage());
            return; // enter shutdown hook
        }
    }

    ProjectConnection initGradleProjectConnection() {
        return initGradleConnection(project.getRootDir());
    }

    static ProjectConnection initGradleConnection(File rootDir) {
        ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(rootDir)
                .connect();

        return connection;
    }

    static void runGradleTask(BuildLauncher buildLauncher, String ... tasks)  {
        buildLauncher
                .setStandardOutput(System.out)
                .setStandardError(System.err);
        buildLauncher.forTasks(tasks);
        buildLauncher.run();
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
