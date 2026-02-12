/**
 * (C) Copyright IBM Corporation 2024, 2026.
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

import org.apache.commons.io.FilenameUtils
import org.apache.tools.ant.taskdefs.Jar
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.bundling.War
import org.gradle.plugins.ear.Ear

import java.nio.file.Path

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
    public static Map<File, Dependency> getDeployDependencies(Project project) {
        Map<File, Dependency> completeDeployDeps = new HashMap<File, Dependency>();
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

        for (Iterator<Configuration> iter = project.getConfigurations().iterator(); iter.hasNext();) {
            Configuration element = iter.next();
            if (element.canBeResolved) {
                Dependency[] deployDeps = element.getAllDependencies().toArray()
                for (Dependency dependency1 : deployDeps) {
                    if (dependency1 instanceof ProjectDependency) {
                        def projectPath = dependency1.getPath()
                        Project dependencyProject = project.findProject(projectPath)
                        //ignore self dependencies and containment's, some configuration such as nativeImageClasspath has containment's
                        if (dependencyProject != project) {
                            if (allDependentProjects.add(dependencyProject)) {
                                // prevent infinite recursion and redundant work, as it only makes the recursive call if a new project is found.
                                allDependentProjects.addAll(getAllUpstreamProjects(dependencyProject))
                            }
                        }
                    }
                }
            }
        }
        return allDependentProjects;
    }

    /**
     * get web app source directories
     * @param project
     * @return
     */
    public static List<Path> getWebSourceDirectoriesToMonitor(Project project) {
        List<Path> retVal = new ArrayList<Path>();
        Task warTask = project.getTasks().findByName('war')
        if (warTask != null) {
            setWarSourceDir(warTask, retVal)
        } else if (project.configurations.deploy != null) {
            setWarSourceDirForDeployDependencies(project, retVal)
        } else {
            retVal.add("src/main/webapp")
        }
        return retVal;
    }
    /**
     * find war deploy dependencies and add source dir
     * @param project
     * @param retVal
     */
    private static void setWarSourceDirForDeployDependencies(Project project, ArrayList<Path> retVal) {
        Task warTask
        HashMap<File, Dependency> completeDeployDeps = DevTaskHelper.getDeployDependencies(project)
        for (Map.Entry<File, Dependency> entry : completeDeployDeps) {
            Dependency dependency = entry.getValue();
            File dependencyFile = entry.getKey();

            if (dependency instanceof ProjectDependency) {
                def projectPath = dependency.getPath()
                Project dependencyProject = project.findProject(projectPath)
                String projectType = FilenameUtils.getExtension(dependencyFile.toString())
                switch (projectType) {
                    case "war":
                        warTask = dependencyProject.getTasks().findByName('war')
                        if (warTask != null) {
                            setWarSourceDir(warTask, retVal)
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private static void setWarSourceDir(Task warTask, ArrayList<Path> retVal) {
        War war = (War) warTask.getProject().war
        if (war.getWebAppDirectory().getAsFile().get() != null) {
            retVal.add(war.getWebAppDirectory().get().asFile.toPath().toAbsolutePath())
        }
    }

    /**
     * Parses a Boolean from a Object if the Object is not null.  Otherwise returns null.
     * @param value the Object to parse
     * @return a Boolean, or null if value is null
     */
    public static Boolean parseBooleanIfDefined(Object value) {
        if (value != null) {
            return Boolean.parseBoolean(value as String);
        }
        return null;
    }

    /**
     * Update map with list of parent build files and their subsequent child build files
     *
     * @param parentBuildFiles Map of parent build files and subsequent child build files
     * @param proj GradleProject
     */
    public static void updateParentBuildFiles(Map<String, List<String>> parentBuildFiles, Project proj) {
        String parentBuildGradle = proj.getRootProject().getBuildFile().getCanonicalPath()
        List<String> childBuildFiles = new ArrayList<>();
        childBuildFiles.add(proj.getBuildFile().getCanonicalPath())
        for (Project dependencyProject : getAllUpstreamProjects(proj)) {
            childBuildFiles.add(dependencyProject.getBuildFile().getCanonicalPath())
        }
        parentBuildFiles.put(parentBuildGradle, childBuildFiles)
    }
}
