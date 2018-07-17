import org.junit.Test

import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCompileJSP {
    def projectDir = new File('.')

    @Test
    public void check_for_jsp() {
        assert new File(projectDir, 'src/main/webapp/index.jsp').exists() : 'index.jsp not found!'
    }

    @Test
    public void test_1() {
        assert new File(projectDir, 'build/compileJsp').exists() : 'compileJsp Directory not found!'
    }

    @Test
    public void test_2() {
        assert new File(projectDir, 'build/classes/java/_index.class').exists() : '_index.class not found!'
    }
}
