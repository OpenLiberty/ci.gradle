/**
* (C) Copyright IBM Corporation 2020.
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

import java.util.Set

import org.gradle.api.artifacts.ResolveException
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

import io.openliberty.tools.common.plugins.util.InstallFeatureUtil
import io.openliberty.tools.common.plugins.util.PluginExecutionException
import io.openliberty.tools.common.plugins.util.PluginScenarioException

public class AbstractFeatureTask extends AbstractServerTask {

    // DevMode uses this option to provide the location of the
    // temporary serverDir it uses after a change to the server.xml
    private String serverDirectoryParam;
    
    public boolean installFeaturesFromAnt;

    @Option(option = 'serverDir', description = '(Optional) Server directory to get the list of features from.')
    void setServerDirectoryParam(String serverDir) {
        this.serverDirectoryParam = serverDir;
    }

    private class InstallFeatureTaskUtil extends InstallFeatureUtil {
        public InstallFeatureTaskUtil(File installDir, String from, String to, Set<String> pluginListedEsas)  throws PluginScenarioException, PluginExecutionException {
            super(installDir, from, to, pluginListedEsas)
        }

        @Override
        public void debug(String msg) {
            logger.debug(msg)
        }

        @Override
        public void debug(String msg, Throwable e) {
            logger.debug(msg, e)
        }

        @Override
        public void debug(Throwable e) {
            logger.debug(e)
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
        public File downloadArtifact(String groupId, String artifactId, String type, String version) throws PluginExecutionException {
            String coordinates = groupId + ":" + artifactId + ":" + version + "@" + type
            def dep = project.dependencies.create(coordinates)
            def config = project.configurations.detachedConfiguration(dep)

            Set<File> files = new HashSet<File>()
            try {
                config.resolvedConfiguration.resolvedArtifacts.each { artifact ->
                    File artifactFile = artifact.file
                    files.add(artifactFile)
                    debug(artifactFile.toString())
                }
            } catch (ResolveException e) {
                throw new PluginExecutionException("Could not find artifact with coordinates " + coordinates, e)
            }

            if (!files) {
                throw new PluginExecutionException("Could not find artifact with coordinates " + coordinates)
            }
            return files.iterator().next()
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

    protected Set<String> getDependencyFeatures() {
        Set<String> features = new HashSet<String>()
        project.configurations.libertyFeature.dependencies.each { dep ->
            logger.debug("Dependency feature: " + dep.name)
            features.add(dep.name)
        }
        return features
    }

    protected Set<String> getInstalledFeatures() throws PluginExecutionException {
        // If getInstallFeatureUtil returns null, then features are installed via ant 
        def pluginListedEsas = getPluginListedFeatures(true)
        InstallFeatureUtil util = getInstallFeatureUtil(pluginListedEsas)
        if(installFeaturesFromAnt) {
            Set<String> featuresInstalledFromAnt;
            if(server.features.name != null) {
                featuresInstalledFromAnt = new HashSet<String>(server.features.name);
                return featuresInstalledFromAnt;
            }
            else {
                featuresInstalledFromAnt = new HashSet<String>();
                return featuresInstalledFromAnt;
            }
        }

        
        def pluginListedFeatures = getPluginListedFeatures(false)
        def dependencyFeatures = getDependencyFeatures()
        def serverFeatures = null;

        // if DevMode provides a server directory parameter use that for finding the server features
        if (serverDirectoryParam != null) {
            serverFeatures = util.getServerFeatures(new File(serverDirectoryParam))
        } else if (getServerDir(project).exists()) {
            serverFeatures = util.getServerFeatures(getServerDir(project))
        }

        Set<String> featuresToInstall = InstallFeatureUtil.combineToSet(pluginListedFeatures, dependencyFeatures, serverFeatures)
        return featuresToInstall 

    }

    protected InstallFeatureUtil getInstallFeatureUtil(Set<String> pluginListedEsas) throws PluginExecutionException {
        
        InstallFeatureUtil util = null;
        try {
            util = new InstallFeatureTaskUtil(getInstallDir(project), server.features.from, server.features.to, pluginListedEsas)
        } catch (PluginScenarioException e) {
            logger.debug(e.getMessage())
            logger.debug("Installing features from installUtility.")
            installFeaturesFromAnt = true
            return
        }
        return util;
    }

}