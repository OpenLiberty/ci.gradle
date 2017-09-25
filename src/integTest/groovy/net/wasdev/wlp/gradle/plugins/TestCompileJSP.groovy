package net.wasdev.wlp.gradle.plugins;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class TestCompileJSP extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/sample.servlet")
    static File buildDir = new File(integTestDir, "/test-compile-jsp")
    static String buildFilename = "testCompileJSP.gradle"

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
    public void test_compile_jsp() {
        try {
            runTasks(buildDir, 'libertyStart')
            runTasks(buildDir, 'compileJsp')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task compileJsp. "+ e)
        }
        assert new File('build/testBuilds/test-compile-jsp/build/compileJsp').exists() : 'compileJsp Directory not found!'
    }
}
