package io.openliberty.tools.gradle;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class VerifyTimeoutSuccessAppsTest extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/sample.servlet")
    static File buildDir = new File(integTestDir, "/verify-timeout-success-apps-test")
    static String buildFilename = "verifyTimeoutSuccessAppsTest.gradle"

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
        assert new File('build/testBuilds/verify-timeout-success-apps-test/build/wlp/usr/servers/LibertyProjectServer/apps/sample.servlet-1.war').exists() : 'application not installed on server'
    }
}
