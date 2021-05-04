/*
 * (C) Copyright IBM Corporation 2019.
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

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.junit.Assert.*

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class InstallDirSubProject extends AbstractIntegrationTest {
    static File resourceDir = new File("build/resources/test/sub-project-test")
    static File buildDir = new File(integTestDir, "/InstallDirSubProject")
    static String buildFilename = "install_dir_sub_project.gradle"

    static File runtimeInstallDir = new File(buildDir, 'wlp')
    static File parentProjectBuildDir = new File(buildDir, 'build')
    static File subProjectBuildDir = new File(buildDir, 'webapp/build')

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @Test
    void testProjectDirectoriesBeforeClean() {
        runTasksWithVersion("21.0.0.4", buildDir, 'libertyCreate')
        assert runtimeInstallDir.exists()
        assert parentProjectBuildDir.exists()
        assert new File(parentProjectBuildDir, "liberty-plugin-config.xml").exists()
        assert subProjectBuildDir.exists()
        assert new File(subProjectBuildDir, "liberty-plugin-config.xml").exists()
    }

    @Test
    void testProjectDirectoriesPostClean() {
        runTasksWithVersion("20.0.0.12", buildDir, "clean", "libertyCreate")
        assert parentProjectBuildDir.exists()
        assert new File(parentProjectBuildDir, "liberty-plugin-config.xml").exists()
        assert subProjectBuildDir.exists()
        assert new File(subProjectBuildDir, "liberty-plugin-config.xml").exists()
    }

    protected static void runTasksWithVersion(String runtimeVersion, File projectDir, String... tasks) {
        String runtimeVersionArg = "-PtestRuntimeVersion=" + runtimeVersion

        List<String> args = new ArrayList<String>()
        tasks.each {
            args.add(it)
        }
        args.add(runtimeVersionArg)
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
}