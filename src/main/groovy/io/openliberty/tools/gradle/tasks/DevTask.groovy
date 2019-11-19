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
package io.openliberty.tools.gradle.tasks

import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.SourceSet
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.options.Option
import org.gradle.tooling.BuildException;
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.GradleConnector

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor;

import io.openliberty.tools.ant.ServerTask

import io.openliberty.tools.common.plugins.util.DevUtil
import io.openliberty.tools.common.plugins.util.PluginExecutionException
import io.openliberty.tools.common.plugins.util.PluginScenarioException
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil
import io.openliberty.tools.common.plugins.util.ServerStatusUtil;


import java.util.concurrent.TimeUnit

class DevTask extends AbstractServerTask {

    DevTask() {
        configure({
            description "Runs a Liberty dev server"
            logging.level = LogLevel.INFO
            group 'Liberty'
        })
    }

    DevTaskUtil util = null;

    private boolean hotTests = false;

    @Option(option = 'hotTests', description = 'Tests are run immediately after dev mode starts up and automatically after source changes.')
    void setHotTests(boolean hotTests) {
        this.hotTests = hotTests;
    }

    private boolean skipTests = false;

    @Option(option = 'skipTests', description = 'Skip tests.')
    void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }

//    Only calling a single `gradle test` command, no need to have options to
//    skipITs vs skipUTs, just a single skipTests flag
//    ?? Could be needed later if we can separate out ITs vs UTs

    private boolean skipUTs = false;
//    @Option(option = 'skipUTs', description = 'Skip unit tests.')
//    void setSkipUTs(boolean skipUTs) {
//        this.skipUTs = skipUTs;
//    }

    private boolean skipITs = false;
//    @Option(option = 'skipITs', description = 'Skip integration tests.')
//    void setSkipITs(boolean skipITs) {
//        this.skipITs = skipITs;
//    }

    boolean libertyDebug = false;

    @Option(option = 'libertyDebug', description = 'TODO')
    void setLibertyDebug(boolean libertyDebug) {
        this.libertyDebug = libertyDebug;
    }

    int libertyDebugPort = 7777;

    @Option(option = 'debugPort', description = 'Liberty debug port.')
    void setLibertyDebugPort(String libertyDebugPort) {
        if (libertyDebugPort.isInteger()) {
            this.libertyDebugPort = libertyDebugPort.toInteger();
        }
    }

    private double compileWait = 0.5;

    @Option(option = 'compileWait', description = 'Time in seconds to wait before processing Java changes and deletions.')
    void setCompileWait(String compileWait) {
        if (compileWait.isDouble()) {
            this.compileWait = compileWait.toDouble();
        }
    }

    private int verifyTimeout = 30;

    @Option(option = 'verifyTimeout', description = 'Time in seconds to wait while verifying that the application has started.')
    void setVerifyTimeout(String verifyTimeout) {
        if (verifyTimeout.isInteger()) {
            this.verifyTimeout = verifyTimeout.toInteger();
        }
    }

    private int appUpdateTimeout = 5;

    @Option(option = 'appUpdateTimeout', description = 'Time in seconds to wait while verifying that the application has updated.')
    void setAppUpdateTimeout(String appUpdateTimeout) {
        if (appUpdateTimeout.isInteger()) {
            this.appUpdateTimeout = appUpdateTimeout.toInteger();
        }
    }

    private int serverStartTimeout = 30;

    @Option(option = 'serverStartTimeout', description = 'Time in seconds to wait while verifying that the server has started.')
    void setServerStartTimeout(String serverStartTimeout) {
        if (serverStartTimeout.isInteger()) {
            this.serverStartTimeout = serverStartTimeout.toInteger();
        }
    }

    boolean clean = false;

    @Option(option = 'clean', description = 'Clean all cached information on server start up.')
    void setClean(boolean clean) {
        this.clean = clean;
    }

    File sourceDirectory;

    File testSourceDirectory;

    private class DevTaskUtil extends DevUtil {

        Set<String> existingFeatures;

        private ServerTask serverTask = null;

        DevTaskUtil(File serverDirectory, File sourceDirectory, File testSourceDirectory,
                    File configDirectory, List<File> resourceDirs, boolean  hotTests, boolean  skipTests,
                    boolean  skipUTs, boolean  skipITs, String artifactId, int serverStartTimeout,
                    int verifyTimeout, int appUpdateTimeout, double compileWait, boolean libertyDebug
        ) throws IOException {
            super(serverDirectory, sourceDirectory, testSourceDirectory, configDirectory, resourceDirs, hotTests,
                    skipTests, skipUTs, skipITs, artifactId,  serverStartTimeout, verifyTimeout, appUpdateTimeout,
                    ((long) (compileWait * 1000L)), libertyDebug);
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
                    serverTaskStop.execute()
                } else {
                    logger.error('There is no server to stop. The server has not been created.')
                }
            } else {
                logger.error('There is no server to stop. The runtime has not been installed.')
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
            def dependencyConfigurationNames = ['compile', 'compileOnly', 'testCompileOnly'];

            Set<File> artifactFiles = new HashSet<File>()

            dependencyConfigurationNames.each { name ->
               def configuration = project.configurations.getByName(name);
                configuration.resolvedConfiguration.resolvedArtifacts.each { artifact ->
                   artifactFiles.add(artifact.file);
               }
           }

            List<String> artifactPaths = new ArrayList<String>();

            for (File file : artifactFiles) {
                artifactPaths.add(file.getCanonicalPath());
            }

            return artifactPaths;

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
                logger.error("Unable to compile");
                return false;
            } finally {
                gradleConnection.close();
            }
        }

        @Override
        public void runUnitTests() throws PluginExecutionException, PluginScenarioException {
            // NOT NEEDED FOR GRADLE ??
        }

        @Override
        public void runIntegrationTests() throws PluginExecutionException, PluginScenarioException {
            ProjectConnection gradleConnection = initGradleProjectConnection();
            BuildLauncher gradleBuildLauncher = gradleConnection.newBuild();

            try {
                // Force tests to run by calling cleanTest first
                // otherwise tests may be skipped with an UP-TO-DATE message
                // https://docs.gradle.org/current/userguide/java_testing.html#sec:forcing_java_tests_to_run
                runGradleTask(gradleBuildLauncher, 'cleanTest', 'test')
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

    @TaskAction
    void action() {
            SourceSet mainSourceSet = project.sourceSets.main;
            SourceSet testSourceSet = project.sourceSets.test;

            sourceDirectory = mainSourceSet.java.srcDirs.iterator().next()
            testSourceDirectory = testSourceSet.java.srcDirs.iterator().next()
            File outputDirectory = mainSourceSet.java.outputDir;
            File testOutputDirectory = testSourceSet.java.outputDir;
            File serverDirectory = getServerDir(project);
            File configDirectory = new File(project.projectDir, "src/main/liberty/config");
            List<File> resourceDirs = mainSourceSet.resources.srcDirs.toList();

            String serverName = server.name;
            File serverInstallDir = getInstallDir(project);
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
                Running the installApps task runs all tasks it depends on:
                    :libertyStop
                    :clean
                    :installLiberty
                    :libertyCreate
                    :compileJava
                    :processResources
                    :classes
                    :war
                    :installApps
                 */
                runGradleTask(gradleBuildLauncher, 'libertyCreate');
                runGradleTask(gradleBuildLauncher, 'installFeature');
                runGradleTask(gradleBuildLauncher, 'installApps');
            } finally {
                gradleConnection.close();
            }

            util = new DevTaskUtil(
                    serverDirectory, sourceDirectory, testSourceDirectory, configDirectory, resourceDirs,
                    hotTests, skipTests, skipUTs, skipITs, artifactId,
                    serverStartTimeout, verifyTimeout, appUpdateTimeout, compileWait, libertyDebug
            );

//            Use the gradle compile task instead of using the DevUtil compile
//            util.setUseMavenOrGradleCompile(true);

            util.addShutdownHook(executor);

            util.startServer();

            List<String> artifactPaths = util.getArtifacts();
            File buildFile = project.getBuildFile()
            File serverXMLFile = getServerXMLFile(server);

            if (hotTests && testSourceDirectory.exists()) {
            // if hot testing, run tests on startup and then watch for keypresses
            util.runTestThread(false, executor, -1, false, false);
            } else {
                // else watch for keypresses immediately
                util.runHotkeyReaderThread(executor);
            }

            try {
                util.watchFiles(buildFile, outputDirectory, testOutputDirectory, executor, artifactPaths, serverXMLFile);
            } catch (PluginScenarioException e) { // this exception is caught when the server has been stopped by another process
                logger.info(e.getMessage());
                return; // enter shutdown hook
            }
    }

    static ProjectConnection initGradleProjectConnection() {
        return initGradleConnection('.');
    }

    static ProjectConnection initGradleConnection(String path) {
        ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(new File(path))
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

    static File getServerXMLFile(def server) {
        if (server.serverXmlFile != null && server.serverXmlFile.exists()) {
            return server.serverXmlFile;
        }

        File configDirServerXML = new File(server.configDirectory, "server.xml")
        if (configDirServerXML.exists()) {
            return configDirServerXML;
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
