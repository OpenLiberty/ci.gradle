import java.io.File

import org.junit.Test

public class TestCreateWithFiles {

    @Test
    public void test_create_with_files() {
        def projectDir = new File(".")

        def bootstrapFile = new File(projectDir, "build/wlp/usr/servers/LibertyProjectServer/bootstrap.properties")
        def jvmOptionsFile = new File(projectDir, "build/wlp/usr/servers/LibertyProjectServer/jvm.options")
        def configFile = new File(projectDir, "build/wlp/usr/servers/LibertyProjectServer/server.xml")
        def serverEnvFile = new File(projectDir, "build/wlp/usr/servers/LibertyProjectServer/server.env")

        assert bootstrapFile.exists() : "file not found"
        assert jvmOptionsFile.exists() : "file not found"
        assert configFile.exists() : "file not found"
        assert serverEnvFile.exists() : "file not found"

        def originalBootstrapFile = new File(projectDir, "../../../resources/test/server-config/bootstrap.test.properties")
        def originalConfigFile = new File(projectDir, "../../../resources/test/server-config/server.xml")

        assert bootstrapFile.text.equals(originalBootstrapFile.text) : "bootstrap.test.properties file did not copy properly"
        assert configFile.text.equals(originalConfigFile.text) : "server.xml file did not copy properly"
    }
}
