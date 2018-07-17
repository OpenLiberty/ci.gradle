import java.io.File

import org.junit.Test

public class TestCreateWithConfigDir {

    @Test
    public void test_create_with_configDir() {
        def projectDir = new File(".")

        def bootstrapFile = new File(projectDir, "build/wlp/usr/servers/LibertyProjectServer/bootstrap.properties")
        def jvmOptionsFile = new File(projectDir, "build/wlp/usr/servers/LibertyProjectServer/jvm.options")
        def configFile = new File(projectDir, "build/wlp/usr/servers/LibertyProjectServer/server.xml")
        def serverEnvFile = new File(projectDir, "build/wlp/usr/servers/LibertyProjectServer/server.env")

        assert serverEnvFile.exists() : "file not found"
        assert configFile.exists() : "file not found"
        assert bootstrapFile.exists() : "file not found"
        assert jvmOptionsFile.exists() : "file not found"
    }
}
