package net.wasdev.wlp.gradle.plugins;

import java.io.File

import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

public class VerifyTimeoutTest extends AbstractIntegrationTest{
    static File sourceDir = new File("build/resources/integrationTest/sample.servlet")

    @BeforeClass
    public static void setup() {
        deleteDir(integTestDir)
        createDir(integTestDir)
        if(test_mode == "offline"){
            WLP_DIR.replace("\\","/")
            createTestProject(integTestDir, sourceDir)
        }else if(test_mode == "online"){
            createTestProject(integTestDir, sourceDir)
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runTasks(integTestDir, 'libertyStop')
        deleteDir(integTestDir)
    }
    
    @Test
    public void test_start_with_timeout() {
        try {
            runTasks(integTestDir, 'libertyStart')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStart. "+ e)
        }
    }
}
