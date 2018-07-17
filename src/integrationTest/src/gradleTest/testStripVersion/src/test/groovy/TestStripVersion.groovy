package net.wasdev.wlp.gradle.plugins;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class TestStripVersion {
    def projectDir = new File('.')

    @Test
    public void test_strip_version_true() {
        assert new File(projectDir, 'build/wlp/usr/servers/LibertyProjectServer/apps/sample.servlet.war').exists() : 'version was NOT removed properly when stripVersion was set to true'
    }
}
