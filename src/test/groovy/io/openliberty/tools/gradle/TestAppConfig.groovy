package io.openliberty.tools.gradle

import org.gradle.testkit.runner.BuildResult;
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import io.openliberty.tools.common.plugins.util.OSUtil

public class TestAppConfig extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/sample.servlet")
    static File buildDir = new File(integTestDir, "/test-app-config")
    static String buildFilename = "testAppConfig.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runTasks(buildDir, 'libertyStop')
    }

    // Note: Variable expansion log messages are logged at DEBUG level.
    // This test will only pass when run locally in debug mode (--debug).
    @Test
    @Ignore("Requires Gradle debug mode (--debug) to capture debug log messages for variable expansion")
    public void test_start_with_timeout_success() {
        try {
            BuildResult result = runTasksResult(buildDir, 'libertyStart')
            String output = result.getOutput()
            if (OSUtil.isWindows()) {
                assert output.contains('Resolved environment variable "EXP_VAR" in path "!EXP_VAR!_!EXP_VAR3!" to "TEST"'): 'Expected info about expansion variable resolution for !EXP_VAR!_!EXP_VAR3!'
                assert output.contains('Resolved environment variable "EXP_VAR3" in path "!EXP_VAR!_!EXP_VAR3!" to "WINDOWS"'): 'Expected info about expansion variable resolution for TEST_!EXP_VAR3!'
                assert output.contains('Resolved path "!EXP_VAR!_!EXP_VAR3!" to "TEST_WINDOWS"'): 'Expected complete resolved value log for !EXP_VAR!_!EXP_VAR3!'
            } else {
                assert output.contains('Resolved environment variable "EXP_VAR" in path "${EXP_VAR}_${EXP_VAR2}" to "TEST"'): 'Expected info about expansion variable resolution for ${EXP_VAR}_${EXP_VAR2}'
                assert output.contains('Resolved environment variable "EXP_VAR2" in path "${EXP_VAR}_${EXP_VAR2}" to "UNIX"'): 'Expected info about expansion variable resolution for TEST_${EXP_VAR2}'
                assert output.contains('Resolved path "${EXP_VAR}_${EXP_VAR2}" to "TEST_UNIX"'): 'Expected complete resolved value log for ${EXP_VAR}_${EXP_VAR2}'
            }
        } catch (Exception e) {
            throw new AssertionError("Fail on task libertyStart.", e)
        }
        assert new File('build/testBuilds/test-app-config/build/wlp/usr/servers/LibertyProjectServer/apps/sample.servlet-1.war').exists(): 'application not installed on server'
    }
}
