package net.wasdev.wlp.gradle.plugins;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class TestEtcOutputDir extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/sample.servlet")
    static File buildDir = new File(integTestDir, "/test-etc-output-dir")
    static String buildFilename = "testEtcOutputDir.gradle"

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

        assert new File('build/testBuilds/test-etc-output-dir/build/testEtcOutputDir').exists() : 'Could not find the outputDir specified in the build file.'
        assert new File('build/testBuilds/test-etc-output-dir/build/testEtcOutputDir/LibertyProjectServer').exists() : 'Could not find the outputDir specified in the build file.'
    }
}
