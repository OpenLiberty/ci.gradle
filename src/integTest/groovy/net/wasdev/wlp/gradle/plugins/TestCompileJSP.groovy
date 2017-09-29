package net.wasdev.wlp.gradle.plugins;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class TestCompileJSP extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/sampleJSP.servlet")
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

    @Test
    public void check_for_jsp() {
        try {
            //runTasks(buildDir, 'compileJsp')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task compileJsp. "+ e)
        }
        assert new File('build/testBuilds/test-compile-jsp/src/main/webapp/index.jsp').exists() : 'compileJsp Directory not found!'
    }


    @Test
    public void test_compile_jsp() {
        try {
            runTasks(buildDir, 'compileJsp')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task compileJsp. "+ e)
        }
        assert(true)
        assert new File('build/testBuilds/test-compile-jsp/build/compileJsp').exists() : 'compileJsp Directory not found!'
    }


    @Test
    public void compile_jsp_class_file() {
      try {
          runTasks(buildDir, 'compileJsp')
      } catch (Exception e) {
          throw new AssertionError ("Fail on task compileJsp. "+ e)
      }
        assert new File('build/testBuilds/test-compile-jsp/build/classes/java/_index.class').exists() : '_index.class not found!'
    }
}
