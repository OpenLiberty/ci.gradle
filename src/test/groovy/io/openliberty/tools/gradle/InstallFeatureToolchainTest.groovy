package io.openliberty.tools.gradle

import org.gradle.testkit.runner.BuildResult
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import static org.junit.Assert.assertTrue

class InstallFeatureToolchainTest extends AbstractIntegrationTest {
    
    static File resourceDir = new File("build/resources/test/kernel-install-feature-test")
    static File buildDir = new File(integTestDir, "/install-feature-toolchain-test")
    static String buildFilename = "install_features_dependencies.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)

        File buildFile = new File(buildDir, "build.gradle")
        def fileContent = buildFile.text
        fileContent = fileContent.replace(
            "apply plugin: 'liberty'",
            "apply plugin: 'liberty'\napply plugin: 'java'\n\njava {\n    toolchain {\n        languageVersion = JavaLanguageVersion.of(11)\n    }\n}"
        )
        buildFile.text = fileContent
    }
    
    @Before
    public void before() {
        runTasks(buildDir, "libertyCreate")
        copyServer("server_empty.xml")
        deleteDir(new File(buildDir, "build/wlp/lib/features"))
    }
    
    @Test
    public void testInstallFeatureWithToolchain() {
        BuildResult result = runTasksResult(buildDir, "installFeature")
        
        String output = result.getOutput()

        assertTrue("Should show toolchain configured message for installFeature task",
                output.contains(String.format(TOOLCHAIN_CONFIGURED, "installFeature")))

        assertTrue("Product validation should use toolchain JAVA_HOME",
                output.contains("Product validation is using toolchain JAVA_HOME:"))

        assertTrue("Product validation should complete successfully",
                output.contains("Product validation completed successfully"))
    }

    private copyServer(String serverFile) {
        assertTrue(new File(resourceDir, serverFile).exists())
        copyFile(new File(resourceDir, serverFile), new File(buildDir, "build/wlp/usr/servers/defaultServer/server.xml"))
    }
}
