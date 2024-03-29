package io.openliberty.tools.gradle

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import java.io.BufferedReader;
import java.io.FileReader;


import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestAppendServerEnvWithProps extends AbstractIntegrationTest {
    static File resourceDir = new File("build/resources/test/sample.servlet")
    static File buildDir = new File(integTestDir, "/test-append-server-env-with-props")
    static String buildFilename = "testAppendServerEnvWithEnvProps.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
        runTasks(buildDir, 'libertyCreate')
    }

    @Test
    public void check_for_server_env() {
        assert new File('build/testBuilds/test-append-server-env-with-props/build/wlp/usr/servers/LibertyProjectServer/server.env').exists() : 'server.env not found!'
    }

    /*
        # envProps in build.gradle
        env = ['TEST_PROP_2':'white', 'CONFIG_SERVER_ENV_PROPS':'TEST']
        
        # default server.env
        keystore_password=sfKRrA1ioLdtIFQC9bEfkua

        # Configured server.env
        CONFIG_SERVER_ENV=TEST
        TEST_PROP_2=green
        TEST_PROP_3=blue

        # server.env in configDir
        ConfigDir=TEST
        TEST_PROP_1=red
        TEST_PROP_2=red
        TEST_PROP_3=red

        # Merged server.env
        # Generated by liberty-gradle-plugin
        keystore_password=sfKRrA1ioLdtIFQC9bEfkua
        CONFIG_SERVER_ENV=TEST
        ConfigDir=TEST
        TEST_PROP_3=blue
        CONFIG_SERVER_ENV_PROPS=TEST
        TEST_PROP_2=white
        TEST_PROP_1=red
    */
    @Test
    public void check_server_env_contents() {
        File serverEnv = new File("build/testBuilds/test-append-server-env-with-props/build/wlp/usr/servers/LibertyProjectServer/server.env")
        FileInputStream input = new FileInputStream(serverEnv)
        
        Map<String,String> serverEnvContents = new HashMap<String,String>();

        BufferedReader bf = new BufferedReader(new FileReader(serverEnv))
        String line = bf.readLine();
        boolean commentFound = false
        while(line != null) {
            //ignore comment lines
            if(!line.startsWith("#")) {
                String[] keyValuePair = line.split("=");
                String key = keyValuePair[0];
                String value = keyValuePair[1];

                serverEnvContents.put(key,value);
            } else {
                commentFound = true
                Assert.assertTrue("File should have been generated by liberty-gradle-plugin", line.contains("liberty-gradle-plugin"));
            }
            line = bf.readLine();
        }
        Assert.assertTrue("Expected generated by liberty-gradle-plugin comment not found", commentFound)
        
        // The contents of the default server.env can change over time.
        // After 20.0.0.3, for example, the WLP_SKIP_MAXPERMSIZE was removed.
        // Just confirm the keystore_password is present to prove the default server.env was merged with the plugin config.
        Assert.assertTrue("Number of env properties should be >= 7, but is "+serverEnvContents.size(),  	serverEnvContents.size() >= 7)
        Assert.assertTrue("keystore_password mapping found", serverEnvContents.containsKey("keystore_password"))
        Assert.assertTrue("ConfigDir=TEST mapping found", serverEnvContents.get("ConfigDir").equals("TEST"))
        Assert.assertTrue("CONFIG_SERVER_ENV=TEST mapping found", serverEnvContents.get("CONFIG_SERVER_ENV").equals("TEST"))
        Assert.assertTrue("TEST_PROP_3=blue", serverEnvContents.get("TEST_PROP_3").equals("blue"))
        Assert.assertTrue("CONFIG_SERVER_ENV_PROPS=TEST", serverEnvContents.get("CONFIG_SERVER_ENV_PROPS").equals("TEST"))
        Assert.assertTrue("TEST_PROP_2=white", serverEnvContents.get("TEST_PROP_2").equals("white"))
        Assert.assertTrue("TEST_PROP_1=red", serverEnvContents.get("TEST_PROP_1").equals("red"))

    }

}