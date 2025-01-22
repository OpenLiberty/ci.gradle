/**
 * (C) Copyright IBM Corporation 2015, 2025.
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
import org.gradle.api.logging.LogLevel
import org.gradle.api.GradleException

class UninstallFeatureTask extends AbstractServerTask {

    UninstallFeatureTask() {
        configure({
            description = 'Uninstall a feature from the Liberty server'
            group = 'Liberty'
        })
    }

    @TaskAction
    void uninstallFeature() {
        if (isLibertyInstalledAndValid(project)) {
            if (server.uninstallfeatures.name != null) {
                def params = buildLibertyMap(project);
                params.remove('timeout')

                def StringBuilder featureFailures = new StringBuilder()
                for (String feature : server.uninstallfeatures.name) {
                    params.put('name', feature)
                    try {
                        uninstallFeature(params)
                    } catch (Exception e) {
                        logger.error ("Exception received while uninstalling feature "+feature+". Exception message: "+e.getMessage())
                        featureFailures.append(feature)
                        featureFailures.append(", ")
                    }
                }

                if (featureFailures.length() > 0) {
                    featureFailures.setLength(featureFailures.length() - 2)
                    throw new GradleException("Failed to uninstall the following features: "+featureFailures.toString())
                }
            }

        } else {
            logger.error ('The runtime has not been installed.')
        }
    }

    protected void uninstallFeature(Map<String, String> params) throws Exception {
            project.ant.taskdef(name: 'uninstallFeature',
                                classname: 'io.openliberty.tools.ant.UninstallFeatureTask',
                                classpath: project.buildscript.configurations.classpath.asPath)
            project.ant.uninstallFeature(params)
    }
}
