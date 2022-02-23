package io.openliberty.tools.gradle

import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import static org.junit.Assert.assertFalse

import java.io.File

import org.gradle.testkit.runner.BuildResult

import org.junit.BeforeClass
import org.junit.Test

import io.openliberty.tools.common.plugins.util.OSUtil

public class TestCreateConfigDirUpToDate extends AbstractIntegrationTest {
    static File sourceDir = new File("build/resources/test/server-config")
    static File testBuildDir = new File(integTestDir, "/test-create-config-dir-up-to-date")
    static String buildFilename = "testCreateUpToDate.gradle"

    @BeforeClass
    public static void setup() {
        createDir(testBuildDir)
        createTestProject(testBuildDir, sourceDir, buildFilename, true)
    }

    @Test
    public void test_create_with_default_configDir() {
        //Update contents of file in config directory to test up to date check of libertyCreate
        //Using additional file since Liberty config files are task inputs
        def testTextFile = new File("build/testBuilds/test-create-config-dir-up-to-date/src/main/liberty/config/test.txt")
        testTextFile.createNewFile()
        testTextFile.append('Test Comment')

        runTasks(testBuildDir, 'libertyCreate')

        //Check config directory test file was copied to server directory
        def serverTestTextFile = new File("build/testBuilds/test-create-config-dir-up-to-date/build/wlp/usr/servers/defaultServer/test.txt")

        assert serverTestTextFile.exists() : "file not found"

        //Update config directory test file content
        testTextFile.append('/nTest Comment 2')

        //Rebuild and check libertyCreate is not up to date
        BuildResult result = runTasksResult(testBuildDir, 'libertyCreate')

        assertFalse UP_TO_DATE == result.task(":libertyCreate").getOutcome()

        //Log should show: Input property 'configDir' file <some path>/build/testBuilds/test-create-config-dir-up-to-date/src/main/liberty/config/test.txt has changed.
        String configDirMessageString = "Input property 'configDir' file"
        String testFileMessageString = "/build/testBuilds/test-create-config-dir-up-to-date/src/main/liberty/config/test.txt has changed."

        if (OSUtil.isWindows()) {
            configDirMessageString = "Input property \'configDir\' file"
            testFileMessageString = "\\build\\testBuilds\\test-create-config-dir-up-to-date\\src\\main\\liberty\\config\\test.txt has changed."
        } 

        assert result.getOutput().contains(configDirMessageString)
        assert result.getOutput().contains(testFileMessageString)

        //Check updated file was copied to server directory
        assert serverTestTextFile.text.contains('Test Comment 2')
    }
}