package io.openliberty.tools.gradle

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import static org.junit.Assert.assertTrue

class DevModeToolchainTest extends BaseDevTest {

    static final String projectName = "basic-dev-project"

    static File resourceDir = new File("build/resources/test/dev-test/" + projectName)
    static File buildDir = new File(integTestDir, "dev-test/" + projectName + "-toolchain-" + System.currentTimeMillis())

    @BeforeClass
    static void setup() throws Exception {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
        File buildFile = new File(buildDir, "build.gradle")
        buildFile.append("\n\njava {\n    toolchain {\n        languageVersion.set(JavaLanguageVersion.of(11))\n    }\n}\n")

        runDevMode("--info --skipTests --generateFeatures=false", buildDir)
    }

    @Test
    void verifyToolchainHonoredInDevMode() throws Exception {
        assertTrue(verifyLogMessage(60000, String.format(TOOLCHAIN_USED, "11"), logFile))
        assertTrue(verifyLogMessage(60000, String.format(TOOLCHAIN_CONFIGURED, "libertyDev"), logFile))

        File messagesLogFile = new File(targetDir, "wlp/usr/servers/defaultServer/logs/messages.log")
        assertTrue("messages.log not found: " + messagesLogFile.getCanonicalPath(), messagesLogFile.exists())
        assertTrue(verifyLogMessage(60000, "java.version = 11", messagesLogFile))
    }

    @Test
    void verifyToolchainUsedForDevRecompile() throws Exception {
        File javaFile = new File(buildDir, "src/main/java/com/demo/HelloWorld.java")
        assertTrue("Source file not found: " + javaFile.getCanonicalPath(), javaFile.exists())

        waitLongEnough()
        BufferedWriter writer = new BufferedWriter(new FileWriter(javaFile, true))
        writer.append(" // adding for testing toolchain devmode recompile")
        writer.close()

        assertTrue(verifyLogMessage(60000, "Using Java toolchain for dev mode compilation: version=11, javaHome=", logFile))

        // Test that the toolchain is used for test compilation
        File testDir = new File(buildDir, "src/test/java")
        File unitTestSrcFile = new File(testDir, "ToolchainUnitTest.java")

        if (!testDir.exists()) {
            assertTrue("Failed creating test directory: " + testDir.getCanonicalPath(), testDir.mkdirs())
        } else if (unitTestSrcFile.exists()) {
            assertTrue("Failed deleting existing test file: " + unitTestSrcFile.getCanonicalPath(), unitTestSrcFile.delete())
        }

        String unitTest = """import org.junit.Test;\n
                            import static org.junit.Assert.*;\n
                            public class ToolchainUnitTest {\n
                                @Test\n
                                public void testTrue() {\n
                                    assertTrue(true);\n
                                }\n
                            }\n
                            """
        BufferedWriter testWriter = new BufferedWriter(new FileWriter(unitTestSrcFile))
        testWriter.write(unitTest)
        testWriter.close()
        assertTrue(unitTestSrcFile.exists())

        File unitTestTargetFile = new File(targetDir, "classes/java/test/ToolchainUnitTest.class")
        assertTrue(verifyFileExists(unitTestTargetFile, 10000))

        assertTrue(verifyLogMessage(60000, "Using Java toolchain for dev mode test compilation: version=11, javaHome=", logFile))

        File messagesLogFile = new File(targetDir, "wlp/usr/servers/defaultServer/logs/messages.log")
        assertTrue("messages.log not found: " + messagesLogFile.getCanonicalPath(), messagesLogFile.exists())
        assertTrue(verifyLogMessage(60000, "java.version = 11", messagesLogFile))
    }

    @AfterClass
    static void cleanUpAfterClass() throws Exception {
        String stdout = getContents(logFile, "Dev mode std output")
        System.out.println(stdout)
        String stderr = getContents(errFile, "Dev mode std error")
        System.out.println(stderr)
        assertTrue("Toolchain warning should not appear when dev mode stops",
                !verifyLogMessage(0, "Could not determine JDK home from toolchain", errFile))
        assertTrue("Toolchain honored message should appear in dev mode output",
                verifyLogMessage(0, String.format(TOOLCHAIN_CONFIGURED, "libertyDev"), logFile))
        cleanUpAfterClass(true)
    }
}
