package net.wasdev.wlp.gradle.plugins;

import java.io.File

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class TestCreateWithFiles extends AbstractIntegrationTest{
    static File sourceDir = new File("build/resources/integrationTest/server-config")
    static File buildDir = new File(integTestDir, "/test-create-with-files")
    static String buildFilename = "testCreateLibertyFiles.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        if(test_mode == "offline"){
            WLP_DIR.replace("\\","/")
        }
        createTestProject(buildDir, sourceDir, buildFilename)
    }
    
    @Test
    public void test_create_with_files() {

        runTasks(buildDir, 'libertyCreate')

        def bootstrapFile = new File("build/testBuilds/test-create-with-files/build/wlp/usr/servers/LibertyProjectServer/bootstrap.properties")
        def jvmOptionsFile = new File("build/testBuilds/test-create-with-files/build/wlp/usr/servers/LibertyProjectServer/jvm.options")
        def configFile = new File("build/testBuilds/test-create-with-files/build/wlp/usr/servers/LibertyProjectServer/server.xml")
        def serverEnvFile = new File("build/testBuilds/test-create-with-files/build/wlp/usr/servers/LibertyProjectServer/server.env")

        assert bootstrapFile.exists() : "file not found"
        assert jvmOptionsFile.exists() : "file not found"
        assert configFile.exists() : "file not found"
        assert serverEnvFile.exists() : "file not found"

        assert bootstrapFile.text.equals(new File("build/testBuilds/test-create-with-files/src/main/liberty/config/bootstrap.test.properties").text) : "bootstrap.test.properties file did not copy properly"
    }
}