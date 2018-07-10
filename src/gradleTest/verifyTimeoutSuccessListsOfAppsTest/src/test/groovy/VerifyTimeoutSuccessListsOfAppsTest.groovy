import org.junit.Test

public class VerifyTimeoutSuccessListsOfAppsTest {
    def projectDir = new File('.')

    @Test
    public void test_start_with_timeout_success() {
        assert new File(projectDir, 'build/wlp/usr/servers/LibertyProjectServer/dropins/sample.servlet-1.war').exists() : 'application not installed on server'
        assert new File(projectDir, 'build/wlp/usr/servers/LibertyProjectServer/dropins/sample.servlet4-1.war').exists() : 'application not installed on server'

        assert new File(projectDir, 'build/wlp/usr/servers/LibertyProjectServer/apps/sample.servlet2-1.war').exists() : 'application not installed on server'
        assert new File(projectDir, 'build/wlp/usr/servers/LibertyProjectServer/apps/sample.servlet3-1.war').exists() : 'application not installed on server'
    }
}
