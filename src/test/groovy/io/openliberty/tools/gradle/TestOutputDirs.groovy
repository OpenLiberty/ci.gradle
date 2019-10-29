package io.openliberty.tools.gradle;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class TestOutputDirs extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/sample.servlet")
    static File buildDir = new File(integTestDir, "/test-output-dirs")
    static String buildFilename = "testOutputDirs.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runTasks(buildDir, 'libertyStop')
    }

    @Test
    public void test_start_with_timeout_success() {
        try {
            runTasks(buildDir, 'libertyStart')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStart. "+ e)
        }
        assert new File('build/testBuilds/test-output-dirs/build/testOutputDir').exists() : 'Could not find the outputDir specified in the build file.'
        assert new File('build/testBuilds/test-output-dirs/build/testOutputDir/LibertyProjectServer').exists() : 'Could not find the outputDir specified in the build file.'
    }
}
