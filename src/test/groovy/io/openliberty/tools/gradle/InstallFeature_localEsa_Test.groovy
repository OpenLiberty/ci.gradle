package io.openliberty.tools.gradle

import static org.junit.Assert.*

import org.junit.BeforeClass
import org.junit.After
import org.junit.AfterClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import io.openliberty.tools.common.plugins.util.PluginExecutionException
import org.gradle.testkit.runner.BuildResult

class InstallFeature_localEsa_Test extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/local-esa-test")
    static File buildDir = new File(integTestDir, "/LocalEsaTest")
	static File jsonPath = new File(buildDir, "io/openliberty/features/json-1.0/23.0.0.10/json-1.0-23.0.0.10.esa")
    def featureFile = new File(buildDir, "build/wlp/lib/features/com.ibm.websphere.appserver.json-1.0.mf")
    

    @BeforeClass
    public static void setup() {
        createTestProject(buildDir, resourceDir, "build.gradle")
    }

    @Test
    public void test_localEsa() {
        try {
            def jsonFile = new File(buildDir, "build/wlp/lib/features/com.ibm.websphere.appserver.json-1.0.mf")
            def jspFile = new File(buildDir, "build/wlp/lib/features/com.ibm.websphere.appserver.jsp-2.3.mf")
            runTasks(buildDir, 'installFeature')

            assert jsonFile.exists() : "json-1.0 cannot be generated"
            assert jspFile.exists() : "jsp-2.3 cannot be generated"
            
        } catch (Exception e) {
            throw new AssertionError ("test should pass", e)
        }
    }



}
