package io.openliberty.tools.gradle

import static org.junit.Assert.*

import org.junit.Before
import org.junit.AfterClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import io.openliberty.tools.common.plugins.util.VersionUtility

class InstallUsrFeature_toExt extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/prepare-feature-test")
    static File buildDir = new File(integTestDir, "/InstallUsrFeature")
    static File resourceHelloBom = new File(resourceDir, "hello-bom-1.0.pom")
	static File resourceHelloEsa = new File(resourceDir, "hello-esa-plugin-1.0.esa")
	static File resourceExtProp = new File(resourceDir, "testExt.properties")
	static File buildFilename = new File(resourceDir, "build_wlp.gradle")
	static File mavenLocalRepo = new File(System.getProperty("user.home")+ "/.m2/repository")
	static File userTestRepo = new File(mavenLocalRepo, "test/user/test/osgi")
	static File helloBom = new File(userTestRepo, "hello-bom/1.0/hello-bom-1.0.pom")
	static File helloEsa = new File(userTestRepo, "hello-esa-plugin/1.0/hello-esa-plugin-1.0.esa")
	static File extensionsDir = new File(buildDir, "build/wlp/etc/extensions/testExt.properties");

	
	//User feature will be installed to "testExt" extension dir
	static File extensionsInstallDir = new File(buildDir, "build/wlp/usr/cik/extensions/testExt");
	
	private static final String MIN_USER_FEATURE_VERSION = "21.0.0.11";
	
	public static boolean checkOpenLibertyVersion() {

		String runtimeVersion = System.getProperty("runtimeVersion");

		if (VersionUtility.compareArtifactVersion(runtimeVersion, MIN_USER_FEATURE_VERSION, true) >= 0) {
			return true
		}
		return false
	}
	

    @Before
    public void setup() {
		org.junit.Assume.assumeTrue(checkOpenLibertyVersion());
        createDir(buildDir)
        copyBuildFiles(buildFilename, buildDir)
        copySettingsFile(resourceDir, buildDir)
		copyFile(resourceHelloBom, helloBom)
		copyFile(resourceHelloEsa, helloEsa)
		copyFile(resourceExtProp, extensionsDir)
    }

    @Test
    public void test_usrFeatureExt() {
        try {
			def file = new File(extensionsInstallDir, "lib/features/com.ibm.ws.install.helloWorld1.mf")
			
			//installFeature will call prepareFeature when featuresBom is specified.
            runTasks(buildDir, 'installFeature')
			
			assert file.exists() : "com.ibm.ws.install.helloWorld1.mf is not installed"
			assert file.canRead() : "com.ibm.ws.install.helloWorld1.mf cannot be read"
			
            
        } catch (Exception e) {
            throw new AssertionError ("Fail to install user feature.", e)
        }
    }
	
	
	@AfterClass
	public static void cleanUp() {
		deleteDir(userTestRepo)
	}
	
}
