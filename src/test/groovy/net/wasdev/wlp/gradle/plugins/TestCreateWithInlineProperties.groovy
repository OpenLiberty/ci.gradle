package net.wasdev.wlp.gradle.plugins;

import java.io.File

import org.junit.BeforeClass
import org.junit.Test

public class TestCreateWithInlineProperties extends AbstractIntegrationTest{
    static File sourceDir = new File("build/resources/test/server-config-files")
    static File testBuildDir = new File(integTestDir, "/test-create-with-inline-properties")
    static String buildFilename = "testCreateLibertyInlineProperties.gradle"

    @BeforeClass
    public static void setup() {
        createDir(testBuildDir)
        createTestProject(testBuildDir, sourceDir, buildFilename)
    }

    @Test
    public void test_create_with_inline_properties() {

        runTasks(testBuildDir, 'libertyCreate')

        def bootstrapFile = new File("build/testBuilds/test-create-with-inline-properties/build/wlp/usr/servers/LibertyProjectServer/bootstrap.properties")
        def jvmOptionsFile = new File("build/testBuilds/test-create-with-inline-properties/build/wlp/usr/servers/LibertyProjectServer/jvm.options")

        assert bootstrapFile.exists() : "file not found"
        assert jvmOptionsFile.exists() : "file not found"
    }
}
