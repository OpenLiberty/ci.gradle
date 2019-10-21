package net.wasdev.wlp.gradle.plugins;

import java.io.File

import org.junit.BeforeClass
import org.junit.Test

public class TestCreateWithConfigDir extends AbstractIntegrationTest{
    static File sourceDir = new File("build/resources/test/server-config")
    static File testBuildDir = new File(integTestDir, "/test-create-with-config-dir")
    static String buildFilename = "testCreateLibertyConfigDir.gradle"

    @BeforeClass
    public static void setup() {
        createDir(testBuildDir)
        createTestProject(testBuildDir, sourceDir, buildFilename)
    }

    @Test
    public void test_create_with_configDir() {

        runTasks(testBuildDir, 'libertyCreate')

        def bootstrapFile = new File("build/testBuilds/test-create-with-config-dir/build/wlp/usr/servers/LibertyProjectServer/bootstrap.properties")
        def jvmOptionsFile = new File("build/testBuilds/test-create-with-config-dir/build/wlp/usr/servers/LibertyProjectServer/jvm.options")
        def configFile = new File("build/testBuilds/test-create-with-config-dir/build/wlp/usr/servers/LibertyProjectServer/server.xml")
        def serverEnvFile = new File("build/testBuilds/test-create-with-config-dir/build/wlp/usr/servers/LibertyProjectServer/server.env")

        assert serverEnvFile.exists() : "file not found"
        assert configFile.exists() : "file not found"
        assert bootstrapFile.exists() : "file not found"
        assert jvmOptionsFile.exists() : "file not found"

        assert bootstrapFile.text.equals(new File("build/testBuilds/test-create-with-config-dir/src/test/resources/bootstrap.properties").text) : "bootstrap.properties file did not copy properly"
        assert jvmOptionsFile.text.equals(new File("build/testBuilds/test-create-with-config-dir/src/test/resources/jvm.options").text) : "jvm.options file did not copy properly"
        assert serverEnvFile.text.equals(new File("build/testBuilds/test-create-with-config-dir/src/test/resources/server.env").text) : "server.env file did not copy properly"
        assert configFile.text.equals(new File("build/testBuilds/test-create-with-config-dir/src/test/resources/server.xml").text) : "server.xml file did not copy properly"

    }
}
