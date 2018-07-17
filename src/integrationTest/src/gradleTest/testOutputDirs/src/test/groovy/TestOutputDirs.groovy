import org.junit.Test

public class TestOutputDirs {
    def projectDir = new File('.')

    @Test
    public void test_start_with_timeout_success() {
        assert new File(projectDir, 'build/testOutputDir').exists() : 'Could not find the outputDir specified in the build file.'
        assert new File(projectDir, 'build/testOutputDir/LibertyProjectServer').exists() : 'Could not find the outputDir specified in the build file.'
    }
}
