package net.wasdev.wlp.gradle.plugins;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class TestWarTasksWithDifferentDependencies extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/sample.servlet-noWebAppConfig")
    static File buildDir = new File(integTestDir, "/test-war-tasks-with-different-dependencies")
    static String buildFilename = "testWarTasksWithDifferentDependencies.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runTasks(buildDir, 'libertyStop')
    }

    @Test
    public void test_start_with_timeout_success() {
        try {
            runTasks(buildDir, 'libertyStart')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStart. "+ e)
        }
        assert new File('build/testBuilds/test-war-tasks-with-different-dependencies/build/wlp/usr/servers/LibertyProjectServer/dropins/sample.servlet-1.war').exists() : 'application not installed on server'
        assert new File('build/testBuilds/test-war-tasks-with-different-dependencies/build/wlp/usr/servers/LibertyProjectServer/dropins/sample.servlet4-1.war').exists() : 'application not installed on server'

        assert new File('build/testBuilds/test-war-tasks-with-different-dependencies/build/wlp/usr/servers/LibertyProjectServer/apps/sample.servlet2-1.war').exists() : 'application not installed on server'
        assert new File('build/testBuilds/test-war-tasks-with-different-dependencies/build/wlp/usr/servers/LibertyProjectServer/apps/sample.servlet3-1.war').exists() : 'application not installed on server'
    }
}
