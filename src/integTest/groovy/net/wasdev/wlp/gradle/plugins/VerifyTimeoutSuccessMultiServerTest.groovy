package net.wasdev.wlp.gradle.plugins;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class VerifyTimeoutSuccessMultiServerTest extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/sample.servlet")
    static File buildDir = new File(integTestDir, "/verify-timeout-success-multi-server-test")
    static String buildFilename = "verifyTimeoutSuccessMultiServerTest.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        if(test_mode == "offline"){
            WLP_DIR.replace("\\","/")
        }
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runTasks(buildDir, 'libertyStop')
    }

    @Test
    public void test_multi_server_start_with_timeout_success() {
        try {
            runTasks(buildDir, 'libertyStart')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStart. "+ e)
        }
        assert new File('build/testBuilds/verify-timeout-success-multi-server-test/build/wlp/usr/servers/libertyServer1/apps/sample.servlet-1.war').exists() : 'application not installed on server1'
        assert new File('build/testBuilds/verify-timeout-success-multi-server-test/build/wlp/usr/servers/libertyServer2/apps/sample.servlet-1.war').exists() : 'application not installed on server2'
        assert new File('build/testBuilds/verify-timeout-success-multi-server-test/build/wlp/usr/servers/libertyServer3/apps/sample.servlet-1.war').exists() : 'application not installed on server3'
    }
}
