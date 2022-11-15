package io.openliberty.tools.gradle

import static org.junit.Assert.*

import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import org.junit.BeforeClass
import org.junit.Test
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class InstallLiberty_installDir_plus_create_server_Test extends AbstractIntegrationTest {
    static File resourceDir = new File("build/resources/test/install-dir-property-test/installDir-plus-create-server")

    static File buildDir = new File(integTestDir, "/InstallLiberty_installDir_plus_create_server")
    static String buildFilename = "build.gradle"

    static File expectedJvmOptionsFile = new File(buildDir, "prebuild/build/wlp/usr/servers/test/jvm.options")
    static File expectedBootstrapPropsFile = new File(buildDir, "prebuild/build/wlp/usr/servers/test/bootstrap.properties")

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @Test
    void test1_installLiberty() {
        BuildResult result = GradleRunner.create()
            .withProjectDir(buildDir)
            .forwardOutput()
            .withArguments('libertyCreate', '-i', '-s')
            .build()

        String output = result.getOutput()
        assert output.contains("Liberty is already installed at") : "Expected installLiberty to detect existing installation at installDir"

        assert !output.contains("jvm.options file deleted before processing plugin configuration.") : "Expected jvm.options file to not be deleted."
        assert expectedJvmOptionsFile.exists()

        assert !output.contains("bootstrap.properties file deleted before processing plugin configuration.") : "Expected jvm.options file to not be deleted."
        assert expectedBootstrapPropsFile.exists()

    }

}