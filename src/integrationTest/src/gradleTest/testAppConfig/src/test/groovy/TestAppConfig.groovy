import org.junit.Test

public class TestAppConfig {
    def projectDir = new File('.')

    @Test
    public void test_start_with_timeout_success() {
        assert new File(projectDir, 'build/wlp/usr/servers/LibertyProjectServer/apps/sample.servlet-1.war').exists() : 'application sample.servlet-1.war not installed on server'
    }
}
