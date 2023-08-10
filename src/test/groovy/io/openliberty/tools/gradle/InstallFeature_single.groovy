package io.openliberty.tools.gradle

import static org.junit.Assert.*

import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class InstallFeature_single extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/liberty-test")
    static File buildDir = new File(integTestDir, "/InstallFeature_single")
    static File buildFilename = new File(resourceDir, "install_feature_single.gradle")
	static File mavenLocalRepo = new File(System.getProperty("user.home")+ "/.m2/repository")
	static File resourceSimpleEsa = new File(new File("build/resources/test/prepare-feature-test"), "SimpleActivatorESA-1.0.esa")
	static File simpleEsa = new File(mavenLocalRepo, "test/user/test/osgi/SimpleActivatorESA/1.0/SimpleActivatorESA-1.0.esa")

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        copySettingsFile(resourceDir, buildDir)
    }

    @Test
    public void test_installFeature_single() {
        try {
			copyBuildFiles(buildFilename, buildDir)
            def file = new File(buildDir, "build/wlp/lib/features/com.ibm.websphere.appserver.mongodb-2.0.mf")
            runTasks(buildDir, 'installFeature')

            assert file.exists() : "com.ibm.websphere.appserver.mongodb-2.0.mf is not installed"
            assert file.canRead() : "com.ibm.websphere.appserver.mongodb-2.0.mf cannot be read"
        } catch (Exception e) {
            throw new AssertionError ("Fail on task installFeature.", e)
        }
    }

    @Test
    public void test_uninstallFeature_single() {
        try {
			copyBuildFiles(buildFilename, buildDir)
            def file = new File(buildDir, "build/wlp/lib/features/com.ibm.websphere.appserver.mongodb-2.0.mf")
            runTasks(buildDir, 'uninstallFeature')

            assert !file.exists() : "com.ibm.websphere.appserver.mongodb-2.0.mf is not uninstalled"
        } catch (Exception e) {
            throw new AssertionError ("Fail on task uninstallFeature.", e)
        }
    }
	
	@Test
	public void test_installUsrFeature_old_wlp() {
		copyBuildFiles(new File(resourceDir, "install_usr_feature_old_wlp.gradle"), buildDir)
		copyFile(resourceSimpleEsa, simpleEsa)
		def simpleFile = new File(buildDir, "build/wlp/usr/extension/lib/features/test.user.test.osgi.SimpleActivator.mf")
		runTasks(buildDir, 'installFeature')
		assert simpleFile.exists() : "test.user.test.osgi.SimpleActivator.mf is not installed"
	}
}
