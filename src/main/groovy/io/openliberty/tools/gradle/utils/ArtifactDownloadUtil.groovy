/**
 * (C) Copyright IBM Corporation 2021, 2025.
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
package io.openliberty.tools.gradle.utils

import io.openliberty.tools.common.plugins.util.VariableUtility
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolveException
import io.openliberty.tools.common.plugins.util.PluginExecutionException
import org.gradle.internal.resolve.ArtifactNotFoundException

public class ArtifactDownloadUtil {
    public static File downloadArtifact(Project project, String groupId, String artifactId, String type, String version) throws PluginExecutionException {
        String coordinates = getResolvedCoordinates(project, groupId, artifactId, version, type)
        def dep = project.dependencies.create(coordinates)
        def config = project.configurations.detachedConfiguration(dep)

        return downloadFile(project, config, coordinates)
    }

    public static File downloadBuildArtifact(Project project, String groupId, String artifactId, String type, String version) throws PluginExecutionException {
        String coordinates = getResolvedCoordinates(project, groupId, artifactId, version, type)
        def dep = project.buildscript.dependencies.create(coordinates)
        def config = project.buildscript.configurations.detachedConfiguration(dep)

        return downloadFile(project, config, coordinates)
    }
	
	public static File downloadSignature(Project project, String groupId, String artifactId, String type, String version, File esa) throws PluginExecutionException {
        String coordinates = getResolvedCoordinates(project, groupId, artifactId, version, type)
		def dep = project.dependencies.create(coordinates)
		def config = project.configurations.detachedConfiguration(dep)
		def sig = downloadFile(project, config, coordinates);
		//if signature and esa file are not in same directory, copy signature file to esa parent directory.
		if (!sig.getParent().equals(esa.getParent())) {
            project.getLogger().debug("Copying " + sig + " to " + esa.getAbsolutePath() + ".asc")
			FileUtils.copyFile(sig, new File(esa.getAbsolutePath() + ".asc"))
		}
		return sig
		
	}

    private static File downloadFile(project, config, coordinates) {
        Set<File> files = new HashSet<File>()
        try {
            config.resolvedConfiguration.resolvedArtifacts.each { artifact ->
                File artifactFile = artifact.file
                files.add(artifactFile)
                project.getLogger().debug(artifactFile.toString())
            }
        } catch (ResolveException | ArtifactNotFoundException e) {
            throw new PluginExecutionException("Could not find artifact with coordinates " + coordinates, e)
        }

        if (!files) {
            throw new PluginExecutionException("Could not find artifact with coordinates " + coordinates)
        }
        return files.iterator().next()
    }

    /**
     * get coordinates after resolving any variables
     * @param project
     * @param groupId
     * @param artifactId
     * @param version
     * @param type
     * @return
     */
    public static String getResolvedCoordinates(Project project, String groupId, String artifactId, String version, String type) {
        Properties projectProps = new Properties();
        project.properties.entrySet().forEach { p -> if (p.value != null) { projectProps.put(p.key, p.value) } }
        String resolvedGroupId = VariableUtility.resolveVariables(new CommonLogger(project), groupId, Collections.emptyList(), projectProps, new Properties(), Collections.emptyMap())
        String resolvedArtifactId = VariableUtility.resolveVariables(new CommonLogger(project), artifactId, Collections.emptyList(), projectProps, new Properties(), Collections.emptyMap())
        String resolvedVersion = VariableUtility.resolveVariables(new CommonLogger(project), version, Collections.emptyList(), projectProps, new Properties(), Collections.emptyMap())
        String resolvedType = VariableUtility.resolveVariables(new CommonLogger(project), type, Collections.emptyList(), projectProps, new Properties(), Collections.emptyMap())
        return resolvedGroupId + ":" + resolvedArtifactId + ":" + resolvedVersion + "@" + resolvedType
    }
}