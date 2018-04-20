package net.wasdev.wlp.gradle.plugins;

import org.gradle.tooling.BuildException
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.gradle.api.GradleException

public class TestAppConfigFail extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/sample.servlet")
    static File buildDir = new File(integTestDir, "/test-app-config-fail")
    static String buildFilename = "testAppConfigFail.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runTasks(buildDir, 'libertyStop')
    }

    //Should throw a GradleException when validating the app configuration which resolves as a BuildException.
    @Test(expected = BuildException.class)
    public void test_smart_config_fail() {
        runTasks(buildDir, 'installApps')
    }
}
