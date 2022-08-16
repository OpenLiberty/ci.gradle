/*
 * (C) Copyright IBM Corporation 2021, 2022.
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

import org.junit.Before
import org.junit.AfterClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

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
	
	public static boolean deleteFolder(final File directory) {
		if (directory.isDirectory()) {
			File[] files = directory.listFiles();
			if (null != files) {
				for (File file : files) {
					if (file.isDirectory()) {
							deleteFolder(file);
					} else {
						if (!file.delete()) {
								file.deleteOnExit();
						}
					}
				}
			}
		}
		if(!directory.delete()){
			directory.deleteOnExit();
			return false;
		}
		return true;
	}
	
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
            throw new AssertionError ("Fail to install user feature. "+e)
        }
    }
	
	
	@AfterClass
	public static void cleanUp() {
		deleteFolder(userTestRepo)
	}
	
}
