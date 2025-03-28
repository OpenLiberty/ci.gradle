/**
 * (C) Copyright IBM Corporation 2014, 2025.
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
import io.openliberty.tools.common.plugins.util.DevUtil
import io.openliberty.tools.common.plugins.util.PluginExecutionException
import io.openliberty.tools.common.plugins.util.PluginScenarioException
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil.FeaturesPlatforms

class InstallFeatureTask extends AbstractFeatureTask {

    InstallFeatureTask() {
        configure({
            description = 'Install a new feature to the Liberty server'
            group = 'Liberty'
        })
    }
    
    private String containerName;

    @Option(option = 'containerName', description = 'This parameter is intended for internal use only. If set, features will be installed to the specified Docker container instead of the local server.')
    void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    @TaskAction
    void installFeature() throws PluginExecutionException {
        // If non-container mode, check for Beta version and skip if needed.  Container mode does not need to check since featureUtility will check when it is called.
        def propertiesList = null;
        def openLibertyVersion = null;
		boolean isClosedLiberty = false;
        if (containerName == null) {
            propertiesList = InstallFeatureUtil.loadProperties(getInstallDir(project))
            openLibertyVersion = InstallFeatureUtil.getOpenLibertyVersion(propertiesList)
			isClosedLiberty = InstallFeatureUtil.isClosedLiberty(propertiesList)

            boolean skipBetaInstallFeatureWarning = Boolean.parseBoolean(System.getProperty(DevUtil.SKIP_BETA_INSTALL_WARNING))
            if (InstallFeatureUtil.isOpenLibertyBetaVersion(openLibertyVersion)) {
                if (!skipBetaInstallFeatureWarning) {
                    logger.warn("Features that are not included with the beta runtime cannot be installed. Features that are included with the beta runtime can be enabled by adding them to your server.xml file.")
                }
                return // do not install features if the runtime is a beta version
            }
        }
    
        def pluginListedEsas = getPluginListedFeatures(true)
        def additionalJsons = getAdditionalJsonList();
        def keyMap = getKeyMap();
        InstallFeatureUtil util = getInstallFeatureUtil(pluginListedEsas, propertiesList, openLibertyVersion, containerName, additionalJsons, keyMap)

		if(!pluginListedEsas.isEmpty() && isClosedLiberty) {
			installFeaturesFromAnt = true;
		}

        // if getInstallFeatureUtil failed to retrieve an InstallFeatureUtil instance for util, then features are installed via ant
        if(installFeaturesFromAnt) {
            installFeatureFromAnt();
        }
        else {
            FeaturesPlatforms fp = getSpecifiedFeatures(containerName);
            Set<String> featuresToInstall = fp == null ? new HashSet<String>() : fp.getFeatures();
            Set<String> platformsToInstall = fp == null ? new HashSet<String>() : fp.getPlatforms();

            util.installFeatures(server.features.acceptLicense, new ArrayList<String>(featuresToInstall), new ArrayList<String>(platformsToInstall))
        }
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
        // Set default server.outputDir to liberty-alt-output-dir for installFeature task.
        if (getOutputDir(project).equals(getUserDir(project).toString() + "/servers")) {
            server.outputDir = new File(project.getLayout().getBuildDirectory().getAsFile().get(), "liberty-alt-output-dir");
        }

        def params = buildAntParams()
        project.ant.taskdef(name: 'installFeature',
                            classname: 'io.openliberty.tools.ant.InstallFeatureTask',
                            classpath: project.buildscript.configurations.classpath.asPath)
        project.ant.installFeature(params)
    }
}
