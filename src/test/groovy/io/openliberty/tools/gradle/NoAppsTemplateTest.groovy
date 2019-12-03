package io.openliberty.tools.gradle;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class NoAppsTemplateTest extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/sample.servlet")
    static File buildDir = new File(integTestDir, "/no-apps-template-test")
    static String buildFilename = "noAppsTemplateTest.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @Test
    public void test_app_installed_correctly() {
        try {
            runTasks(buildDir, 'deploy')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy. "+ e)
        }
        assert new File(buildDir, 'build/wlp/usr/servers/testServer').exists() : 'testServer was not created'
        assert new File(buildDir, 'build/wlp/usr/servers/testServer/apps').exists() : 'testServer/apps was not created'
        assert new File(buildDir, 'build/wlp/usr/servers/testServer/dropins').exists() : 'testServer/dropins was not created'
        assert new File(buildDir, 'build/wlp/usr/servers/testServer/apps/sample.servlet-1.war').exists() : 'test app was not installed to apps'
        assert new File(buildDir, 'build/wlp/usr/servers/testServer/dropins/sample.servlet2-1.war').exists() : 'test app was not installed to dropins'
    }
}
