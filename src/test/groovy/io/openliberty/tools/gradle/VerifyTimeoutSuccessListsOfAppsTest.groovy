package io.openliberty.tools.gradle;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class VerifyTimeoutSuccessListsOfAppsTest extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/sample.servlet-noWebAppConfig")
    static File buildDir = new File(integTestDir, "/verify-timeout-success-lists-of-apps-test")
    static String buildFilename = "verifyTimeoutSuccessListsOfAppsTest.gradle"

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
        assert new File('build/testBuilds/verify-timeout-success-lists-of-apps-test/build/wlp/usr/servers/LibertyProjectServer/dropins/sample.servlet-1.war').exists() : 'application not installed on server'
        assert new File('build/testBuilds/verify-timeout-success-lists-of-apps-test/build/wlp/usr/servers/LibertyProjectServer/dropins/sample.servlet4-1.war').exists() : 'application not installed on server'

        assert new File('build/testBuilds/verify-timeout-success-lists-of-apps-test/build/wlp/usr/servers/LibertyProjectServer/apps/sample.servlet2-1.war').exists() : 'application not installed on server'
        assert new File('build/testBuilds/verify-timeout-success-lists-of-apps-test/build/wlp/usr/servers/LibertyProjectServer/apps/sample.servlet3-1.war').exists() : 'application not installed on server'
    }
}
