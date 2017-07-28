package net.wasdev.wlp.gradle.plugins;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class TestStripVersion extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/sample.servlet")
    static File buildDir = new File(integTestDir, "/test-strip-version")
    static String buildFilename = "TestStripVersion.gradle"

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
    public void test_strip_version_true() {
        try {
            runTasks(buildDir, 'installApps')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task installApps. "+ e)
        }
        assert new File('build/testBuilds/test-strip-version/build/wlp/usr/servers/LibertyProjectServer/apps/sample.servlet.war').exists() : 'version was NOT removed properly when stripVersion was set to true'
    }
}
