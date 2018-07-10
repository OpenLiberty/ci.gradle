import org.junit.Test

public class TestConfigDropinsApp {
    File projectDir = new File('.')

    @Test
    public void test_start_with_timeout_success() {
        assert new File(projectDir, 'build/wlp/usr/servers/LibertyProjectServer/apps/testWar-1.war').exists() : 'application not installed on server'
    }
}
