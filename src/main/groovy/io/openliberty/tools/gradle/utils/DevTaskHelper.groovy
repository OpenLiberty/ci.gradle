/**
 * (C) Copyright IBM Corporation 2024
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


import org.apache.tools.ant.taskdefs.Jar
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.bundling.War
import org.gradle.plugins.ear.Ear

public class DevTaskHelper {

    /**
     * <p> Get packaging type of Gradle project
     * @param project gradle project
     * @return
     * @throws Exception
     */
    public static String getPackagingType(Project project) throws Exception{
        if (project.plugins.hasPlugin("war") || !project.tasks.withType(War).isEmpty()) {
            if (project.plugins.hasPlugin("org.springframework.boot")) {
                return "springboot"
            }
            return "war"
        }
        else if (project.plugins.hasPlugin("ear") || !project.tasks.withType(Ear).isEmpty()) {
            return "ear"
        }
        else if (project.plugins.hasPlugin("org.springframework.boot") ) {
            return "springboot"
        } else if (project.plugins.hasPlugin("java") ||!project.tasks.withType(Jar).isEmpty()) {
            return "jar"
        }
        else {
            throw new GradleException("Archive path not found. Supported formats are jar, war, ear, and springboot jar.")
        }
    }

    /**
     * get deploy dependencies for gradle project
     * @param project project
     * @return
     */
    public static HashMap<File, Dependency> getDeployDependencies(Project project) {
        HashMap<File, Dependency> completeDeployDeps = new HashMap<File, Dependency>();
        File[] filesAsDeps = project.configurations.deploy.getFiles().toArray()
        Dependency[] deployDeps = project.configurations.deploy.getAllDependencies().toArray()

        if (filesAsDeps.size() == deployDeps.size()) {
            for (int i = 0; i < filesAsDeps.size(); i++) {
                completeDeployDeps.put(filesAsDeps[i], deployDeps[i])
            }
        }
        return completeDeployDeps
    }

    /**
     * get all upstream projects for a gradle project. Iterate through all projects and recursively find all dependent projects
     * @param project
     * @return
     */
    public static Set<Project> getAllUpstreamProjects(Project project) {
        Set<Project> allDependentProjects = new HashSet<>()

        for (Iterator<Configuration> iter = project.getConfigurations().iterator(); iter.hasNext(); ) {
            Configuration element = iter.next();
            if (element.canBeResolved) {
                Dependency[] deployDeps = element.getAllDependencies().toArray()
                for (Dependency dependency1: deployDeps) {
                    if (dependency1 instanceof ProjectDependency) {
                        Project dependencyProject = dependency1.getDependencyProject()
                        allDependentProjects.add(dependencyProject)
                        allDependentProjects.addAll(getAllUpstreamProjects(dependencyProject))
                    }
                }
            }
        }
        return allDependentProjects;
    }
}
