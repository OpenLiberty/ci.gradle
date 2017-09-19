package net.wasdev.wlp.gradle.plugins;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class TestAppListsWithObjects extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/sample.servlet-noWebAppConfig")
    static File buildDir = new File(integTestDir, "/test-app-lists-with-objects")
    static String buildFilename = "testAppListsWithObjects.gradle"

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
        assert new File('build/testBuilds/test-app-lists-with-objects/build/wlp/usr/servers/LibertyProjectServer/dropins/sample.servlet-1.war').exists() : 'application not installed on server'
        assert new File('build/testBuilds/test-app-lists-with-objects/build/wlp/usr/servers/LibertyProjectServer/dropins/sample.test-1.war').exists() : 'application not installed on server'

        assert new File('build/testBuilds/test-app-lists-with-objects/build/wlp/usr/servers/LibertyProjectServer/apps/test.servlet-1.war').exists() : 'application not installed on server'
        assert new File('build/testBuilds/test-app-lists-with-objects/build/wlp/usr/servers/LibertyProjectServer/apps/servlet.test-1.war').exists() : 'application not installed on server'
    }
}
