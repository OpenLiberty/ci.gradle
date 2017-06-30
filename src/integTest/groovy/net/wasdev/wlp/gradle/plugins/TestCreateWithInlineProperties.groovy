package net.wasdev.wlp.gradle.plugins;

import java.io.File

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class TestCreateWithInlineProperties extends AbstractIntegrationTest{
    static File sourceDir = new File("build/resources/integrationTest/sample.servlet")
    static File buildDir = new File(integTestDir, "/test-create-with-inline-properties")
    static String buildFilename = "testCreateLibertyInlineProperties.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        if(test_mode == "offline"){
            WLP_DIR.replace("\\","/")
        }
        createTestProject(buildDir, sourceDir, buildFilename)
        renameBuildFile(buildFilename, buildDir)
    }
    
    @Test
    public void test_create_with_inline_properties() {
        try {
            runTasks(buildDir, 'libertyCreate')

            def bootstrapFile = new File("build/testBuilds/test-create-with-inline-properties/build/wlp/usr/servers/LibertyProjectServer/bootstrap.properties")
            def jvmOptionsFile = new File("build/testBuilds/test-create-with-inline-properties/build/wlp/usr/servers/LibertyProjectServer/jvm.options")

            assert bootstrapFile.exists() : "file not found"
            assert jvmOptionsFile.exists() : "file not found"

        } catch (Exception e) {
            println(e.getMessage())
            org.codehaus.groovy.runtime.StackTraceUtils.printSanitizedStackTrace(e)

            throw new AssertionError ("Fail on task libertyCreate. "+ e)
        }
    }
}