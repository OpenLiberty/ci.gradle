package net.wasdev.wlp.gradle.plugins;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class VerifyTimeoutSuccessTest extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/sample.servlet")
    static File buildDir = new File(integTestDir, "/verify-timeout-success-test")
    static String buildFilename = "verifyTimeoutSuccessTest.gradle"

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
    public void test_start_with_timeout_success() {
        try {
            runTasks(buildDir, 'libertyStart')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStart. "+ e)
        }
    }
}
