package io.openliberty.tools.gradle;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class NoServerNameTest extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/liberty-test")
    static File buildDir = new File(integTestDir, "/no-server-name-test")
    static String buildFilename = "testNoServerName.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @Test
    public void test_start_with_timeout_success() {
        try {
            runTasks(buildDir, 'libertyCreate')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyCreate. "+ e)
        }
        assert new File(buildDir, 'build/wlp/usr/servers/defaultServer').exists() : 'defaultServer was not created'
    }
}
