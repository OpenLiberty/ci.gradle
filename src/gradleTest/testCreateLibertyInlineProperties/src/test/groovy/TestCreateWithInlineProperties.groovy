import java.io.File

import org.junit.Test

public class TestCreateWithInlineProperties {

    @Test
    public void test_create_with_inline_properties() {
        def projectDir = new File(".")

        def bootstrapFile = new File(projectDir, "build/wlp/usr/servers/LibertyProjectServer/bootstrap.properties")
        def jvmOptionsFile = new File(projectDir, "build/wlp/usr/servers/LibertyProjectServer/jvm.options")

        assert bootstrapFile.exists() : "file not found"
        assert jvmOptionsFile.exists() : "file not found"
    }
}
