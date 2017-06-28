package net.wasdev.wlp.gradle.plugins;

import java.io.File

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class VerifyTimeoutTest extends AbstractIntegrationTest{
    static File sourceDir = new File("build/resources/integrationTest/sample.servlet")
    static File buildDir = new File(integTestDir, "/verify-timeout-test")
    static String buildFilename = "verifyTimeoutTest.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        if(test_mode == "offline"){
            WLP_DIR.replace("\\","/")
            createTestProject(buildDir, sourceDir, buildFilename)
        }
        else if(test_mode == "online"){
            createTestProject(buildDir, sourceDir, buildFilename)
        }
        renameBuildFile(buildFilename, buildDir)
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runTasks(buildDir, 'libertyStop')
    }
    
    @Test
    public void test_start_with_timeout() {
        try {
            runTasks(buildDir, 'libertyStart')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStart. "+ e)
        }
    }
}
