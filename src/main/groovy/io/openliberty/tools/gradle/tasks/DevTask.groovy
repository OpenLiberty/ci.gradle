/**
 * (C) Copyright IBM Corporation 2019, 2021.
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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.SourceSet
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.options.Option
import org.gradle.testfixtures.ProjectBuilder
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
import io.openliberty.tools.common.plugins.util.ProjectModule

import java.util.concurrent.TimeUnit
import java.util.Map.Entry
import java.nio.file.Path;

class DevTask extends AbstractFeatureTask {

    private static final String LIBERTY_HOSTNAME = "liberty.hostname";
    private static final String LIBERTY_HTTP_PORT = "liberty.http.port";
    private static final String LIBERTY_HTTPS_PORT = "liberty.https.port";
    private static final String MICROSHED_HOSTNAME = "microshed_hostname";
    private static final String MICROSHED_HTTP_PORT = "microshed_http_port";
    private static final String MICROSHED_HTTPS_PORT = "microshed_https_port";
    private static final String WLP_USER_DIR_PROPERTY_NAME = "wlp.user.dir";

    DevTask() {
        configure({
            description 'Runs a Liberty server in dev mode'
            group 'Liberty'
        })
    }

    @Optional
    @Input
    DevTaskUtil util = null;

    // Default DevMode argument values
    // DevMode uses CLI Arguments if provided, otherwise it uses ServerExtension properties if one exists, fallback to default value if neither are provided.
    private static final int DEFAULT_VERIFY_TIMEOUT = 30;
    private static final int DEFAULT_SERVER_TIMEOUT = 90;
    private static final double DEFAULT_COMPILE_WAIT = 0.5;
    private static final int DEFAULT_DEBUG_PORT = 7777;
    private static final boolean DEFAULT_HOT_TESTS = false;
    private static final boolean  DEFAULT_SKIP_TESTS = false;
    private static final boolean DEFAULT_LIBERTY_DEBUG = true;
    private static final boolean DEFAULT_POLLING_TEST = false;
    private static final boolean DEFAULT_CONTAINER = false;
    private static final boolean DEFAULT_SKIP_DEFAULT_PORTS = false;
    private static final boolean DEFAULT_KEEP_TEMP_DOCKERFILE = false;
    private static final boolean DEFAULT_GENERATE_FEATURES = true;

    protected final String CONTAINER_PROPERTY_ARG = '-P'+CONTAINER_PROPERTY+'=true';

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

    @Optional
    @Input
    Boolean libertyDebug;

    // Need to use a string value to allow someone to specify --libertyDebug=false
    // bool @Options only allow you to specify "--libertyDebug" or nothing.
    // So there is no way to explicitly set libertyDebug to false if we want the default behavior to be true
    @Option(option = 'libertyDebug', description = 'Whether to allow attaching a debugger to the running server. The default value is true.')
    void setLibertyDebug(String libertyDebug) {
        this.libertyDebug = Boolean.parseBoolean(libertyDebug)
    }

    @Optional
    @Input
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

    @Option(option = 'serverStartTimeout', description = 'Time in seconds to wait while verifying that the server has started. The default value is 90 seconds.')
    void setServerStartTimeout(String serverStartTimeout) {
        try {
            this.serverStartTimeout = serverStartTimeout.toInteger();
        } catch (NumberFormatException e) {
            logger.error(String.format("Unexpected value: %s for dev mode option serverStartTimeout. serverStartTimeout should be a valid integer.", serverStartTimeout));
            throw e;
        }
    }

    private Boolean pollingTest;

    @Option(option = 'pollingTest', description = 'This option is only for testing dev mode using polling to track file changes instead of using file system notifications. The default value is false, in which case dev mode will rely on file system notifications but will automatically fall back to polling if file system notifications are not available.')
    void setPollingTest(boolean pollingTest) {
        this.pollingTest = pollingTest;
    }

    @Optional
    @Input
    private Boolean container = null;

    @Option(option = 'container', description = 'Run the server in a Docker container instead of locally. The default value is false for the libertyDev task, and true for the libertyDevc task.')
    void setContainer(boolean container) {
        this.container = container;
        project.liberty.dev.container = container; // Needed in DeployTask and AbstractServerTask
    }

    Boolean getContainer() {
        return container;
    }

    private File dockerfile;

    @Option(option = 'dockerfile', description = 'Dev mode will build a docker image from the provided Dockerfile and start a container from the new image.')
    void setDockerfile(String dockerfile) {
        if (dockerfile != null) {
            // ensures the dockerfile is defined with the full path - matches how maven behaves
            this.dockerfile = convertParameterToCanonicalFile(dockerfile, "dockerfile");      
        }
    }

    private File dockerBuildContext;

    @Option(option = 'dockerBuildContext', description = 'The Docker build context used when building the container in dev mode. Defaults to the directory of the Dockerfile if not specified.')
    void setDockerBuildContext(String dockerBuildContext) {
        if (dockerBuildContext != null) {
            // ensures the dockerBuildContext is defined with the full path - matches how maven behaves
            this.dockerBuildContext = convertParameterToCanonicalFile(dockerBuildContext, "dockerBuildContext");      
        }
    }

    private convertParameterToCanonicalFile(String relativeOrAbsolutePath, String parameterName) {
        File result = null;
        if (relativeOrAbsolutePath != null) {
            File file = new File(relativeOrAbsolutePath);
            try {
                if (file.isAbsolute()) {
                    result = file.getCanonicalFile();
                } else {
                    result = new File(project.getRootDir(), relativeOrAbsolutePath).getCanonicalFile(); 
                }
            } catch (IOException e) {
                throw new PluginExecutionException("Could not resolve canonical path of the " + parameterName + " parameter: " + parameterName, e);
            }
        }
        return result;
    }

    private String dockerRunOpts;

    @Option(option = 'dockerRunOpts', description = 'Additional options for the docker run command when dev mode starts a container.')
    void setDockerRunOpts(String dockerRunOpts) {
        this.dockerRunOpts = dockerRunOpts;
    }

    private int dockerBuildTimeout;

    @Option(option = 'dockerBuildTimeout', description = 'Specifies the amount of time to wait (in seconds) for the completion of the Docker operation to build the image.')
    void setDockerBuildTimeout(String inputValue) {
        try {
            this.dockerBuildTimeout = inputValue.toInteger();
        } catch (NumberFormatException e) {
            logger.error(String.format("Unexpected value: %s for dev mode option dockerBuildTimeout. dockerBuildTimeout should be a valid integer.", inputValue));
            throw e;
        }
    }

    private Boolean skipDefaultPorts;

    @Option(option = 'skipDefaultPorts', description = 'If true, the default Docker port mappings are skipped in the docker run command.')
    void setSkipDefaultPorts(boolean skipDefaultPorts) {
        this.skipDefaultPorts = skipDefaultPorts;
    }

    private Boolean keepTempDockerfile;

    @Option(option = 'keepTempDockerfile', description = 'If true, preserve the temporary Dockerfile used to build the container.')
    void setKeepTempDockerfile(boolean keepTempDockerfile) {
        this.keepTempDockerfile = keepTempDockerfile;
    }

    @Optional
    @Input
    Boolean generateFeatures;

    // Need to use a string value to allow someone to specify --generateFeatures=false, if not explicitly set defaults to true
    @Option(option = 'generateFeatures', description = 'If true, scan the application binary files to determine which Liberty features should be used. The default value is true.')
    void setGenerateFeatures(String generateFeatures) {
        this.generateFeatures = Boolean.parseBoolean(generateFeatures);
    }

    @Optional
    @Input
    Boolean clean;

    @Option(option = 'clean', description = 'Clean all cached information on server start up. The default value is false.')
    void setClean(boolean clean) {
        this.clean = clean;
    }

    @Optional
    @InputDirectory
    File sourceDirectory;

    @Optional
    @InputDirectory
    File testSourceDirectory;

    private class DevTaskUtil extends DevUtil {

        Set<String> existingFeatures;

        Set<String> existingLibertyFeatureDependencies;

        Map<String, File> libertyDirPropertyFiles = new HashMap<String, File> ();

        private ServerTask serverTask = null;

        DevTaskUtil(File buildDir, File installDirectory, File userDirectory, File serverDirectory, File sourceDirectory, File testSourceDirectory,
                    File configDirectory, File projectDirectory, List<File> resourceDirs,
                    boolean  hotTests, boolean  skipTests, String artifactId, int serverStartTimeout,
                    int verifyAppStartTimeout, int appUpdateTimeout, double compileWait,
                    boolean libertyDebug, boolean pollingTest, boolean container, File dockerfile, File dockerBuildContext,
                    String dockerRunOpts, int dockerBuildTimeout, boolean skipDefaultPorts, boolean keepTempDockerfile, 
                    String mavenCacheLocation, String packagingType, File buildFile, boolean generateFeatures
        ) throws IOException {
            super(buildDir, serverDirectory, sourceDirectory, testSourceDirectory, configDirectory, projectDirectory, /* multi module project directory */ projectDirectory,
                    resourceDirs, hotTests, skipTests, false /* skipUTs */, false /* skipITs */, artifactId,  serverStartTimeout,
                    verifyAppStartTimeout, appUpdateTimeout, ((long) (compileWait * 1000L)), libertyDebug,
                    true /* useBuildRecompile */, true /* gradle */, pollingTest, container, dockerfile, dockerBuildContext, dockerRunOpts, dockerBuildTimeout, skipDefaultPorts,
                    null /* compileOptions not needed since useBuildRecompile is true */, keepTempDockerfile, mavenCacheLocation, null /* multi module upstream projects */,
                    false /* recompileDependencies only supported in ci.maven */, packagingType, buildFile, null /* parent build files */, generateFeatures, null /* compileArtifactPaths */, null /* testArtifactPaths */, new ArrayList<Path>() /* webResources */
                );

            ServerFeatureUtil servUtil = getServerFeatureUtil();
            this.libertyDirPropertyFiles = AbstractServerTask.getLibertyDirectoryPropertyFiles(installDirectory, userDirectory, serverDirectory);
            this.existingFeatures = servUtil.getServerFeatures(serverDirectory, libertyDirPropertyFiles);

            this.existingLibertyFeatureDependencies = new HashSet<String>();

            project.configurations.getByName('libertyFeature').dependencies.each {
                dep -> this.existingLibertyFeatureDependencies.add(dep.name)
            }
        }

        @Override
        public void debug(String msg) {
            logger.debug(msg);
        }

        @Override
        public void debug(String msg, Throwable e) {
            logger.debug(msg, (Throwable) e)
        }

        @Override
        public void debug(Throwable e) {
            logger.debug("Throwable exception received: "+e.getMessage(), (Throwable) e)
        }

        @Override
        public void warn(String msg) {
            logger.warn(msg);
        }

        @Override
        public void info(String msg) {
            logger.lifecycle(msg);
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
        public String getServerStartTimeoutExample() {
            return "'gradle libertyDev --serverStartTimeout=120'";
        }

        @Override
        public String getProjectName() {
            return project.getName();
        }

        @Override
        public void stopServer() {
            super.serverFullyStarted.set(false);

            if (container) {
                // Shouldn't get here, DevUtil should stop the container instead
                logger.debug('DevUtil called stopServer when the server should be running in a container.')
                return;
            }
            if (isLibertyInstalledAndValid(project)) {
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
        public boolean updateArtifactPaths(ProjectModule projectModule, boolean redeployCheck, ThreadPoolExecutor executor)
                throws PluginExecutionException {
            // not supported for Gradle, only used for multi module Maven projects
            return false;
        }

        @Override
        public boolean updateArtifactPaths(File parentBuildFile) {
            // not supported for Gradle, only used for multi module Maven projects
            return false;
        }
        
        @Override
        protected void updateLooseApp() throws PluginExecutionException {
        	// not supported for Gradle, only used for exploded war Maven projects
        }
        
        @Override
        protected void resourceDirectoryCreated() throws IOException {
            // Nothing to do
        }

        @Override
        protected void resourceModifiedOrCreated(File fileChanged, File resourceParent, File outputDirectory) throws IOException {
            copyFile(fileChanged, resourceParent, outputDirectory, null);
        }

        @Override
        protected void resourceDeleted(File fileChanged, File resourceParent, File outputDirectory) throws IOException {
            deleteFile(fileChanged, resourceParent, outputDirectory, null);
        }

        @Override
        public boolean recompileBuildFile(File buildFile, Set<String> compileArtifactPaths, Set<String> testArtifactPaths, ThreadPoolExecutor executor) {
            boolean restartServer = false;
            boolean installFeatures = false;

            ProjectBuilder builder = ProjectBuilder.builder();
            Project newProject;
            try {
                newProject = builder
                        .withProjectDir(project.rootDir)
                        .withGradleUserHomeDir(project.gradle.gradleUserHomeDir)
                        .withName(project.name)
                        .build();

                // need this for gradle to evaluate the project
                // and load the different plugins and extensions
                newProject.evaluate();
            } catch (Exception e) {
                logger.error("Could not parse build.gradle " + e.getMessage());
                logger.debug('Error parsing build.gradle', e);
                return false;
            }

            if(hasServerConfigBootstrapPropertiesChanged(newProject, project)) {
                logger.debug('Bootstrap properties changed');
                restartServer = true;
                project.liberty.server.bootstrapProperties = newProject.liberty.server.bootstrapProperties;
            }

            if (hasServerConfigBootstrapPropertiesFileChanged(newProject, project)) {
                logger.debug('Bootstrap properties file changed');
                restartServer = true;
                project.liberty.server.bootstrapPropertiesFile = newProject.liberty.server.bootstrapPropertiesFile;
            }

            if (hasServerConfigJVMOptionsChanged(newProject, project)) {
                logger.debug('JVM Options changed');
                restartServer = true;
                project.liberty.server.jvmOptions = newProject.liberty.server.jvmOptions;
            }

            if (hasServerConfigJVMOptionsFileChanged(newProject, project)) {
                logger.debug('JVM Options file changed');
                restartServer = true;
                project.liberty.server.jvmOptionsFile = newProject.liberty.server.jvmOptionsFile;
            }

            if (hasServerConfigEnvFileChanged(newProject, project)) {
                logger.debug('Server Env file changed');
                restartServer = true;
                project.liberty.server.serverEnvFile = newProject.liberty.server.serverEnvFile;
            }

            if (hasServerConfigDirectoryChanged(newProject, project)) {
                logger.debug('Server config directory changed');
                restartServer = true;
                project.liberty.server.configDirectory = newProject.liberty.server.configDirectory;
                initializeConfigDirectory(); // make sure that the config dir is set if it was null in the new project
            }

            if (hasServerConfigEnvChanged(newProject, project)) {
                logger.debug('Server env changed');
                restartServer = true;
                project.liberty.server.env = newProject.liberty.server.env;
            }

            if (hasServerConfigVarChanged(newProject, project)) {
                logger.debug('Server var changed');
                restartServer = true;
                project.liberty.server.var = newProject.liberty.server.var;
            }

            if (hasServerConfigDefaultVarChanged(newProject, project)) {
                logger.debug('Server default var changed');
                restartServer = true;
                project.liberty.server.defaultVar = newProject.liberty.server.defaultVar;
            }

            if (hasCopyLibsDirectoryChanged(newProject, project)) {
                logger.debug('copyLibsDirectory changed');
                restartServer = true;
                project.liberty.server.deploy.copyLibsDirectory = newProject.liberty.server.deploy.copyLibsDirectory;
            }

            if (hasMergeServerEnvChanged(newProject, project)) {
                logger.debug('mergeServerEnv changed');
                restartServer = true;
                project.liberty.server.mergeServerEnv = newProject.liberty.server.mergeServerEnv;
            }

            // if we don't already need to restart the server
            // check if we need to install any additional features
            if (!restartServer) {
                List<String> oldFeatureNames = project.liberty.server.features.name;
                List<String> newFeatureNames = newProject.liberty.server.features.name;

                if (oldFeatureNames != newFeatureNames) {
                    logger.debug('Server feature changed');
                    installFeatures = true;
                    project.liberty.server.features.name = newFeatureNames;
                }

                Configuration newLibertyFeatureConfiguration = newProject.configurations.getByName('libertyFeature');
                List<String> newLibertyFeatureDependencies = new ArrayList<String>();
                newLibertyFeatureConfiguration.dependencies.each { dep -> newLibertyFeatureDependencies.add(dep.name) }

                newLibertyFeatureDependencies.removeAll(existingLibertyFeatureDependencies);

                if (!newLibertyFeatureDependencies.isEmpty()) {
                    logger.debug('libertyFeature dependency changed');
                    installFeatures = true;
                    existingLibertyFeatureDependencies.addAll(newLibertyFeatureDependencies);
                }

            }
            if (restartServer) {
                // - stop Server
                // - generate features (if generateFeatures=true)
                // - create server or runBoostMojo
                // - install feature
                // - deploy app
                // - start server
                util.restartServer();
                return true;
            } else if (installFeatures) {
                if (generateFeatures) {
                    // Increment generate features on build dependency change
                    ProjectConnection gradleConnection = initGradleProjectConnection();
                    BuildLauncher gradleBuildLauncher = gradleConnection.newBuild();
                    runGradleTask(gradleBuildLauncher, 'compileJava', 'processResources'); // ensure class files exist
                    Collection<String> javaSourceClassPaths = getJavaSourceClassPaths();
                    libertyGenerateFeatures(javaSourceClassPaths, false);
                    libertyCreate(); // need to run create in order to copy generated config file to target
                }
                libertyInstallFeature();
            }

            return true;
        }

        private boolean hasServerConfigBootstrapPropertiesChanged(Project newProject, Project oldProject) {
            return newProject.liberty.server.bootstrapProperties != oldProject.liberty.server.bootstrapProperties;
        }

        private boolean hasServerConfigBootstrapPropertiesFileChanged(Project newProject, Project oldProject) {
            return newProject.liberty.server.bootstrapPropertiesFile != oldProject.liberty.server.bootstrapPropertiesFile;
        }

        private boolean hasServerConfigJVMOptionsChanged(Project newProject, Project oldProject) {
            return newProject.liberty.server.jvmOptions != oldProject.liberty.server.jvmOptions;
        }

        private boolean hasServerConfigJVMOptionsFileChanged(Project newProject, Project oldProject) {
            return newProject.liberty.server.jvmOptionsFile != oldProject.liberty.server.jvmOptionsFile;
        }

        private boolean hasServerConfigEnvFileChanged(Project newProject, Project oldProject) {
            return newProject.liberty.server.serverEnvFile != oldProject.liberty.server.serverEnvFile;
        }

        private boolean hasServerConfigDirectoryChanged(Project newProject, Project oldProject) {
            File newServerConfigDir = newProject.liberty.server.configDirectory;
            File oldServerConfigDir = oldProject.liberty.server.configDirectory;

            // Since no tasks have been run on the new project the initializeConfigDirectory()
            // method has not been ran yet, so the file may still be null. But for the old project
            // this method is guaranteed to have ran at the start of DevMode. So we need to initialize
            // the config directory on the new project or else it would report that the directory has changed
            if (newServerConfigDir == null) {
                newServerConfigDir = new File(newProject.projectDir, "src/main/liberty/config");
            }

            return newServerConfigDir != oldServerConfigDir;
        }

        private boolean hasServerConfigEnvChanged(Project newProject, Project oldProject) {
            return newProject.liberty.server.env != oldProject.liberty.server.env;
        }

        private boolean hasServerConfigVarChanged(Project newProject, Project oldProject) {
            return newProject.liberty.server.var != oldProject.liberty.server.var;
        }

        private boolean hasServerConfigDefaultVarChanged(Project newProject, Project oldProject) {
            return newProject.liberty.server.defaultVar != oldProject.liberty.server.defaultVar;
        }

        private boolean hasMergeServerEnvChanged(Project newProject, Project oldProject) {
            return newProject.liberty.server.mergeServerEnv != oldProject.liberty.server.mergeServerEnv;
        }

        private boolean hasCopyLibsDirectoryChanged(Project newProject, Project oldProject) {
            File newCopyLibsDir = newProject.liberty.server.deploy.copyLibsDirectory;
            File oldCopyLibsDirDir = oldProject.liberty.server.deploy.copyLibsDirectory;

            return newCopyLibsDir != oldCopyLibsDirDir;
        }


        @Override
        public void checkConfigFile(File configFile, File serverDir) {
            ServerFeatureUtil servUtil = getServerFeatureUtil();
            Set<String> features = servUtil.getServerFeatures(serverDir, libertyDirPropertyFiles);

            if (features == null) {
                return;
            }

            features.removeAll(existingFeatures);

            if (!features.isEmpty()) {
                logger.info("Configuration features have been added");

                // Call the installFeature gradle task using the temporary serverDir directory that DevMode uses
                ProjectConnection gradleConnection = initGradleProjectConnection();
                BuildLauncher gradleBuildLauncher = gradleConnection.newBuild();

                // Exclude libertyCreate from the task dependencies, so that it will not update the server features
                // before the features are installed.
                gradleBuildLauncher.withArguments("--exclude-task", "libertyCreate");

                try {
                    List<String> options = new ArrayList<String>();
                    options.add("--serverDir=${serverDir.getAbsolutePath()}");
                    if (container) {
                        options.add("--containerName=${super.getContainerName()}");
                    }
                    runInstallFeatureTask(gradleBuildLauncher, options);
                    this.existingFeatures.addAll(features);
                } catch (BuildException e) {
                    // stdout/stderr from the installFeature task is sent to the terminal
                    // only need to log the actual stacktrace when debugging
                    logger.debug('Failed to install features from configuration file', e);
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
                // stdout/stderr from the compile task is sent to the terminal
                // only need to log the actual stacktrace when debugging
                logger.debug('Unable to compile', e);
                return false;
            } finally {
                gradleConnection.close();
            }
        }

        @Override
        public boolean compile(File dir, ProjectModule project) {
            // used for multi module scenario, not yet supported in ci.gradle
            return false;
        }

        @Override
        public void runUnitTests(File buildFile) throws PluginExecutionException, PluginScenarioException {
            // Not needed for gradle.
        }

        @Override
        public void runIntegrationTests(File buildFile) throws PluginExecutionException, PluginScenarioException {
            // buildFile parameter is not used, implemented for multi module projects, which is not supported in Gradle

            ProjectConnection gradleConnection = initGradleProjectConnection();
            BuildLauncher gradleBuildLauncher = gradleConnection.newBuild();

            ArrayList<String> systemPropertyArgs = new ArrayList<String>();

            if (util.getHostName() != null) {
                systemPropertyArgs.push("-D" + LIBERTY_HOSTNAME + "=" + util.getHostName());
                systemPropertyArgs.push("-D" + MICROSHED_HOSTNAME + "=" + util.getHostName());
            }

            if (util.getHttpPort() != null) {
                systemPropertyArgs.push("-D" + LIBERTY_HTTP_PORT + "=" + util.getHttpPort());
                systemPropertyArgs.push("-D" + MICROSHED_HTTP_PORT + "=" + util.getHttpPort());
            }

            if (util.getHttpsPort() != null) {
                systemPropertyArgs.push("-D" + LIBERTY_HTTPS_PORT + "=" + util.getHttpsPort());
                systemPropertyArgs.push("-D" + MICROSHED_HTTPS_PORT + "=" + util.getHttpsPort());
            }

            try {
                systemPropertyArgs.push("-D" + WLP_USER_DIR_PROPERTY_NAME + "=" + getUserDir(project).getCanonicalPath());
            } catch (IOException e) {
                throw new PluginExecutionException("Could not resolve canonical path of the user directory: " + getUserDir(project).getAbsolutePath(), e);
            }

            try {
                gradleBuildLauncher.withArguments(systemPropertyArgs);
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
            ProjectConnection gradleConnection = initGradleProjectConnection();
            BuildLauncher gradleBuildLauncher = gradleConnection.newBuild();

            try {
                if (container) {
                    gradleBuildLauncher.addArguments(CONTAINER_PROPERTY_ARG);
                }
                runGradleTask(gradleBuildLauncher, 'deploy');
            } catch (BuildException e) {
                throw new PluginExecutionException(e);
            } finally {
                gradleConnection.close();
            }
        }

        @Override
        public boolean libertyGenerateFeatures(Collection<String> classes, boolean optimize) {
            ProjectConnection gradleConnection = initGradleProjectConnection();
            BuildLauncher gradleBuildLauncher = gradleConnection.newBuild();

            try {
                List<String> options = new ArrayList<String>();
                classes.each {
                    // generate features for only the classFiles passed (if any)
                    options.add("--classFile=" + it);
                }
                options.add("--optimize=" + optimize);
                runGenerateFeaturesTask(gradleBuildLauncher, options);
                return true; // successfully generated features
            } catch (BuildException e) {
                // log errors instead of throwing an exception so we do not flood console with stacktrace
                Exception pluginEx = getPluginExecutionException(e);
                if (pluginEx != null) {
                    // PluginExecutionException indicates that the binary scanner jar could not be found
                    logger.error(pluginEx.getMessage() + ".\nDisabling the automatic generation of features.");
                    setFeatureGeneration(false);
                } else {
                    logger.error(e.getMessage() + ".\nTo disable the automatic generation of features, type 'g' and press Enter.");
                }
                return false;
            } finally {
                gradleConnection.close();
            }
        }

        @Override
        public void libertyInstallFeature() {
            ProjectConnection gradleConnection = initGradleProjectConnection();
            BuildLauncher gradleBuildLauncher = gradleConnection.newBuild();

            try {
                List<String> options = new ArrayList<String>();
                if (container) {
                    options.add("--containerName=${super.getContainerName()}");
                }
                runInstallFeatureTask(gradleBuildLauncher, options);
            } catch (BuildException e) {
                throw new PluginExecutionException(e);
            } finally {
                gradleConnection.close();
            }
        }

        @Override
        public void libertyDeploy() {
            ProjectConnection gradleConnection = initGradleProjectConnection();
            BuildLauncher gradleBuildLauncher = gradleConnection.newBuild();
            try {
                if (container) {
                    gradleBuildLauncher.addArguments(CONTAINER_PROPERTY_ARG)
                    // Skip installFeature since it is not needed here in container mode.
                    // Container mode should call installFeature separately with the containerName parameter where needed.
                    gradleBuildLauncher.addArguments("--exclude-task", "installFeature");
                }
                runGradleTask(gradleBuildLauncher, 'deploy');
            } catch (BuildException e) {
                throw new PluginExecutionException(e);
            } finally {
                gradleConnection.close();
            }
        }

        @Override
        public void libertyCreate() {
            if (container) {
                createServerDirectories();
            } else {
                // need to force liberty-create to re-run
                // else it will just say up-to-date and skip the task
                ProjectConnection gradleConnection = initGradleProjectConnection();
                BuildLauncher gradleBuildLauncher = gradleConnection.newBuild();

                gradleBuildLauncher.addArguments('--rerun-tasks');
                addLibertyRuntimeProperties(gradleBuildLauncher);
                try {
                    runGradleTask(gradleBuildLauncher, 'libertyCreate');
                } catch (BuildException e) {
                    throw new PluginExecutionException(e);
                } finally {
                    gradleConnection.close();
                }
            }
        }

        @Override
        public boolean isLooseApplication() {
            return server.looseApplication && DeployTask.isSupportedLooseAppType(getPackagingType());
        }

        @Override
        public File getLooseApplicationFile() {
            configureApps(project)
            String appsDir
            if (server.deploy.apps != null && !server.deploy.apps.isEmpty()) {
                appsDir = 'apps'
            } else if (server.deploy.dropins != null && !server.deploy.dropins.isEmpty()) {
                appsDir = 'dropins'
            }
            return getLooseAppConfigFile(container, appsDir);
        }
    }

    public void runInstallFeatureTask(BuildLauncher gradleBuildLauncher, List<String> options) throws BuildException {
        String[] tasks = new String[options != null ? options.size() + 1 : 1];
        tasks[0] = 'installFeature';
        if (options != null) {
            for(int i = 0; i < options.size(); i++) {
                tasks[i+1] = options.get(i);
            }
        }

        runGradleTask(gradleBuildLauncher, tasks);
    }

    public void runGenerateFeaturesTask(BuildLauncher gradleBuildLauncher, boolean optimize) throws BuildException {
        List<String> options = new ArrayList<String>();
        options.add("--optimize="+optimize);
        runGenerateFeaturesTask(gradleBuildLauncher, options);
    }

    public void runGenerateFeaturesTask(BuildLauncher gradleBuildLauncher, List<String> options) throws BuildException {
        String[] tasks = new String[options != null ? options.size() + 1 : 1];
        tasks[0] = 'generateFeatures';
        if (options != null) {
            for(int i = 0; i < options.size(); i++) {
                tasks[i+1] = options.get(i);
            }
        }

        runGradleTask(gradleBuildLauncher, tasks);
    }

    // If a argument has not been set using CLI arguments set a default value
    // Using the ServerExtension properties if available, otherwise use hardcoded defaults
    private void initializeDefaultValues() throws Exception {
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

        if (pollingTest == null) {
            pollingTest = DEFAULT_POLLING_TEST;
        }

        if (generateFeatures == null) {
            generateFeatures = DEFAULT_GENERATE_FEATURES;
        }

        processContainerParams();
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

        if (!container) {
            if (serverDirectory.exists()) {
                if (ServerStatusUtil.isServerRunning(serverInstallDir, serverOutputDir, serverName)) {
                    throw new Exception("The server " + serverName
                            + " is already running. Terminate all instances of the server before starting dev mode."
                            + " You can stop a server instance with the command 'gradle libertyStop'.");
                }
            }
        } // else TODO check if the container is already running?

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
            if (generateFeatures) {
                // Optimize generate features on startup
                runGradleTask(gradleBuildLauncher, 'compileJava', 'processResources'); // ensure class files exist
                try {
                    runGenerateFeaturesTask(gradleBuildLauncher, true);
                } catch (BuildException e) {
                    Exception pluginEx = getPluginExecutionException(e);
                    if (pluginEx != null) {
                        // PluginExecutionException indicates that the binary scanner jar could not be found
                        logger.error(pluginEx.getMessage() + ".\nDisabling the automatic generation of features.");
                        generateFeatures = false;
                    } else if (e.getCause() != null) {
                        throw new BuildException(e.getCause().getMessage() + " To disable the automatic generation of features, start dev mode with --generateFeatures=false.", e.getCause());
                    } else {
                        throw new BuildException("Failed to run the generateFeaturesTask. To disable the automatic generation of features, start dev mode with --generateFeatures=false.", e)
                    }
                }
            }
            if (!container) {
                addLibertyRuntimeProperties(gradleBuildLauncher);
                runGradleTask(gradleBuildLauncher, 'libertyCreate');
                // suppress extra install feature warnings (one would have shown up already from the libertyCreate task on the line above)
                gradleBuildLauncher.addArguments("-D" + DevUtil.SKIP_BETA_INSTALL_WARNING + "=" + Boolean.TRUE.toString());
                runInstallFeatureTask(gradleBuildLauncher, null);
            } else {
                // skip creating the server and installing features and just propagate the option to 'deploy'
                createServerDirectories();
                gradleBuildLauncher.addArguments("--exclude-task", "installFeature"); // skip installing features at startup since Dockerfile should have RUN features.sh
                gradleBuildLauncher.addArguments(CONTAINER_PROPERTY_ARG);
            }
            runGradleTask(gradleBuildLauncher, 'deploy');
        } finally {
            gradleConnection.close();
        }

        String localMavenRepoForFeatureUtility = new File(new File(System.getProperty("user.home"), ".m2"), "repository");

        File buildFile = project.getBuildFile();

        util = new DevTaskUtil(project.buildDir, serverInstallDir, getUserDir(project, serverInstallDir),
                serverDirectory, sourceDirectory, testSourceDirectory, configDirectory, project.getRootDir(),
                resourceDirs, hotTests.booleanValue(), skipTests.booleanValue(), artifactId, serverStartTimeout.intValue(),
                verifyAppStartTimeout.intValue(), verifyAppStartTimeout.intValue(), compileWait.doubleValue(), 
                libertyDebug.booleanValue(), pollingTest.booleanValue(), container.booleanValue(), dockerfile, dockerBuildContext, dockerRunOpts, 
                dockerBuildTimeout, skipDefaultPorts.booleanValue(), keepTempDockerfile.booleanValue(), localMavenRepoForFeatureUtility, 
                getPackagingType(), buildFile, generateFeatures.booleanValue()
        );

        util.addShutdownHook(executor);

        List<File> propertyFiles = new ArrayList<File>();
        propertyFiles.add(new File(project.gradle.gradleUserHomeDir, "gradle.properties"));
        propertyFiles.add(new File(project.getRootDir(), "gradle.properties"));
        util.setPropertyFiles(propertyFiles);

        util.startServer();

       
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
            util.watchFiles(outputDirectory, testOutputDirectory, executor, serverXMLFile,
                            project.liberty.server.bootstrapPropertiesFile, project.liberty.server.jvmOptionsFile);
        } catch (PluginScenarioException e) {
            if (e.getMessage() != null) {
                // a proper message is included in the exception if the server has been stopped by another process
                logger.info(e.getMessage());
            }
            return; // enter shutdown hook
        }
    }

    private void addLibertyRuntimeProperties(BuildLauncher gradleBuildLauncher) {
        Set<Entry<Object, Object>> entries = project.getProperties().entrySet()
        for (Entry<Object, Object> entry : entries) {
            String key = (String) entry.getKey()
            if (key.startsWith("liberty.runtime")) {
                gradleBuildLauncher.addArguments("-P" + key + "=" + project.getProperty(key));
            }
        }
    }

    void createServerDirectories() {
        File installDirectory = getInstallDir(project);
        if (!installDirectory.isDirectory()) {
            installDirectory.mkdirs();
        }
        File serverDirectory = getServerDir(project);
        if (!serverDirectory.isDirectory()) {
            serverDirectory.mkdirs();
        }
    }

    // Get container option values from build.gradle if not specified on the command line
    private void processContainerParams() throws Exception {
        // process parameters from dev extension
        if (container == null) {
            boolean buildContainerSetting = project.liberty.dev.container; // get from build.gradle or from -Pdev_mode_container=true
            if (buildContainerSetting == null) {
                setContainer(DEFAULT_CONTAINER);
            } else {
                setContainer(buildContainerSetting);
            }
        }

        if (dockerfile == null) {
            File buildDockerfileSetting = project.liberty.dev.dockerfile; // get from build.gradle
            if (buildDockerfileSetting != null) {
                setDockerfile(buildDockerfileSetting.getAbsolutePath()); // setDockerfile will convert it to canonical path
            }
        }

        if (dockerBuildContext == null) {
            File buildDockerBuildContextSetting = project.liberty.dev.dockerBuildContext; // get from build.gradle
            if (buildDockerBuildContextSetting != null) {
                setDockerBuildContext(buildDockerBuildContextSetting.getAbsolutePath()); // setDockerBuildContext will convert it to canonical path
            }
        }

        if (dockerRunOpts == null) {
            String buildDockerRunOptsSetting = project.liberty.dev.dockerRunOpts; // get from build.gradle
            if (buildDockerRunOptsSetting != null) {
                setDockerRunOpts(buildDockerRunOptsSetting);
            }
        }

        if (dockerBuildTimeout == 0) {
            String buildDockerBuildTimeoutSetting = project.liberty.dev.dockerBuildTimeout; // get from build.gradle
            if (buildDockerBuildTimeoutSetting != null) {
                setDockerBuildTimeout(buildDockerBuildTimeoutSetting);
            }
        }

        if (skipDefaultPorts == null) {
            boolean buildSkipDefaultPortsSetting = project.liberty.dev.skipDefaultPorts; // get from build.gradle
            if (buildSkipDefaultPortsSetting == null) {
                setSkipDefaultPorts(DEFAULT_SKIP_DEFAULT_PORTS);
            } else {
                setSkipDefaultPorts(buildSkipDefaultPortsSetting);
            }
        }

        if (keepTempDockerfile == null) {
            boolean buildKeepTempDockerfileSetting = project.liberty.dev.keepTempDockerfile; // get from build.gradle
            if (buildKeepTempDockerfileSetting == null) {
                setKeepTempDockerfile(DEFAULT_KEEP_TEMP_DOCKERFILE);
            } else {
                setKeepTempDockerfile(buildKeepTempDockerfileSetting);
            }
        }
    }

    ProjectConnection initGradleProjectConnection() {
        logger.debug("Gradle user home: " + project.gradle.gradleUserHomeDir)
        return initGradleConnection(project.getRootDir(), project.gradle.gradleUserHomeDir);
    }

    static ProjectConnection initGradleConnection(File rootDir, File gradleUserHomeDir) {
        ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(rootDir)
                .useGradleUserHomeDir(gradleUserHomeDir)
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

    /**
     * Traces root causes of the passed exception and returns a PluginExecutionException if found
     * @param e Exception to search
     * @return PluginExecutionException or null if could not be found
     */
    Exception getPluginExecutionException(Exception exception) {
        Exception rootCause = exception;
        while (rootCause.getCause() != null && rootCause.getCause() != exception) {
            // compare class strings to verify if a PluginExecutionException is present
            // using "rootCause instanceof PluginExecutionException" will return false
            if (rootCause.getClass().toString().equals(PluginExecutionException.toString())) {
                logger.debug("Found PluginExecutionException indicating that the binary-app-scanner.jar could not be resolved")
                return rootCause;
            }
            rootCause = rootCause.getCause();
        }
        return null;
    }

}
