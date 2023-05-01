package io.openliberty.tools.gradle;

import java.io.File

import org.junit.BeforeClass
import org.junit.Test

public class TestCreateWithConfigDir extends AbstractIntegrationTest{
    static File sourceDir = new File("build/resources/test/server-config")
    static File testBuildDir = new File(integTestDir, "/test-create-with-config-dir")
    static String buildFilename = "testCreateLibertyConfigDir.gradle.kts"

    @BeforeClass
    public static void setup() {
        createDir(testBuildDir)
        createTestProject(testBuildDir, sourceDir, buildFilename)
    }

    @Test
    public void test_create_with_configDir() {

        runTasks(testBuildDir, 'libertyCreate')

        def bootstrapFile = new File("build/testBuilds/test-create-with-config-dir/build/wlp/usr/servers/LibertyProjectServer/bootstrap.properties")
        def jvmOptionsFile = new File("build/testBuilds/test-create-with-config-dir/build/wlp/usr/servers/LibertyProjectServer/jvm.options")
        def configFile = new File("build/testBuilds/test-create-with-config-dir/build/wlp/usr/servers/LibertyProjectServer/server.xml")
        def serverEnvFile = new File("build/testBuilds/test-create-with-config-dir/build/wlp/usr/servers/LibertyProjectServer/server.env")
        def copiedJdbcFile = new File("build/testBuilds/test-create-with-config-dir/build/wlp/usr/shared/resources/postgresql-42.3.8.jar")

        assert serverEnvFile.exists() : "file not found"
        assert configFile.exists() : "file not found"
        assert bootstrapFile.exists() : "file not found"
        assert jvmOptionsFile.exists() : "file not found"
        assert copiedJdbcFile.exists() : "file not found"

        assert bootstrapFile.text.equals(new File("build/testBuilds/test-create-with-config-dir/src/test/resources/bootstrap.properties").text) : "bootstrap.properties file did not copy properly"
        assert jvmOptionsFile.text.equals(new File("build/testBuilds/test-create-with-config-dir/src/test/resources/jvm.options").text) : "jvm.options file did not copy properly"
        assert serverEnvFile.text.equals(new File("build/testBuilds/test-create-with-config-dir/src/test/resources/server.env").text) : "server.env file did not copy properly"
        assert configFile.text.equals(new File("build/testBuilds/test-create-with-config-dir/src/test/resources/server.xml").text) : "server.xml file did not copy properly"

    }

    @Test
    public void test_micro_clean_from_config() {

        runTasks(testBuildDir, 'libertyCreate')

        def bootstrapFile = new File("build/testBuilds/test-create-with-config-dir/build/wlp/usr/servers/LibertyProjectServer/bootstrap.properties")
        def jvmOptionsFile = new File("build/testBuilds/test-create-with-config-dir/build/wlp/usr/servers/LibertyProjectServer/jvm.options")

        assert bootstrapFile.exists() : "bootstap.properties was not generated"
        assert jvmOptionsFile.exists() : "jvm.options was not generated"

        def bootstrapSrcFile = new File("build/testBuilds/test-create-with-config-dir/src/test/resources/bootstrap.properties")
        def jvmOptionsSrcFile = new File("build/testBuilds/test-create-with-config-dir/src/test/resources/jvm.options")

        // invalidating while keeping
        bootstrapSrcFile.renameTo 'bootstrap.properties~'
        jvmOptionsSrcFile.renameTo 'jvm.options~'

        runTasks(testBuildDir, 'libertyCreate')

        assert !bootstrapFile.exists() : "bootstrap.properties should be cleaned for new build"
        assert !jvmOptionsFile.exists() : "jvm.options should be cleaned for new build"

    }

    @Test
    public void test_micro_clean_liberty_plugin_variable_config() {

        def gradleProperties = new File("build/testBuilds/test-create-with-config-dir/gradle.properties")
        def libertyPluginVariableConfig = new File("build/testBuilds/test-create-with-config-dir/build/wlp/usr/servers/LibertyProjectServer/configDropins/defaults/liberty-plugin-variable-config.xml")

        gradleProperties.append("liberty.server.defaultVar.postgres.port=51432")
        runTasks(testBuildDir, 'libertyCreate')
        assert libertyPluginVariableConfig.exists() : "liberty variable xml was not generated"

        gradleProperties.write(gradleProperties.text.replaceAll("liberty.server.defaultVar.postgres.port=51432", ""))
        runTasks(testBuildDir, 'libertyCreate')
        assert new File("build/testBuilds/test-create-with-config-dir/build/wlp/usr/servers/LibertyProjectServer/configDropins/defaults").exists() : "verify liberty variable xml path generation"
        assert !libertyPluginVariableConfig.exists() : "liberty variable xml should be cleaned for new build"
        
    }

}
