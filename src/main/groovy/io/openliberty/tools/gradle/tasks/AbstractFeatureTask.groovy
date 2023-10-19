/**
 * (C) Copyright IBM Corporation 2021, 2023.
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


import io.openliberty.tools.common.plugins.util.AbstractContainerSupportUtil
import io.openliberty.tools.common.plugins.util.InstallFeatureUtil
import io.openliberty.tools.common.plugins.util.InstallFeatureUtil.ProductProperties
import io.openliberty.tools.common.plugins.util.PluginExecutionException
import io.openliberty.tools.common.plugins.util.PluginScenarioException
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil
import io.openliberty.tools.gradle.utils.ArtifactDownloadUtil
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.artifacts.Configuration

public class AbstractFeatureTask extends AbstractServerTask {

    // DevMode uses this option to provide the location of the
    // temporary serverDir it uses after a change to the server.xml
    private String serverDirectoryParam;

    public boolean installFeaturesFromAnt;

    private InstallFeatureUtil util;

    private ServerFeatureUtil servUtil;
	
	@Internal
 	String jsonCoordinate;

    @Option(option = 'serverDir', description = '(Optional) Server directory to get the list of features from.')
    void setServerDirectoryParam(String serverDir) {
        this.serverDirectoryParam = serverDir;
    }

    private class ServerFeatureTaskUtil extends ServerFeatureUtil {

        @Override
        public void error(String msg) {
            logger.error(msg);
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        @Override
        public void debug(String msg) {
            if (isDebugEnabled()) {
                logger.debug(msg);
            }
        }

        @Override
        public void error(String msg, Throwable throwable) {
            logger.error(msg, e);
        }

        @Override
        public void debug(String msg, Throwable throwable) {
            if (isDebugEnabled()) {
                logger.debug(msg, (Throwable) e);
            }
        }

        @Override
        public void debug(Throwable throwable) {
            if (isDebugEnabled()) {
                logger.debug("Throwable exception received: " + e.getMessage(), (Throwable) e);
            }
        }

        @Override
        public void warn(String msg) {
            if (!suppressLogs) {
                logger.warn(msg);
            } else {
                debug(msg);
            }
        }

        @Override
        public void info(String msg) {
            if (!suppressLogs) {
                logger.lifecycle(msg);
            } else {
                debug(msg);
            }
        }
    }

    private class InstallFeatureTaskUtil extends InstallFeatureUtil {
        public InstallFeatureTaskUtil(File installDir, File buildDir, String from, String to, Set<String> pluginListedEsas, List<ProductProperties> propertiesList, String openLibertyVerion, String containerName, List<String> additionalJsons)  throws PluginScenarioException, PluginExecutionException {
            super(installDir, buildDir, from, to, pluginListedEsas, propertiesList, openLibertyVerion, containerName, additionalJsons)
            setContainerEngine(this);
        }

        @Override
        public void debug(String msg) {
            if (isDebugEnabled()) {
                logger.debug(msg)
            }
        }

        @Override
        public void debug(String msg, Throwable e) {
            if (isDebugEnabled()) {
                logger.debug(msg, (Throwable) e)
            }
        }

        @Override
        public void debug(Throwable e) {
            if (isDebugEnabled()) {
                logger.debug("Throwable exception received: " + e.getMessage(), (Throwable) e)
            }
        }

        @Override
        public void warn(String msg) {
            logger.warn(msg)
        }

        @Override
        public void info(String msg) {
            logger.info(msg)
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isEnabled(LogLevel.DEBUG)
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
        public File downloadArtifact(String groupId, String artifactId, String type, String version) throws PluginExecutionException {
 			//check if jsonCoordinate is provided from prepareFeature task
			def coordinates = groupId + ":" + artifactId + ":" + version
			def configName = "json-" + groupId + version
 			if(jsonCoordinate != null && !jsonCoordinate.isEmpty() && jsonCoordinate.equals(coordinates)) {
 				Configuration provided = project.getConfigurations().findByName(configName);
 				if (provided != null) {
 					return provided.getFiles().iterator().next()
 				}else {
 					return ArtifactDownloadUtil.downloadArtifact(project, groupId, artifactId, type, version);
 				}
 			}
 			return ArtifactDownloadUtil.downloadArtifact(project, groupId, artifactId, type, version);
 		}
    }

    protected Set<String> getPluginListedFeatures(boolean findEsaFiles) {
        def features = server.features.name
        Set<String> result = new HashSet<String>()
        for (String feature : features) {
            if ((findEsaFiles && feature.endsWith(".esa"))
                    || (!findEsaFiles && !feature.endsWith(".esa"))) {
                result.add(feature)
                logger.debug(("Plugin listed " + (findEsaFiles ? "ESA" : "feature") + ": " + feature))
            }
        }
        return result
    }

    @Internal
    protected Set<String> getDependencyFeatures() {
        Set<String> features = new HashSet<String>()
        project.configurations.libertyFeature.dependencies.each { dep ->
            logger.debug("Dependency feature: " + dep.name)
            features.add(dep.name)
        }
        return features
    }

    @Internal
    protected List<String> getAdditionalJsonList() {
        List<String> result = new ArrayList<String>()
        project.configurations.featuresBom.dependencies.each { dep ->
            def coordinate = dep.group + ":" + "features" + ":" + dep.version
			jsonCoordinate = coordinate
            logger.debug("feature Json: " + coordinate)
            result.add(coordinate)
        }
        return result;
    }

    protected Set<String> getSpecifiedFeatures(String containerName) throws PluginExecutionException {
        InstallFeatureUtil util = getInstallFeatureUtil(null, containerName);
        // if createNewInstallFeatureUtil failed to create a new InstallFeatureUtil instance, then features are installed via ant
        if (installFeaturesFromAnt) {
            Set<String> featuresInstalledFromAnt;
            if (server.features.name != null) {
                featuresInstalledFromAnt = new HashSet<String>(server.features.name);
                return featuresInstalledFromAnt;
            } else {
                featuresInstalledFromAnt = new HashSet<String>();
                return featuresInstalledFromAnt;
            }
        }

        def pluginListedFeatures = getPluginListedFeatures(false)
        def dependencyFeatures = getDependencyFeatures()
        def serverFeatures = null;

        // if DevMode provides a server directory parameter use that for finding the server features
        if (serverDirectoryParam != null) {
            serverFeatures = util.getServerFeatures(new File(serverDirectoryParam), getLibertyDirectoryPropertyFiles(serverDirectoryParam))
        } else if (getServerDir(project).exists()) {
            serverFeatures = util.getServerFeatures(getServerDir(project), getLibertyDirectoryPropertyFiles(null))
        }

        Set<String> featuresToInstall = util.combineToSet(pluginListedFeatures, dependencyFeatures, serverFeatures)
        return featuresToInstall
    }

    /**
     * Get a new instance of ServerFeatureUtil
     *
     * @param suppressLogs if true info and warning will be logged as debug
     * @return instance of ServerFeatureUtil
     */
    @Internal
    protected ServerFeatureUtil getServerFeatureUtil(boolean suppressLogs, Map<String, File> libDirPropFiles) {
        if (servUtil == null) {
            servUtil = new ServerFeatureTaskUtil();
            servUtil.setLibertyDirectoryPropertyFiles(libDirPropFiles);
        }
        if (suppressLogs) {
            servUtil.setSuppressLogs(true);
        } else {
            servUtil.setSuppressLogs(false);
        }
        return servUtil;
    }

    private void createNewInstallFeatureUtil(Set<String> pluginListedEsas, List<ProductProperties> propertiesList, String openLibertyVerion, String containerName, List<String> additionalJsons) throws PluginExecutionException {
        try {
            util = new InstallFeatureTaskUtil(getInstallDir(project), project.getBuildDir(), server.features.from, server.features.to, pluginListedEsas, propertiesList, openLibertyVerion, containerName, additionalJsons)
        } catch (PluginScenarioException e) {
            logger.debug("Exception received: " + e.getMessage(), (Throwable) e)
            logger.debug("Installing features from installUtility.")
            installFeaturesFromAnt = true
            return
        }
    }

    protected InstallFeatureUtil getInstallFeatureUtil(Set<String> pluginListedEsas, String containerName) throws PluginExecutionException {
        if (util == null) {
            if (pluginListedEsas == null) {
                pluginListedEsas = getPluginListedFeatures(true);
            }
            def propertiesList = null;
            def openLibertyVersion = null;
            if (containerName == null) {
                propertiesList = InstallFeatureUtil.loadProperties(getInstallDir(project))
                openLibertyVersion = InstallFeatureUtil.getOpenLibertyVersion(propertiesList)
            }
            def additionalJsons = getAdditionalJsonList()
            createNewInstallFeatureUtil(pluginListedEsas, propertiesList, openLibertyVersion, containerName, additionalJsons)
        }
        return util;
    }

    protected InstallFeatureUtil getInstallFeatureUtil(Set<String> pluginListedEsas, List<ProductProperties> propertiesList, String openLibertyVerion, String containerName, List<String> additionalJsons) throws PluginExecutionException {
        createNewInstallFeatureUtil(pluginListedEsas, propertiesList, openLibertyVerion, containerName, additionalJsons)
        return util
    }

    protected void setContainerEngine(AbstractContainerSupportUtil util) throws PluginExecutionException {
        String LIBERTY_DEV_PODMAN = "liberty.dev.podman";
        Map<String, Object> projectProperties = project.getProperties();
        if (!projectProperties.isEmpty() && projectProperties.containsKey(LIBERTY_DEV_PODMAN)) {
            Object isPodman = projectProperties.get(LIBERTY_DEV_PODMAN);
            if (isPodman != null) {
                util.setIsDocker(!(Boolean.parseBoolean(isPodman.toString())));
                logger.debug("liberty.dev.podman was set to: " + (Boolean.parseBoolean(isPodman.toString())));
            }
        }
    }

}