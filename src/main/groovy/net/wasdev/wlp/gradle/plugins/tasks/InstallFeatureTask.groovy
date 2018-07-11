/**
 * (C) Copyright IBM Corporation 2014, 2018.
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

import java.util.Set

import org.gradle.api.artifacts.Dependency
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction

import net.wasdev.wlp.common.plugins.util.InstallFeatureUtil
import net.wasdev.wlp.common.plugins.util.PluginExecutionException
import net.wasdev.wlp.common.plugins.util.PluginScenarioException

class InstallFeatureTask extends AbstractServerTask {

    InstallFeatureTask() {
        configure({
            description 'Install a new feature to the Liberty server'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })
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
            config.resolvedConfiguration.resolvedArtifacts.each { artifact ->
                File artifactFile = artifact.file
                files.add(artifactFile)
                debug(artifactFile.toString())
            }

            if (!files) {
                throw new PluginExecutionException("Could not find artifact with coordinates " + coordinates)
            }
            return files.iterator().next()
        }
    }

    @TaskAction
    void installFeature() {
        def pluginListedFeatures = getPluginListedFeatures(false)
        def pluginListedEsas = getPluginListedFeatures(true)

        InstallFeatureUtil util
        try {
            util = new InstallFeatureTaskUtil(getInstallDir(project), server.features.from, server.features.to, pluginListedEsas)
        } catch (PluginScenarioException e) {
            logger.debug(e.getMessage())
            logger.debug("Installing features from installUtility.")
            installFeatureFromAnt()
            return
        }

        def dependencyFeatures = getDependencyFeatures()
        def serverFeatures = getServerDir(project).exists() ? util.getServerFeatures(getServerDir(project)) : null

        Set<String> featuresToInstall = InstallFeatureUtil.combineToSet(pluginListedFeatures, dependencyFeatures, serverFeatures)

        util.installFeatures(server.features.acceptLicense, new ArrayList<String>(featuresToInstall))
    }

    private Set<String> getPluginListedFeatures(boolean findEsaFiles) {
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

    private Set<String> getDependencyFeatures() {
        Set<String> features = new HashSet<String>()
        project.configurations.libertyFeature.dependencies.each { dep ->
            logger.debug("Dependency feature: " + dep.name)
            features.add(dep.name)
        }
        return features
    }

    private def buildAntParams() {
        def params = buildLibertyMap(project)
        params.put('acceptLicense', server.features.acceptLicense)
        if (server.features.name != null) {
            params.put('name', server.features.name.join(","))
        }
        if (server.features.to != null) {
            params.put('to', server.features.to)
        }
        if (server.features.from != null) {
            params.put('from', server.features.from)
        }
        params.remove('timeout')
        return params
    }

    void installFeatureFromAnt() {
        def params = buildAntParams()
        project.ant.taskdef(name: 'installFeature',
                            classname: 'net.wasdev.wlp.ant.InstallFeatureTask',
                            classpath: project.buildscript.configurations.classpath.asPath)
        project.ant.installFeature(params)
    }
}
