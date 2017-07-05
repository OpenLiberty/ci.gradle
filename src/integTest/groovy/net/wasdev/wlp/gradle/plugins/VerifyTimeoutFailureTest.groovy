package net.wasdev.wlp.gradle.plugins;

import java.io.File

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.gradle.tooling.BuildException

public class VerifyTimeoutFailureTest extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/sample.servlet")
    static File buildDir = new File(integTestDir, "/verify-timeout-failure-test")
    static String buildFilename = "verifyTimeoutFailureTest.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        if(test_mode == "offline"){
            WLP_DIR.replace("\\","/")
        }
        createTestProject(buildDir, resourceDir)
        renameBuildFile(buildFilename, buildDir)
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runTasks(buildDir, 'libertyStop')
    }
    
    @Test(expected = BuildException.class)
    public void test_start_with_timeout_failure() {
        runTasks(buildDir, 'libertyStart')
    }
}
