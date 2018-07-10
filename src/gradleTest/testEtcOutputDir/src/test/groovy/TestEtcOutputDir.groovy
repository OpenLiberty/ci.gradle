import org.junit.Test

public class TestEtcOutputDir {
    File projectDir = new File('.')

    @Test
    public void test_start_with_timeout_success() {
        assert new File(projectDir, 'build/testEtcOutputDir').exists() : 'Could not find the outputDir specified in the build file.'
        assert new File(projectDir, 'build/testEtcOutputDir/LibertyProjectServer').exists() : 'Could not find the outputDir specified in the build file.'
    }
}
