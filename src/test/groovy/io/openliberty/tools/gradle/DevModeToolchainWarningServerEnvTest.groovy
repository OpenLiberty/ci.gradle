package io.openliberty.tools.gradle

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import java.nio.file.Paths

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

class DevModeToolchainWarningServerEnvTest extends BaseDevTest {

    static final String projectName = "basic-dev-project"

    static File resourceDir = new File("build/resources/test/dev-test/" + projectName)
    static File buildDir = new File(integTestDir, "dev-test/" + projectName + "-toolchain-serverenv-" + System.currentTimeMillis())
    static File configDir = new File(buildDir, "src/main/liberty/config")

    @BeforeClass
    static void setup() throws Exception {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)

        File buildFile = new File(buildDir, "build.gradle")
        buildFile.append("\n\njava {\n    toolchain {\n        languageVersion.set(JavaLanguageVersion.of(11))\n    }\n}\n")

        // Create server.env with JAVA_HOME
        createDir(configDir)
        File serverEnvFile = new File(configDir, "server.env")
        def javaHome = Paths.get(System.getenv("JAVA_HOME")).toString()
        serverEnvFile.write("JAVA_HOME=" + javaHome + "\n")

        runDevMode("--info --skipTests --generateFeatures=false", buildDir)
    }

    @Test
    void verifyToolchainWarningInDevMode() throws Exception {
        assertTrue("Toolchain warning message should appear", 
            verifyLogMessage(60000, "CWWKM4101W: The toolchain JDK configuration for task :libertyDev is not honored because the JAVA_HOME property is specified in server.env.", logFile))

        assertFalse("Toolchain configured message should NOT appear when JAVA_HOME is in server.env",
            verifyLogMessage(0, String.format(TOOLCHAIN_CONFIGURED, "libertyDev"), logFile))
    }

    @Test
    void verifyToolchainNotUsedForDevRecompile() throws Exception {
        File javaFile = new File(buildDir, "src/main/java/com/demo/HelloWorld.java")
        assertTrue("Source file not found: " + javaFile.getCanonicalPath(), javaFile.exists())

        waitLongEnough()
        BufferedWriter writer = new BufferedWriter(new FileWriter(javaFile, true))
        writer.append(" // adding for testing toolchain devmode recompile with server.env")
        writer.close()

        assertFalse("Toolchain usage message should NOT appear when JAVA_HOME is in server.env",
            verifyLogMessage(10000, "Using Java toolchain for dev mode compilation:", logFile))

        // Test that the toolchain is not used for test compilation
        File testDir = new File(buildDir, "src/test/java")
        File unitTestSrcFile = new File(testDir, "ToolchainUnitTest.java")

        if (!testDir.exists()) {
            assertTrue("Failed creating test directory: " + testDir.getCanonicalPath(), testDir.mkdirs())
        } else if (unitTestSrcFile.exists()) {
            assertTrue("Failed deleting existing test file: " + unitTestSrcFile.getCanonicalPath(), unitTestSrcFile.delete())
        }

        String unitTest = """import org.junit.Test;
                            import static org.junit.Assert.*;
                            public class ToolchainUnitTest {
                                @Test
                                public void testTrue() {
                                    assertTrue(true);
                                }
                            }
                            """
        BufferedWriter testWriter = new BufferedWriter(new FileWriter(unitTestSrcFile))
        testWriter.write(unitTest)
        testWriter.close()
        assertTrue(unitTestSrcFile.exists())

        File unitTestTargetFile = new File(targetDir, "classes/java/test/ToolchainUnitTest.class")
        assertTrue(verifyFileExists(unitTestTargetFile, 10000))

        // Verify the "Using Java toolchain" message does NOT appear for test compilation
        assertFalse("Toolchain test compilation message should NOT appear when JAVA_HOME is in server.env",
            verifyLogMessage(10000, "Using Java toolchain for dev mode test compilation:", logFile))
    }

    @AfterClass
    static void cleanUpAfterClass() throws Exception {
        String stdout = getContents(logFile, "Dev mode std output")
        System.out.println(stdout)
        String stderr = getContents(errFile, "Dev mode std error")
        System.out.println(stderr)
        
        // Verify warning appeared at least once
        assertTrue("Toolchain warning should appear when JAVA_HOME is in server.env",
            verifyLogMessage(0, "CWWKM4101W:", logFile))
        
        cleanUpAfterClass(true)
    }
}
