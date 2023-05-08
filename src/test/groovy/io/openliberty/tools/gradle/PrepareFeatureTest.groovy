package io.openliberty.tools.gradle

import static org.junit.Assert.*

import org.junit.Before
import org.junit.AfterClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

class PrepareFeatureTest extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/prepare-feature-test")
    static File buildDirSingle = new File(integTestDir, "/PrepareFeature_single")
    static File buildDirMultiple = new File(integTestDir, "/PrepareFeature_multiple")
	static File resourceHelloBom = new File(resourceDir, "hello-bom-1.0.pom")
	static File resourceSimpleBom = new File(resourceDir, "SimpleActivator-bom-1.0.pom")
	static File resourceHelloEsa = new File(resourceDir, "hello-esa-plugin-1.0.esa")
	static File resourceSimpleEsa = new File(resourceDir, "SimpleActivatorESA-1.0.esa")
	static File buildFilename = new File(resourceDir, "build.gradle")
	static File buildFilename_multiple = new File(resourceDir, "build_multiple.gradle")
	static File mavenLocalRepo = new File(System.getProperty("user.home")+ "/.m2/repository")
	static File userTestRepo = new File(mavenLocalRepo, "test/user/test/osgi")
	static File helloBom = new File(userTestRepo, "hello-bom/1.0/hello-bom-1.0.pom")
	static File simpleBom = new File(userTestRepo, "SimpleActivator-bom/1.0/SimpleActivator-bom-1.0.pom")
	static File helloEsa = new File(userTestRepo, "hello-esa-plugin/1.0/hello-esa-plugin-1.0.esa")
	static File simpleEsa = new File(userTestRepo, "SimpleActivatorESA/1.0/SimpleActivatorESA-1.0.esa")
	
	private static final String MIN_USER_FEATURE_VERSION = "21.0.0.11";
	
	public static boolean checkOpenLibertyVersion() {
		DefaultArtifactVersion minVersion = new DefaultArtifactVersion(MIN_USER_FEATURE_VERSION);
		DefaultArtifactVersion version = new DefaultArtifactVersion(System.getProperty("runtimeVersion"))
		if (version.compareTo(minVersion) >= 0) {
			return true
		}
		return false
	}
	

    @Before
    public void setup() {
		org.junit.Assume.assumeTrue(checkOpenLibertyVersion());
        createDir(buildDirSingle)
        createDir(buildDirMultiple)
        copyBuildFiles(buildFilename, buildDirSingle)
        copySettingsFile(resourceDir, buildDirSingle)
        copyBuildFiles(buildFilename_multiple, buildDirMultiple)
        copySettingsFile(resourceDir, buildDirMultiple)
  		copyFile(resourceHelloBom, helloBom)
		copyFile(resourceHelloEsa, helloEsa)
        copyFile(resourceSimpleBom, simpleBom)
		copyFile(resourceSimpleEsa, simpleEsa)	
		
    }


    @Test
    public void test_prepareFeature() {
        try {
			def jsonFile = new File(userTestRepo, "features/1.0/features-1.0.json")
			
            runTasks(buildDirSingle, 'prepareFeature')

            assert jsonFile.exists() : "features.json cannot be generated"	
            deleteDir(jsonFile.getParentFile())
        } catch (Exception e) {
            throw new AssertionError ("Fail on task prepareFeature. "+e)
        }
    }
	
	@Test
	public void test_usrFeatureInstall() {
		try {
			def jsonFile = new File(userTestRepo, "features/1.0/features-1.0.json")
			def file = new File(buildDirSingle, "build/wlp/usr/extension/lib/features/com.ibm.ws.install.helloWorld1.mf")
			
			//installFeature will call prepareFeature when featuresBom is specified.
            runTasks(buildDirSingle, 'installFeature')

            assert jsonFile.exists() : "features.json cannot be generated"
			
			assert file.exists() : "com.ibm.ws.install.helloWorld1.mf is not installed"
			assert file.canRead() : "com.ibm.ws.install.helloWorld1.mf cannot be read"
			
			deleteDir(file.getParentFile())
			deleteDir(jsonFile.getParentFile())
        } catch (Exception e) {
            throw new AssertionError ("Fail to install user feature. " + e)
        }
	}	

	
	@Test
	public void test_multipleUsrFeatureInstall() {
		try {
			def jsonFile = new File(userTestRepo, "features/1.0/features-1.0.json")
			def helloFile = new File(buildDirMultiple, "build/wlp/usr/extension/lib/features/com.ibm.ws.install.helloWorld1.mf")
			def simpleFile = new File(buildDirMultiple, "build/wlp/usr/extension/lib/features/test.user.test.osgi.SimpleActivator.mf")
			
			
			//installFeature will call prepareFeature when featuresBom is specified.
			runTasks(buildDirMultiple, 'installFeature')

			assert jsonFile.exists() : "features.json cannot be generated"
			
			assert helloFile.exists() : "com.ibm.ws.install.helloWorld1.mf is not installed"
			assert helloFile.canRead() : "com.ibm.ws.install.helloWorld1.mf cannot be read"
			assert simpleFile.exists() : "test.user.test.osgi.SimpleActivator.mf is not installed"
			assert simpleFile.canRead() : "test.user.test.osgi.SimpleActivator.mf cannot be read"
			
			deleteDir(helloFile.getParentFile())
			deleteDir(jsonFile.getParentFile())
		} catch (Exception e) {
			throw new AssertionError ("Fail to install multiple user features. " + e)
		}
	}
	
}
