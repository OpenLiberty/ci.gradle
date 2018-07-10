import org.junit.Test

public class TestAppListsWithObjects {
    def projectDir = new File('.')

    @Test
    public void test_start_with_timeout_success() {

        assert new File(projectDir, 'build/wlp/usr/servers/LibertyProjectServer/dropins/sample.servlet-1.war').exists() : 'application not installed on server'
        assert new File(projectDir, 'build/wlp/usr/servers/LibertyProjectServer/dropins/sample.test-1.war').exists() : 'application not installed on server'

        assert new File(projectDir, 'build/wlp/usr/servers/LibertyProjectServer/apps/test.servlet-1.war').exists() : 'application not installed on server'
        assert new File(projectDir, 'build/wlp/usr/servers/LibertyProjectServer/apps/servlet.test-1.war').exists() : 'application not installed on server'
    }
}
