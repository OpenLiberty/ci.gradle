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

class VerifyFeatureTest extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/prepare-feature-test")
	static File resourceDir2 = new File("build/resources/test/verify-feature-test")
    static File buildDir = new File(integTestDir, "/VerifyFeature")
	static File resourceSimpleBom = new File(resourceDir, "SimpleActivator-bom-1.0.pom")
	static File resourceSimpleEsa = new File(resourceDir, "SimpleActivatorESA-1.0.esa")
	static File resourceSimpleAsc = new File(resourceDir, "SimpleActivatorESA-1.0.esa.asc")
	static File mavenLocalRepo = new File(System.getProperty("user.home")+ "/.m2/repository")
	static File userTestRepo = new File(mavenLocalRepo, "test/user/test/osgi")
	static File simpleBom = new File(userTestRepo, "SimpleActivator-bom/1.0/SimpleActivator-bom-1.0.pom")
	static File simpleEsa = new File(userTestRepo, "SimpleActivatorESA/1.0/SimpleActivatorESA-1.0.esa")
	static File simpleAsc = new File(userTestRepo, "SimpleActivatorESA/1.0/SimpleActivatorESA-1.0.esa.asc")
	static File simpleValidKey = new File(buildDir, "src/test/resources/SimpleActivatorValidKey.asc")
    def featureFile = new File(buildDir, "build/wlp/usr/extension/lib/features/test.user.test.osgi.SimpleActivator.mf")
    

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir2, "build.gradle")
        copyFile(resourceSimpleBom, simpleBom)
		copyFile(resourceSimpleEsa, simpleEsa)	
        copyFile(resourceSimpleAsc, simpleAsc)
    }
	
	@After
	public void cleanup() {
		featureFile.delete()
	}

    
    @Test
    public void test_verifyEnforce() {
        try {
            System.properties['verify'] = 'enforce'
            System.properties['keyid'] = ''
            runTasks(buildDir, 'installFeature')

            assert featureFile.exists() : "SimpleActivator.mf cannot be generated"
            
        } catch (Exception e) {
            throw new AssertionError ("Verify \"enforce\" should pass", e)
        }
    }



    //TODO: Disable for now. 
//    @Test
//    public void test_verifyALL() {
//        try {
//			System.properties['verify'] = 'all'
//			System.properties['keyid'] = '0x05534365803788CE'
//			assert simpleValidKey.exists() : "no valid key"
//			
//            runTasks(buildDir, 'installFeature')
//
//            assert featureFile.exists() : "SimpleActivator.mf cannot be generated"	
//            
//        } catch (Exception e) {
//            throw new AssertionError ("Fail to verify user feature.", e)
//        }
//    }
	
	@Test
	public void test_verifyALLWrongKeyId() {
		boolean testPassed = false;
		System.properties['verify'] = 'all'
		System.properties['keyid'] = '0xWRONGKEY'

		try{
			runTasks(buildDir, 'installFeature')	
		}catch (Exception e) {
            testPassed = true;
        }
		
		assert testPassed == true : "Verify \"all\" with wrong key id should fail"
	}
	
	@Test
	public void test_verifyWARN() {
		try {
			println(featureFile.getAbsolutePath())
			System.properties['verify'] = 'warn'
			System.properties['keyid'] = '0xWRONGKEY'
			
			runTasks(buildDir, 'installFeature')

			assert featureFile.exists() : "SimpleActivator.mf cannot be generated"
			
		} catch (Exception e) {
			throw new AssertionError ("Verify \"warn\" with wrong key id should install the feature, but print warning message", e)
		}
	}
	
	@Test
	public void test_verifySkip() {
		try {
			println(featureFile.getAbsolutePath())
			System.properties['verify'] = 'skip'
			System.properties['keyid'] = '0xWRONGKEY'
			
			runTasks(buildDir, 'installFeature')

			assert featureFile.exists() : "SimpleActivator.mf cannot be generated"
			
		} catch (Exception e) {
			throw new AssertionError ("Verify \"skip\" with wrong key id should install the feature, but print warning message", e)
		}
	}

	
}
