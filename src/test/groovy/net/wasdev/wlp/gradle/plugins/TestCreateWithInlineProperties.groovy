package net.wasdev.wlp.gradle.plugins;

import java.io.File
import java.io.FileInputStream;
import java.util.Properties;

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

        // Verify the server.env file does not contain a keystore_password entry
        File serverEnvFile = new File("build/testBuilds/test-create-with-inline-properties/build/wlp/usr/servers/LibertyProjectServer/server.env");
        assert serverEnvFile.exists() : "file not found"
        FileInputStream input = new FileInputStream(serverEnvFile);

        Properties prop = new Properties();
        prop.load( input );
        String value = prop.getProperty("keystore_password");
        assert value == null : "keystore_password property unexpectedly found"
    }
}
