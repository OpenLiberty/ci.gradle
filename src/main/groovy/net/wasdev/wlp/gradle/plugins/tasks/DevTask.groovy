/**
 * (C) Copyright IBM Corporation 2014, 2019.
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

import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.SourceSet
import org.gradle.api.logging.LogLevel

import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.GradleConnector

import net.wasdev.wlp.ant.ServerTask

import net.wasdev.wlp.gradle.plugins.tasks.StartTask

class DevTask extends AbstractServerTask {

    DevTask() {
        configure({
            description "Runs a Liberty dev server"
            logging.level = LogLevel.INFO
            group 'Liberty'
        })
    }

    void runGradleTask(BuildLauncher buildLauncher, String ... tasks) {
        buildLauncher.forTasks(tasks);
        buildLauncher.run();
    }

    @TaskAction
    void action() {
        // https://docs.gradle.org/current/userguide/embedding.html Tooling API docs
        // Represents a long-lived connection to a Gradle project.
        ProjectConnection connection = GradleConnector.newConnector()
            .forProjectDirectory(new File("."))
            .connect();

        try {
            // configure a gradle build launcher
            // you can reuse the launcher to launch additional builds.
            BuildLauncher gradleBuildLauncher = connection.newBuild()
                .setStandardOutput(System.out)
                .setStandardError(System.err);

            // runGradleTask(gradleBuildLauncher, 'compileJava');
            // runGradleTask(gradleBuildLauncher, 'processResources');
            // runGradleTask(gradleBuildLauncher, 'compileTestJava');
            // runGradleTask(gradleBuildLauncher, 'processTestResources');

            SourceSet mainSourceSet = project.sourceSets.main;
            SourceSet testSourceSet = project.sourceSets.test;

            runGradleTask(gradleBuildLauncher, 'libertyStart');

            println 'srcDirs';
            println mainSourceSet.java.srcDirs;
            println testSourceSet.java.srcDirs;

            println 'outputDir'
            println mainSourceSet.java.outputDir;
            println testSourceSet.java.outputDir;

        } finally {
            connection.close();
        }

    }
}
