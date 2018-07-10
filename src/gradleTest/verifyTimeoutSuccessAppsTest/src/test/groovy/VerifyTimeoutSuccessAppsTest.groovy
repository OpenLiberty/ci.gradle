import org.junit.Test

public class VerifyTimeoutSuccessAppsTest {
    def projectDir = new File('.')

    @Test
    public void test_start_with_timeout_success() {
        assert new File(projectDir, 'build/wlp/usr/servers/LibertyProjectServer/apps/sample.servlet-1.war').exists() : 'application not installed on server'
    }
}
