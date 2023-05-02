/*
 * (C) Copyright IBM Corporation 2015, 2017.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
            throw new AssertionError ("Fail on task installFeature. "+e)
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
            throw new AssertionError ("Fail on task uninstallFeature. "+e)
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
