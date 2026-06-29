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
package io.openliberty.tools.gradle

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import static org.junit.Assert.assertTrue

/**
 * Integration test for the fix to:
 *   "Could not get unknown property 'main' for SourceSet container"
 *
 * Reproduces the exact user scenario: an EAR submodule that applies ONLY
 * 'ear' + 'liberty' plugins (no 'java' plugin), while WAR and JAR submodules
 * each apply 'java' themselves.
 *
 * Prior to the fix, DevTask.groovy accessed project.sourceSets.main using
 * Groovy dynamic property syntax, which throws on Gradle 9 when the 'java'
 * plugin is not applied (so 'main' source set does not exist).
 *
 * The fix switches to sourceSets.findByName('main') / findByName('test'),
 * which returns null safely, and falls back to conventional default paths.
 *
 * This test verifies that libertyDev starts successfully on such a project
 * and that hot-recompilation of a JAR module Java file still works.
 */
class TestMultiModuleEarNoJavaPluginDevMode extends BaseDevTest {

    static File resourceDir = new File("build/resources/test/multi-module-loose-ear-no-java-plugin-test")
    static File buildDir    = new File(integTestDir, "/multi-module-loose-ear-no-java-plugin-test")
    static String buildFilename = "build.gradle"

    @BeforeClass
    static void setup() throws IOException, InterruptedException, FileNotFoundException {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
        new File(buildDir, "build").mkdirs()

        // Start libertyDev with --skipTests so we don't need test infrastructure.
        // This exercises the fixed code path in DevTask.action() and
        // DevTask.getProjectModules() for an EAR project without 'java' plugin.
        runDevMode("--skipTests", buildDir)
    }

    /**
     * Primary regression test: libertyDev must start without throwing
     *   "Could not get unknown property 'main' for SourceSet container"
     *
     * If the bug is present, setup() would have failed with a BuildException
     * before this test even runs. The fact that the server reached dev mode
     * proves the fix is effective.
     */
    @Test
    void earModuleWithoutJavaPluginStartsDevMode() throws Exception {
        // Server startup is verified in setup() via verifyLogMessage("Liberty is running in dev mode.").
        // This test documents the intent and provides a named regression marker.
        assertTrue("libertyDev should be running in dev mode",
                verifyLogMessage(5000, "Liberty is running in dev mode."))
    }

    /**
     * Verify that modifying a Java source file in the JAR submodule
     * (which DOES have the 'java' plugin) triggers recompilation while
     * the EAR module's absence of sourceSets.main does not cause a crash.
     */
    @Test
    void modifyJavaFileInJarSubmoduleTriggersRecompilation() throws Exception {
        File srcGreeter = new File(buildDir,
                "jar/src/main/java/io/openliberty/guides/multimodules/lib/Greeter.java")
        File targetGreeter = new File(buildDir,
                "jar/build/classes/java/main/io/openliberty/guides/multimodules/lib/Greeter.class")

        assertTrue("Greeter.java source file must exist", srcGreeter.exists())
        assertTrue("Greeter.class must exist after initial build", targetGreeter.exists())

        long lastModified = targetGreeter.lastModified()
        waitLongEnough()

        // Append a no-op comment to trigger the file watcher
        BufferedWriter javaWriter = new BufferedWriter(new FileWriter(srcGreeter, true))
        javaWriter.append(" // recompile trigger")
        javaWriter.close()

        assertTrue("Greeter.class should be recompiled after source change",
                waitForCompilation(targetGreeter, lastModified, 120000))
    }

    /**
     * Verify that modifying the EAR build.gradle triggers the expected
     * "change detected in build file" dev mode warning.
     */
    @Test
    void modifyEarBuildGradleTriggersWarning() throws Exception {
        waitLongEnough()

        File earBuildGradle = new File(buildDir, "ear/build.gradle")
        assertTrue("ear/build.gradle must exist", earBuildGradle.exists())

        BufferedWriter buildWriter = new BufferedWriter(new FileWriter(earBuildGradle, true))
        buildWriter.append(" // testing")
        buildWriter.close()

        assertTrue("Expected build file change warning in dev mode output",
                verifyLogMessage(120000,
                        "A change was detected in a build file. The libertyDev task could not determine if a server restart is required. To restart server, type 'r' and press Enter."))
    }

    @AfterClass
    static void cleanUpAfterClass() throws Exception {
        String stdout = getContents(logFile, "Dev mode std output")
        System.out.println(stdout)
        String stderr = getContents(errFile, "Dev mode std error")
        System.out.println(stderr)
        cleanUpAfterClass(true)
    }
}
