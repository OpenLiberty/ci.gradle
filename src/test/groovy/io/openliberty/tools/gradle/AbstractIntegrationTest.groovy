/*
 * (C) Copyright IBM Corporation 2015, 2022.
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

package io.openliberty.tools.gradle

import java.io.File

import java.util.List
import java.util.ArrayList

import static org.junit.Assert.*

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.gradle.api.GradleException
import io.openliberty.tools.common.plugins.util.OSUtil

abstract class AbstractIntegrationTest {

    static File integTestDir = new File('build/testBuilds')

    protected static void deleteDir(File dir) {
        if (dir.exists()) {
            if (!dir.deleteDir()) {
                throw new AssertionError("Unable to delete directory '$dir.canonicalPath'.", null)
            }
        }
    }

    protected static void createDir(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new AssertionError("Unable to create directory '$dir.canonicalPath'.", null)
            }
        }
    }

    protected static File copyBuildFiles(File buildFilename, File buildDir) {
        copyBuildFiles(buildFilename, buildDir, false)
    }

    protected static File copyBuildFiles(File buildFile, File buildDir, boolean appendGradleProperties) {
        if (buildFile.getPath().endsWith(".kts")) {
            copyFile(buildFile, new File(buildDir, 'build.gradle.kts'))
        } else {
            copyFile(buildFile, new File(buildDir, 'build.gradle'))
        }

        File destProps = new File(buildDir, 'gradle.properties')
        if (appendGradleProperties && destProps.exists()) {
            // if gradle.properties file already exists, need to merge the two files
            mergeFiles(new File("build/gradle.properties"), destProps)
        } else {
            copyFile(new File("build/gradle.properties"), destProps)
        }
    }

    protected static File copySettingsFile(File resourceDir, File destDir) {
        copyFile(new File(resourceDir, "settings.gradle"), new File(destDir, "settings.gradle"))
    }

    protected static File copySettingsKtsFile(File resourceDir, File destDir) {
        copyFile(new File(resourceDir, "settings.gradle.kts"), new File(destDir, "settings.gradle.kts"))
    }

    protected static File createTestProject(File parent, File sourceDir, String buildFilename) {
        createTestProject(parent, sourceDir, buildFilename, false)
    }

    protected static File createTestProject(File parent, File sourceDir, String buildFilename, boolean appendGradleProperties) {
        if (!sourceDir.exists()){
            throw new AssertionError("The source file '${sourceDir.canonicalPath}' doesn't exist.", null)
        }
        try {
            // Copy all resources except the individual test .gradle files
            // Do copy settings.gradle or settings.gradle.kts.
            boolean isKts = buildFilename.endsWith(".kts")
            FileUtils.copyDirectory(sourceDir, parent, new FileFilter() {
               public boolean accept (File pathname) {
                   return ((!pathname.getPath().endsWith(".gradle") && 
                            !pathname.getPath().endsWith(".gradle.kts")) ||
                            (pathname.getPath().endsWith("settings.gradle") && !isKts) ||
                            (pathname.getPath().endsWith("settings.gradle.kts") && isKts) ||
                            pathname.getPath().endsWith("build.gradle"))
               }
            });

            // copy the needed gradle build and property files
            File buildFile = new File(sourceDir, buildFilename)
            copyBuildFiles(buildFile, parent, appendGradleProperties)

        } catch (IOException e) {
            throw new AssertionError("Unable to copy directory '${parent.canonicalPath}'.", e)
        }
    }

    protected static void runTasks(File projectDir, String... tasks) {
        List<String> args = new ArrayList<String>()
        tasks.each {
            args.add(it)
        }
        args.add("-i")
        args.add("-s")

        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withArguments(args)
            .build()

        //'it' is null if tasks is a single String
        if(tasks.length > 1) {
            tasks.each {
                assert SUCCESS == result.task(":$it").getOutcome()
            }
        }
    }

    protected static BuildResult runTasksResult(File projectDir, String... tasks) {
        List<String> args = new ArrayList<String>()
        tasks.each {
            args.add(it)
        }
        args.add("-i")
        args.add("-s")

        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withArguments(args)
            .build()

        return result
    }

    protected static boolean runTaskCheckForUpToDate(File projectDir, String task, String argument) {
        List<String> args = new ArrayList<String>()
        args.add(task)
        args.add(argument)
        args.add("-i")
        args.add("-s")

        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withArguments(args)
            .build()

        return UP_TO_DATE == result.task(":" + task + "").getOutcome()
    }

    protected static File copyFile(File sourceFile, File destFile) {
        if (!sourceFile.exists()){
            throw new AssertionError("The source file '${sourceFile.canonicalPath}' doesn't exist.", null)
        }
        try {
            FileUtils.copyFile(sourceFile, destFile)
        } catch (Exception e) {
            throw new AssertionError("Unable to create file '${destFile.canonicalPath}'.", e)
        }
    }

    protected static File mergeFiles(File sourceFile, File destFile) {
        if (sourceFile.exists()) {
            try {
                String endLineCharacters = "\n"
                if (OSUtil.isWindows()) {
                    endLineCharacters = "\r\n"
                }

                List<String> sourceLines = FileUtils.readLines(sourceFile, (String) null)

                FileUtils.writeStringToFile(destFile, endLineCharacters, (String) null, true)

                for (String sourceLine : sourceLines) {
                    FileUtils.writeStringToFile(destFile, sourceLine + endLineCharacters, (String) null, true)
                }

            } catch (Exception e) {
                throw new AssertionError("Unable to merge file '${sourceFile.canonicalPath}' to '${destFile.canonicalPath}'.", e)
            }
        }
    }

}
