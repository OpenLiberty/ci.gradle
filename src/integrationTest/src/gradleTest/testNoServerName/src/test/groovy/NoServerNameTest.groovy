import org.junit.Test

public class NoServerNameTest {
    def projectDir = new File ('.')

    @Test
    public void test_start_with_timeout_success() {
        assert new File(projectDir, 'build/wlp/usr/servers/defaultServer').exists() : 'defaultServer was not created'
    }
}
