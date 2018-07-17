import org.junit.Test

public class NoAppsTemplateTest {
    def projectDir = new File('.')

    @Test
    public void test_app_installed_correctly() {
        assert new File(projectDir, 'build/wlp/usr/servers/testServer').exists() : 'testServer was not created'
        assert new File(projectDir, 'build/wlp/usr/servers/testServer/apps').exists() : 'testServer/apps was not created'
        assert new File(projectDir, 'build/wlp/usr/servers/testServer/dropins').exists() : 'testServer/dropins was not created'
        assert new File(projectDir, 'build/wlp/usr/servers/testServer/apps/sample.servlet-1.war').exists() : 'test app was not installed to apps'
        assert new File(projectDir, 'build/wlp/usr/servers/testServer/dropins/sample.servlet2-1.war').exists() : 'test app was not installed to dropins'
    }
}
