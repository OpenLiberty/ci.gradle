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


import net.wasdev.wlp.ant.ServerTask

import net.wasdev.wlp.gradle.plugins.tasks.StartTask

import net.wasdev.wlp.common.plugins.util.DevUtil
import net.wasdev.wlp.common.plugins.util.PluginExecutionException
import net.wasdev.wlp.common.plugins.util.PluginScenarioException

class DevTask extends AbstractServerTask {

    DevTask() {
        configure({
            description "Runs a Liberty dev server"
            logging.level = LogLevel.INFO
            group 'Liberty'
        })
    }

    /**
     * Hot tests
     */
    @Input
    private boolean hotTests = false;

    @Option(option = 'hotTests', description = 'TODO')
    void setHotTests(boolean hotTests) {
        this.hotTests = hotTests;
    }

    /**
     * Skip tests
     */
    @Input
    private boolean skipTests = false;

    @Option(option = 'skipTests', description = 'Skip tests.')
    void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }

    /**
     * Skip unit tests
     */
    @Input
    private boolean skipUTs = false;

    @Option(option = 'skipUTs', description = 'Skip unit tests.')
    void setSkipUTs(boolean skipUTs) {
        this.skipUTs = skipUTs;
    }

    /**
     * Skip integration tests
     */
    @Input
    private boolean skipITs = false;

    @Option(option = 'skipITs', description = 'Skip integration tests.')
    void setSkipITs(boolean skipITs) {
        this.skipITs = skipITs;
    }

    /**
     * Debug
     */
    @Input
    private boolean libertyDebug = true;

    @Option(option = 'debug', description = 'TODO')
    void setLibertyDebug(boolean libertyDebug) {
        this.libertyDebug = libertyDebug;
    }

    /**
     * Debug Port
     */
    @Input
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
    @Input
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
    @Input
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
    @Input
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
    @Input
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
    @Input
    private String applications;

    @Option(option = 'applications', description = 'Comma separated list of app names to wait for')
    void setApplications(String applications) {
        this.applications = applications;
    }

    /**
     * Clean all cached information on server start up.
     */
    @Input
    private boolean clean = false;

    @Option(option = 'clean', description = 'Clean all cached information on server start up.')
    void setClean(boolean clean) {
        this.clean = clean;
    }

    /**
     * The directory for source files.
     */
    @Input
    private String sourceDirectoryString

    @Input
    private File sourceDirectory

    @Option(option = 'sourceDirectory', description = 'The directory for source files.')
    void setSourceDirectory(String sourceDirectoryString) {
        this.sourceDirectoryString = sourceDirectoryString;
        this.sourceDirectory = new File(sourceDirectoryString);
    }

    /**
     * The directory for test source files.
     */
    @Input
    private String testSourceDirectoryString
    
    @Input
    private File testSourceDirectory

    @Option(option = 'testSourceDirectory', description = 'The directory for test source files.')
    void setSestSourceDirectory(String testSourceDirectoryString) {
        this.testSourceDirectoryString = testSourceDirectoryString;
        this.testSourceDirectory = new File(testSourceDirectoryString);
    }

    
    /**
     * The directory for compiled classes.
     */
    @Input
    private File outputDirectory;

    @Option(option = 'outputDirectory', description = 'The directory for test source files.')
    void setOutputDirectory(String outputDirectoryString) {
        this.outputDirectory = new File(outputDirectoryString);
    }

    /**
     * The directory for compiled test classes.
     */
    @Input
    private File testOutputDirectory;

    @Option(option = 'testOutputDirectory', description = 'The directory for compiled test classes.')
    void setTestOutputDirectory(String testOutputDirectoryString) {
        this.testOutputDirectory = new File(testOutputDirectoryString);
    }

    
    private class DevTaskUtil extends DevUtil {
        public DevTaskUtil(
            File serverDirectory, File sourceDirectory, File testSourceDirectory, File configDirectory,
                List<File> resourceDirs
                ) throws IOException {
            // super(serverDirectory, sourceDirectory, testSourceDirectory, configDirectory, resourceDirs, hotTests,
            //         skipTests, skipUTs, skipITs, project.getArtifactId(), verifyTimeout, appUpdateTimeout,
            //         ((long) (compileWait * 1000L)), libertyDebug);

            // ServerFeature servUtil = getServerFeatureUtil();
            // this.existingFeatures = servUtil.getServerFeatures(serverDirectory);
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
            // try {
            //     ServerTask serverTask = initializeJava();
            //     serverTask.setOperation("stop");
            //     serverTask.execute();
            // } catch (Exception e) {
            //     // ignore
            //     log.debug("Error stopping server", e);
            // }
        }

        @Override
        public ServerTask getDebugServerTask() throws Exception {
        }

        @Override
        public List<String> getArtifacts() {
            // List<String> artifactPaths = new ArrayList<String>();
            // Set<Artifact> artifacts = project.getArtifacts();
            // for (Artifact artifact : artifacts) {
            //     try {
            //         artifactPaths.add(artifact.getFile().getCanonicalPath());
            //     } catch (IOException e) {
            //         log.error("Unable to resolve project artifact " + e.getMessage());
            //     }
            // }
            // return artifactPaths;
        }

        @Override
        public boolean recompileBuildFile(File buildFile, List<String> artifactPaths, ThreadPoolExecutor executor) {
        }

        @Override
        public void checkConfigFile(File configFile, File serverDir) {
            
        }

        @Override
        public boolean compile(File dir) {
        }

        @Override
        public void runUnitTests() throws PluginExecutionException, PluginScenarioException {
            // try {
            //     runTestMojo("org.apache.maven.plugins", "maven-surefire-plugin", "test");
            //     runTestMojo("org.apache.maven.plugins", "maven-surefire-report-plugin", "report-only");
            // } catch (MojoExecutionException e) {
            //     Throwable cause = e.getCause();
            //     if (cause != null && cause instanceof MojoFailureException) {
            //         throw new PluginScenarioException("Unit tests failed: " + cause.getLocalizedMessage(), e);
            //     } else {
            //         throw new PluginExecutionException("Failed to run unit tests", e);
            //     }
            // }
        }

        @Override
        public void runIntegrationTests() throws PluginExecutionException, PluginScenarioException {
            // try {
            //     runTestMojo("org.apache.maven.plugins", "maven-failsafe-plugin", "integration-test");
            //     runTestMojo("org.apache.maven.plugins", "maven-surefire-report-plugin", "failsafe-report-only");
            //     runTestMojo("org.apache.maven.plugins", "maven-failsafe-plugin", "verify");
            // } catch (MojoExecutionException e) {
            //     Throwable cause = e.getCause();
            //     if (cause != null && cause instanceof MojoFailureException) {
            //         throw new PluginScenarioException("Integration tests failed: " + cause.getLocalizedMessage(), e);
            //     } else {
            //         throw new PluginExecutionException("Failed to run integration tests", e);
            //     }
            // }
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

            // runGradleTask(gradleBuildLauncher, 'compileJava');
            // runGradleTask(gradleBuildLauncher, 'processResources');
            // runGradleTask(gradleBuildLauncher, 'compileTestJava');
            // runGradleTask(gradleBuildLauncher, 'processTestResources');

            // SourceSet mainSourceSet = project.sourceSets.main;
            // SourceSet testSourceSet = project.sourceSets.test;
            // println 'srcDirs';
            // println mainSourceSet.java.srcDirs;
            // println testSourceSet.java.srcDirs;

            // println 'outputDir'
            // println mainSourceSet.java.outputDir;
            // println testSourceSet.java.outputDir;

            println this.hotTests
            println this.skipTests
            println this.skipUTs
            println this.skipITs
            println this.libertyDebug
            println this.libertyDebugPort
            println this.compileWait
            println this.verifyTimeout
            println this.appUpdateTimeout
            println this.serverStartTimeout
            println this.applications
            println this.clean
            println this.sourceDirectoryString
            println this.testSourceDirectoryString

            // runGradleTask(gradleBuildLauncher, 'libertyStart');
            // println 'SLEEPING';
            // sleep(15 * 1000); 
            // runGradleTask(gradleBuildLauncher, 'libertyStop');
            // runGradleTask(gradleBuildLauncher, 'libertyStart');


        } finally {
            connection.close();
        }

    }
}
