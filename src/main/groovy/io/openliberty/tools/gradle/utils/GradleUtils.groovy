/*
 * (C) Copyright IBM Corporation 2026.
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

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.util.GradleVersion

/**
 * Utility class for Gradle-related operations and validations
 */
class GradleUtils {
    
    /**
     * Validates project dependency configuration for Gradle 9 compatibility
     * 
     * @param project The Gradle project
     * @param dependency The project dependency to validate
     */
    static void validateProjectDependencyConfiguration(Project project, ProjectDependency dependency) {
        if (GradleVersion.current().compareTo(GradleVersion.version("9.0")) >= 0
                && dependency.getTargetConfiguration() == 'archives') {
            project.getLogger().warn("WARNING: Using 'configuration:archives' with project dependencies is deprecated in Gradle 9. " +
                    "This may lead to deployment problems. " +
                    "Please create a custom configuration (e.g., 'warOnly', 'jarOnly') and use that instead. " +
                    "See migration guide for more information.")
        }
    }
    
    /**
     * Validates simple project dependency for Gradle 9 compatibility
     * 
     * @param project The Gradle project
     * @param dependency The project dependency to validate
     */
    static void validateSimpleProjectDependency(Project project, ProjectDependency dependency) {
        if (GradleVersion.current().compareTo(GradleVersion.version("9.0")) >= 0
                && dependency.getTargetConfiguration() == null) {
            project.getLogger().warn("WARNING: Simple project dependencies like project(':jar') are not recommended in Gradle 9. " +
                    "This may lead to deployment problems. " +
                    "Please specify a configuration explicitly: project(path: ':jar', configuration: 'jarOnly') " +
                    "See migration guide for more information.")
        }
    }
}
