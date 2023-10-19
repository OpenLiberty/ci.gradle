/**
 * (C) Copyright IBM Corporation 2019, 2023.
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

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.file.DefaultFilePropertyFactory
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
import io.openliberty.tools.common.plugins.util.InstallFeatureUtil;
import io.openliberty.tools.common.plugins.util.PluginExecutionException
import io.openliberty.tools.common.plugins.util.PluginScenarioException
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil
import io.openliberty.tools.common.plugins.util.ServerStatusUtil
import io.openliberty.tools.common.plugins.util.ProjectModule
import io.openliberty.tools.common.plugins.util.BinaryScannerUtil

import java.util.concurrent.TimeUnit
import java.util.Map.Entry
import java.nio.file.Path;

class DevTask extends AbstractFeatureTask {

    private static final String LIBERTY_DEV_PODMAN = "liberty.dev.podman";
    private static final String LIBERTY_HOSTNAME = "liberty.hostname";
    private static final String LIBERTY_HTTP_PORT = "liberty.http.port";
    private static final String LIBERTY_HTTPS_PORT = "liberty.https.port";
    private static final String MICROSHED_HOSTNAME = "microshed_hostname";
    private static final String MICROSHED_HTTP_PORT = "microshed_http_port";
    private static final String MICROSHED_HTTPS_PORT = "microshed_https_port";
    private static final String WLP_USER_DIR_PROPERTY_NAME = "wlp.user.dir";
    private static final String GEN_FEAT_LIBERTY_DEP_WARNING = "Liberty feature dependencies were detected in the build.gradle file and automatic generation of features is [On]. " +
            "Automatic generation of features does not support Liberty feature dependencies. " +
            "Remove any Liberty feature dependencies from the build.gradle file or disable automatic generation of features by typing 'g' and press Enter.";

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
    private static final boolean DEFAULT_KEEP_TEMP_CONTAINERFILE = false;
    private static final boolean DEFAULT_GENERATE_FEATURES = false;
    private static final boolean DEFAULT_SKIP_INSTALL_FEATURE = false;

    // Debug port for BuildLauncher tasks launched from DevTask as parent JVM
    // (parent defaults to '5005')
    private Integer childDebugPort = null;  // cache
    private static final int DEFAULT_CHILD_DEBUG_PORT = 6006;

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
            this.libertyDebugPort = Integer.valueOf(libertyDebugPort);
        } catch (NumberFormatException e) {
            logger.error(String.format("Unexpected value: %s for dev mode option libertyDebugPort. libertyDebugPort should be a valid integer.", libertyDebugPort));
            throw e;
        }
    }

    private Double compileWait;

    @Option(option = 'compileWait', description = 'Time in seconds to wait before processing Java changes and deletions. The default value is 0.5 seconds.')
    void setCompileWait(String compileWait) {
        try {
            this.compileWait = Double.valueOf(compileWait);
        } catch (NumberFormatException e) {
            logger.error(String.format("Unexpected value: %s for dev mode option compileWait. compileWait should be a valid number.", compileWait));
            throw e;
        }
    }

    private Integer verifyAppStartTimeout;

    @Option(option = 'verifyAppStartTimeout', description = 'Maximum time to wait (in seconds) to verify that the application has started or updated before running tests. The default value is 30 seconds.')
    void setVerifyAppStartTimeout(String verifyAppStartTimeout) {
        try {
            this.verifyAppStartTimeout = Integer.valueOf(verifyAppStartTimeout);
        } catch (NumberFormatException e) {
            logger.error(String.format("Unexpected value: %s for dev mode option verifyAppStartTimeout. verifyAppStartTimeout should be a valid integer.", verifyAppStartTimeout));
            throw e;
        }
    }

    private Integer serverStartTimeout;

    @Option(option = 'serverStartTimeout', description = 'Time in seconds to wait while verifying that the server has started. The default value is 90 seconds.')
    void setServerStartTimeout(String serverStartTimeout) {
        try {
            this.serverStartTimeout = Integer.valueOf(serverStartTimeout);
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

    @Option(option = 'container', description = 'Run the server in a container instead of locally. The default value is false for the libertyDev task, and true for the libertyDevc task.')
    void setContainer(boolean container) {
        this.container = container;
        project.liberty.dev.container = container; // Needed in DeployTask and AbstractServerTask
    }

    Boolean getContainer() {
        return container;
    }

    private File containerfile;

    @Option(option = 'containerfile', description = 'Dev mode will build a container image from the provided Containerfile/Dockerfile and start a container from the new image.')
    void setContainerfile(String containerfile) {
        if (containerfile != null) {
            // ensures the containerfile is defined with the full path - matches how maven behaves
            this.containerfile = convertParameterToCanonicalFile(containerfile, "containerfile");      
        }
    }

    private File dockerfile;

    @Option(option = 'dockerfile', description = 'Alias for containerfile')
    void setDockerfile(String dockerfile) {
        if (dockerfile != null && containerfile == null) {
            setContainerFile(dockerfile)
        }
    }

    private File containerBuildContext;

    @Option(option = 'containerBuildContext', description = 'The container build context used when building the container in dev mode. Defaults to the directory of the Containerfile/Dockerfile if not specified.')
    void setContainerBuildContext(String containerBuildContext) {
        if (containerBuildContext != null) {
            // ensures the containerBuildContext is defined with the full path - matches how maven behaves
            this.containerBuildContext = convertParameterToCanonicalFile(containerBuildContext, "containerBuildContext");      
        }
    }

    private File dockerBuildContext;

    @Option(option = 'dockerBuildContext', description = 'Alias for containerBuildContext') 
    void setDockerBuildContext(String dockerBuildContext) {
        if (dockerBuildContext != null && containerBuildContext == null) {
            setContainerBuildContext(dockerBuildContext)
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

    private String containerRunOpts;

    @Option(option = 'containerRunOpts', description = 'Additional options for the container run command when dev mode starts a container.')
    void setContainerRunOpts(String containerRunOpts) {
        this.containerRunOpts = containerRunOpts;
    }

    private String dockerRunOpts;
    @Option(option = 'dockerRunOpts', description = 'Alias for containerRunOpts')
    void setDockerRunOpts(String dockerRunOpts) {
        if (dockerRunOpts != null && containerRunOpts == null) {
            setContainerRunOpts(dockerRunOpts)
        }
    }

    private int containerBuildTimeout;

    @Option(option = 'containerBuildTimeout', description = 'Specifies the amount of time to wait (in seconds) for the completion of the container operation to build the image.')
    void setContainerBuildTimeout(String inputValue) {
        try {
            this.containerBuildTimeout = Integer.valueOf(inputValue);
        } catch (NumberFormatException e) {
            logger.error(String.format("Unexpected value: %s for dev mode option containerBuildTimeout. containerBuildTimeout should be a valid integer.", inputValue));
            throw e;
        }
    }

    private int dockerBuildTimeout;
    @Option(option = 'dockerBuildTimeout', description = 'Alias for containerBuildTimeout')
    void setDockerBuildTimeout(String inputValue) {
        if (inputValue != null && containerBuildTimeout == null) {
            setContainerBuildTimeout(inputValue)
        }
    }

    private Boolean skipDefaultPorts;

    @Option(option = 'skipDefaultPorts', description = 'If true, the default container port mappings are skipped in the container run command.')
    void setSkipDefaultPorts(boolean skipDefaultPorts) {
        this.skipDefaultPorts = skipDefaultPorts;
    }

    private Boolean keepTempContainerfile;

    @Option(option = 'keepTempContainerfile', description = 'If true, preserve the temporary Containerfile/Dockerfile used to build the container.')
    void setKeepTempContainerfile(boolean keepTempContainerfile) {
        this.keepTempContainerfile = keepTempContainerfile;
    }

    private Boolean keepTempDockerfile;
    @Option(option = 'keepTempDockerfile', description = 'Alias for keepTempContainerfile')
    void setKeepTempDockerfile(boolean keepTempDockerfile) {
        if (keepTempDockerfile != null && keepTempContainerfile == null) {
            setKeepTempContainerfile(keepTempDockerfile)
        }
    }

    @Optional
    @Input
    Boolean generateFeatures;

    // Need to use a string value to allow someone to specify --generateFeatures=false, if not explicitly set defaults to true
    @Option(option = 'generateFeatures', description = 'If true, scan the application binary files to determine which Liberty features should be used. The default value is false.')
    void setGenerateFeatures(String generateFeatures) {
        this.generateFeatures = Boolean.parseBoolean(generateFeatures);
    }

    @Optional
    @Input
    Boolean skipInstallFeature;

    // Need to use a string value to allow someone to specify --skipInstallFeature=true, if not explicitly set defaults to false
    @Option(option = 'skipInstallFeature', description = 'If set to true, the installFeature task will be skipped when dev mode is started on an already existing Liberty runtime installation. It will also be skipped when dev mode is running and a restart of the server is triggered either directly by the user or by application changes. The installFeature task will be invoked though when dev mode is running and a change to the configured features is detected. The default value is false.')
    void setSkipInstallFeature(String skipInstallFeature) {
        this.skipInstallFeature = Boolean.parseBoolean(skipInstallFeature);
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
                    boolean  hotTests, boolean  skipTests, boolean skipInstallFeature, String artifactId, int serverStartTimeout,
                    int verifyAppStartTimeout, int appUpdateTimeout, double compileWait,
                    boolean libertyDebug, boolean pollingTest, boolean container, File containerfile, File containerBuildContext,
                    String containerRunOpts, int containerBuildTimeout, boolean skipDefaultPorts, boolean keepTempContainerfile, 
                    String mavenCacheLocation, String packagingType, File buildFile, boolean generateFeatures
        ) throws IOException, PluginExecutionException {
            super(buildDir, serverDirectory, sourceDirectory, testSourceDirectory, configDirectory, projectDirectory, /* multi module project directory */ projectDirectory,
                    resourceDirs, hotTests, skipTests, false /* skipUTs */, false /* skipITs */, skipInstallFeature, artifactId,  serverStartTimeout,
                    verifyAppStartTimeout, appUpdateTimeout, ((long) (compileWait * 1000L)), libertyDebug,
                    true /* useBuildRecompile */, true /* gradle */, pollingTest, container, containerfile, containerBuildContext, containerRunOpts, containerBuildTimeout, skipDefaultPorts,
                    null /* compileOptions not needed since useBuildRecompile is true */, keepTempContainerfile, mavenCacheLocation, null /* multi module upstream projects */,
                    false /* recompileDependencies only supported in ci.maven */, packagingType, buildFile, null /* parent build files */, generateFeatures, null /* compileArtifactPaths */, null /* testArtifactPaths */, new ArrayList<Path>() /* webResources */
                );

            this.libertyDirPropertyFiles = AbstractServerTask.getLibertyDirectoryPropertyFiles(installDirectory, userDirectory, serverDirectory);
            ServerFeatureUtil servUtil = getServerFeatureUtil(true, libertyDirPropertyFiles);
            this.existingFeatures = servUtil.getServerFeatures(serverDirectory, libertyDirPropertyFiles);

            this.existingLibertyFeatureDependencies = new HashSet<String>();

            project.configurations.getByName('libertyFeature').dependencies.each {
                dep -> this.existingLibertyFeatureDependencies.add(dep.name)
            }

            setContainerEngine(this)
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
        public boolean updateArtifactPaths(ProjectModule projectModule, boolean redeployCheck, boolean generateFeatures, ThreadPoolExecutor executor)
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
        public boolean recompileBuildFile(File buildFile, Set<String> compileArtifactPaths, Set<String> testArtifactPaths, boolean generateFeatures, ThreadPoolExecutor executor) {
            boolean restartServer = false;
            boolean installFeatures = false;
            boolean optimizeGenerateFeatures = false;

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

                // check if compile dependencies have changed
                Configuration existingProjectCompileConfiguration = project.configurations.getByName('providedCompile');
                List<String> existingProjectCompileDependencies = new ArrayList<String>();
                existingProjectCompileConfiguration.dependencies.each { dep -> existingProjectCompileDependencies.add(dep.group + ":" + dep.name + ":" + dep.version) }

                Configuration newProjectCompileConfiguration = newProject.configurations.getByName('providedCompile');
                List<String> newProjectCompileDependencies = new ArrayList<String>();
                newProjectCompileConfiguration.dependencies.each { dep -> newProjectCompileDependencies.add(dep.group + ":" + dep.name + ":" + dep.version) }

                newProjectCompileDependencies.removeAll(existingProjectCompileDependencies);
                if (!newProjectCompileDependencies.isEmpty()) {
                    logger.debug("Compile dependencies changed");
                    optimizeGenerateFeatures = true;
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
            if (optimizeGenerateFeatures && generateFeatures) {
                logger.debug("Detected a change in the compile dependencies, regenerating features");
                // optimize generate features on build dependency change
                boolean generateFeaturesSuccess = libertyGenerateFeatures(null, true);
                if (generateFeaturesSuccess) {
                    util.javaSourceClassPaths.clear();
                } else {
                    installFeatures = false;
                }
            }
            if (restartServer) {
                // - stop Server
                // - create server or runBoostMojo
                // - install feature
                // - deploy app
                // - start server
                util.restartServer();
                return true;
            } else if (installFeatures) {
                try {
                    libertyInstallFeature();
                } catch (PluginExecutionException e) {
                    // display warning if install feature fails, generateFeatures is on, and Liberty features are in buildfile
                    if (e.getCause() instanceof BuildException && generateFeatures) {
                        libertyDependencyWarning(e.getCause());
                    }
                }
            }
            return true;
        }

        // Check if BuildException contains InstallFeature feature conflict error message
        // This method is only meant to be called if generateFeatures == true and Liberty feature dependencies
        // are detected in the build file
        private void libertyDependencyWarning(BuildException e) {
            if (e.getCause() != null && e.getCause().getCause() != null && e.getCause().getCause().getCause() != null) {
                // PluginExecutionException from installFeature will be 3 layers deep
                if (e.getCause().getCause().getCause().getMessage().contains(InstallFeatureUtil.CONFLICT_MESSAGE)) {
                    logger.warn(GEN_FEAT_LIBERTY_DEP_WARNING);
                }
            }
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
        public void installFeatures(File configFile, File serverDir, boolean generateFeatures) {
            ServerFeatureUtil servUtil = getServerFeatureUtil(true, libertyDirPropertyFiles);
            Set<String> features = servUtil.getServerFeatures(serverDir, libertyDirPropertyFiles);

            if (features == null) {
                return;
            }

            Set<String> featuresCopy = new HashSet<String>(features);
            if (existingFeatures != null) {
                features.removeAll(existingFeatures);
                // check if features have been removed
                Set<String> existingFeaturesCopy = new HashSet<String> (existingFeatures);
                existingFeaturesCopy.removeAll(featuresCopy);
                if (!existingFeaturesCopy.isEmpty()) {
                    logger.info("Configuration features have been removed: " + existingFeaturesCopy);
                }
            }

            if (!features.isEmpty()) {
                logger.info("Configuration features have been added: " + features);

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
                } catch (BuildException e) {
                    // stdout/stderr from the installFeature task is sent to the terminal
                    // only need to log the actual stacktrace when debugging
                    logger.error('Failed to install features from configuration file' + e.getMessage())
                    if (generateFeatures && !project.configurations.getByName('libertyFeature').dependencies.isEmpty()) {
                        libertyDependencyWarning(e);
                    }
                } finally {
                    gradleConnection.close();
                }
            }
        }

        @Override
        public ServerFeatureUtil getServerFeatureUtilObj() {
            // suppress logs from ServerFeatureUtil so that dev console is not flooded
            return getServerFeatureUtil(true, libertyDirPropertyFiles);
        }

        @Override
        public Set<String> getExistingFeatures() {
            return this.existingFeatures;
        }

        @Override
        public void updateExistingFeatures() {
            ServerFeatureUtil servUtil = getServerFeatureUtil(true, libertyDirPropertyFiles);
            Set<String> features = servUtil.getServerFeatures(getServerDir(project), libertyDirPropertyFiles);
            existingFeatures = features;
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
                if (skipInstallFeature) {
                    gradleBuildLauncher.addArguments("--exclude-task", "libertyCreate"); // deploy dependsOn libertyCreate which is finalizedBy installFeature
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
                    logger.error(e.getMessage() + "\nTo disable the automatic generation of features, type 'g' and press Enter.");
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

        /**
        * This method is only called from DevUtil.restartServer() which explicitly calls libertyCreate() before calling this method. We need to explicitly
        * exclude the libertyCreate task.
        **/
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
                } else if (skipInstallFeature) {
                    gradleBuildLauncher.addArguments("--exclude-task", "libertyCreate"); // deploy dependsOn libertyCreate which is finalizedBy installFeature
                    gradleBuildLauncher.addArguments("--exclude-task", "installFeature");
                }
                runGradleTask(gradleBuildLauncher, 'deploy');
            } catch (BuildException e) {
                throw new PluginExecutionException(e);
            } finally {
                gradleConnection.close();
            }
        }

        /*
        * This method is only called from common DevUtil.restartServer() method. The installLiberty task should not need to be called, and must be
        * explicitly excluded since libertyCreate dependsOn installLiberty.
        */      
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
                gradleBuildLauncher.addArguments("--exclude-task", "installLiberty");
                if (skipInstallFeature) {
                    gradleBuildLauncher.addArguments("--exclude-task", "installFeature");
                }
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
            if (server.timeout != null && !server.timeout.isEmpty()) {
                try {
                    serverStartTimeout = Integer.valueOf(server.timeout);
                } catch (NumberFormatException e) {
                    logger.error(String.format("Unexpected value: %s for dev mode option server.timeout. server.timeout should be a valid integer.", server.timeout));
                    throw e;
                }
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

        if (skipInstallFeature == null) {
            skipInstallFeature = DEFAULT_SKIP_INSTALL_FEATURE;
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
        DefaultFilePropertyFactory.DefaultDirectoryVar outputDirectory = mainSourceSet.java.classesDirectory;
        DefaultFilePropertyFactory.DefaultDirectoryVar testOutputDirectory = testSourceSet.java.classesDirectory;
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

        String localMavenRepoForFeatureUtility = new File(new File(System.getProperty("user.home"), ".m2"), "repository");

        File buildFile = project.getBuildFile();

        // Instantiate util before any child gradle tasks launched so it can help find available port if needed
        try {
            this.util = new DevTaskUtil(project.buildDir, serverInstallDir, getUserDir(project, serverInstallDir),
                serverDirectory, sourceDirectory, testSourceDirectory, configDirectory, project.getRootDir(),
                resourceDirs, hotTests.booleanValue(), skipTests.booleanValue(), skipInstallFeature.booleanValue(), artifactId, serverStartTimeout.intValue(),
                verifyAppStartTimeout.intValue(), verifyAppStartTimeout.intValue(), compileWait.doubleValue(),
                libertyDebug.booleanValue(), pollingTest.booleanValue(), container.booleanValue(), containerfile, containerBuildContext, containerRunOpts,
                containerBuildTimeout, skipDefaultPorts.booleanValue(), keepTempContainerfile.booleanValue(), localMavenRepoForFeatureUtility,
                getPackagingType(), buildFile, generateFeatures.booleanValue()
            );
        } catch (IOException | PluginExecutionException e) {
            throw new GradleException("Error initializing dev mode.", e)
        }

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

                String generatedFileCanonicalPath;
                try {
                    generatedFileCanonicalPath = new File(configDirectory,
                            BinaryScannerUtil.GENERATED_FEATURES_FILE_PATH).getCanonicalPath();
                } catch (IOException e) {
                    generatedFileCanonicalPath = new File(configDirectory,
                            BinaryScannerUtil.GENERATED_FEATURES_FILE_PATH).toString();
                }
                logger.warn(
                        "The source configuration directory will be modified. Features will automatically be generated in a new file: "
                                + generatedFileCanonicalPath);
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
                boolean isNewInstallation = true;
                // Check to see if Liberty was already installed and set flag accordingly.
                if (serverInstallDir != null) {
                    try {
                        File installDirectoryCanonicalFile = serverInstallDir.getCanonicalFile();
                        // Quick check to see if a Liberty installation exists at the installDirectory
                        File file = new File(installDirectoryCanonicalFile, "lib/ws-launch.jar");
                        if (file.exists()) {
                            isNewInstallation = false;
                            logger.info("Dev mode is using an existing installation.");
                        }
                    } catch (IOException e) {
                    }
                }

                // if skipInstallFeature is set to true, skip installFeature task unless it is a new installation
                if (skipInstallFeature) {
                    logger.debug("skipInstallFeature flag is set to true");
                }
                
                if (!isNewInstallation) {
                    logger.info("Skipping installLiberty task for existing installation.")
                    // will this cause an issue when changing the runtime? Customer would be forced to cleanup first?
                    gradleBuildLauncher.addArguments("--exclude-task", "installLiberty"); // skip installing Liberty at startup since it was already installed
                    if (skipInstallFeature) {
                        logger.info("Skipping installFeature task due to skipInstallFeature configuration.")
                        gradleBuildLauncher.addArguments("--exclude-task", "installFeature"); // skip installing features at startup since flag was set
                    }
                }
                addLibertyRuntimeProperties(gradleBuildLauncher);
                runGradleTask(gradleBuildLauncher, 'libertyCreate');

                if (!skipInstallFeature || isNewInstallation) {
                    // suppress extra install feature warnings (one would have shown up already from the libertyCreate task on the line above)
                    gradleBuildLauncher.addArguments("-D" + DevUtil.SKIP_BETA_INSTALL_WARNING + "=" + Boolean.TRUE.toString());
                    runInstallFeatureTask(gradleBuildLauncher, null);
                }
            } else {
                // skip creating the server and installing features and just propagate the option to 'deploy'
                createServerDirectories();
                gradleBuildLauncher.addArguments("--exclude-task", "installFeature"); // skip installing features at startup since Containerfile/Dockerfile should have RUN features.sh
                gradleBuildLauncher.addArguments(CONTAINER_PROPERTY_ARG);
            }
            runGradleTask(gradleBuildLauncher, 'deploy');
        } finally {
            gradleConnection.close();
        }


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
            util.watchFiles(outputDirectory.get().asFile, testOutputDirectory.get().asFile, executor, serverXMLFile,
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

        File buildContainerfileSetting = project.liberty.dev.containerfile == null ? project.liberty.dev.dockerfile : project.liberty.dev.containerfile; // get from build.gradle
        if (buildContainerfileSetting != null) {
            setContainerfile(buildContainerfileSetting.getAbsolutePath()); // setContainerfile will convert it to canonical path
        }

        File buildContainerBuildContextSetting = project.liberty.dev.containerBuildContext == null ? project.liberty.dev.dockerBuildContext : project.liberty.dev.containerBuildContext; // get from build.gradle
        if (buildContainerBuildContextSetting != null) {
            setContainerBuildContext(buildContainerBuildContextSetting.getAbsolutePath()); // setContainerBuildContext will convert it to canonical path
        }


        String buildContainerRunOptsSetting = project.liberty.dev.containerRunOpts == null ? project.liberty.dev.dockerRunOpts : project.liberty.dev.containerRunOpts; // get from build.gradle
        if (buildContainerRunOptsSetting != null) {
            setContainerRunOpts(buildContainerRunOptsSetting);
        }


        String buildContainerBuildTimeoutSetting = project.liberty.dev.containerBuildTimeout == null ? project.liberty.dev.dockerBuildTimeout : project.liberty.dev.containerBuildTimeout; // get from build.gradle
        if (buildContainerBuildTimeoutSetting != null) {
            setContainerBuildTimeout(buildContainerBuildTimeoutSetting);
        }

        if (skipDefaultPorts == null) {
            boolean buildSkipDefaultPortsSetting = project.liberty.dev.skipDefaultPorts; // get from build.gradle
            if (buildSkipDefaultPortsSetting == null) {
                setSkipDefaultPorts(DEFAULT_SKIP_DEFAULT_PORTS);
            } else {
                setSkipDefaultPorts(buildSkipDefaultPortsSetting);
            }
        }

        if (keepTempContainerfile == null && keepTempDockerfile == null) {
            boolean buildKeepTempContainerfileSetting = project.liberty.dev.keepTempContainerfile; // get from build.gradle
            boolean buildKeepTempDockerfileSetting = project.liberty.dev.keepTempDockerfile;
            if (buildKeepTempContainerfileSetting != null) {
                setKeepTempContainerfile(buildKeepTempContainerfileSetting);
            } else if (buildKeepTempDockerfileSetting != null) {
                setKeepTempContainerfile(buildKeepTempDockerfileSetting);
            } else {
                setKeepTempContainerfile(DEFAULT_KEEP_TEMP_CONTAINERFILE);
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

    void runGradleTask(BuildLauncher buildLauncher, String ... tasks)  {
        buildLauncher
                .setStandardOutput(System.out)
                .setStandardError(System.err)
                .forTasks(tasks);
        if (logger.isEnabled(LogLevel.DEBUG)) {
            buildLauncher.addArguments("--debug");
        }

        if (Boolean.getBoolean("org.gradle.debug")) {
            if (this.childDebugPort == null) {
                childDebugPort = this.util.findAvailablePort(DEFAULT_CHILD_DEBUG_PORT, true)
                logger.warn("Launch JVM with debug port = " + childDebugPort.toString() + ". The child daemon JVM will initially launch in a suspended state and require a debugger to be attached to proceed.")
            }
            buildLauncher.addArguments("-Dorg.gradle.debug.port=" + Integer.toString(childDebugPort))
        }
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
