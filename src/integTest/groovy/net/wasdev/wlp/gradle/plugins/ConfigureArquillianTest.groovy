package net.wasdev.wlp.gradle.plugins;

import java.io.File
import org.junit.Test
import org.junit.BeforeClass

/**
 * Runs tests on the arquillian-tests project test cases. Any failures will result in
 * this test failing, but you'll have to look at the cause in the arquillian-tests folder.
 */
class ConfigureArquillianTest extends AbstractIntegrationTest {

    static File resourceDir = new File("build/resources/integrationTest/arquillian-tests")
    static File buildDir = new File(integTestDir, "/arquillian-tests")
    static String buildFilename = "build.gradle"
    
    static final ERROR_MESSAGE = "Build(s) failed. Check the build/testBuilds/arquillian-tests folder for more information on the cause, and potential test failures."
    
    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        if(test_mode == "offline"){
            WLP_DIR.replace("\\","/")
        }
        createTestProject(buildDir, resourceDir, buildFilename)
    }
    
    @Test
    public void test_build() {        
        try {
            runTasks(buildDir, 'build')
        } catch (Exception e) {
            throw new AssertionError(ERROR_MESSAGE)
        }
    }
    
}
