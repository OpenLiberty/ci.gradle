/*
 * (C) Copyright IBM Corporation 2018.
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
package net.wasdev.wlp.gradle.plugins

import static junit.framework.Assert.assertEquals
import static org.junit.Assert.*

import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import net.wasdev.wlp.common.plugins.util.InstallFeatureUtil

class KernelInstallFeatureTest extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/kernel-install-feature-test")
    static File buildDir = new File(integTestDir, "/kernel-install-feature-test")
    static String buildFilename = "build.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
    }
    
    @Before
    public void before() {
        runTasks(buildDir, "libertyCreate")
        copyServer("server_empty.xml")
        deleteDir(new File(buildDir, "build/wlp/lib/features"))
    }
    
    @Test
    /**
     * Install with identical dependencies
     */
    public void testInstallFeaturesDependenciesAlreadyInstalled() {
        copyBuildFiles(new File(resourceDir, "install_features_dependencies.gradle"), buildDir)
        runTasks(buildDir, 'installFeature')
        copyBuildFiles(new File(resourceDir, "install_features_dependencies.gradle"), buildDir)
        runTasks(buildDir, 'installFeature')
        assertInstalled("appSecurityClient-1.0")
        assertNotInstalled("beanValidation-2.0")
        assertNotInstalled("couchdb-1.0")
        assertNotInstalled("distributedMap-1.0")
    }
    
    @Test
    /**
     * Install with more dependencies
     */
    public void testInstallFeaturesDependenciesInstallMore() {
        copyBuildFiles(new File(resourceDir, "install_features_dependencies.gradle"), buildDir)
        runTasks(buildDir, 'installFeature')
        assertInstalled("appSecurityClient-1.0")
        assertNotInstalled("beanValidation-2.0")
        assertNotInstalled("couchdb-1.0")
        assertNotInstalled("distributedMap-1.0")
        
        copyBuildFiles(new File(resourceDir, "install_features_dependencies_install_more.gradle"), buildDir)
        runTasks(buildDir, 'installFeature')
        assertInstalled("appSecurityClient-1.0")
        assertNotInstalled("beanValidation-2.0")
        assertInstalled("couchdb-1.0")
        assertInstalled("distributedMap-1.0")
    }
    
    @Test
    /**
     * Install with dependencies
     */
    public void testInstallFeaturesDependencies() {
        copyBuildFiles(new File(resourceDir, "install_features_dependencies.gradle"), buildDir)
        runTasks(buildDir, 'installFeature')
        assertInstalled("appSecurityClient-1.0")
        assertNotInstalled("beanValidation-2.0")
        assertNotInstalled("couchdb-1.0")
        assertNotInstalled("distributedMap-1.0")
    }
    
    @Test
    /**
     * Install with dependencies and plugin listed features, then again with more plugin listed features
     */
    public void testInstallFeaturesDependenciesPluginListInstallMore() {
        copyBuildFiles(new File(resourceDir, "install_features_dependencies_pluginlist.gradle"), buildDir)
        runTasks(buildDir, 'installFeature')
        assertInstalled("appSecurityClient-1.0")
        assertInstalled("beanValidation-2.0")
        assertNotInstalled("couchdb-1.0")
        assertNotInstalled("distributedMap-1.0")
        
        copyBuildFiles(new File(resourceDir, "install_features_dependencies_pluginlist_install_more.gradle"), buildDir)
        runTasks(buildDir, 'installFeature')
        assertInstalled("appSecurityClient-1.0")
        assertInstalled("beanValidation-2.0")
        assertInstalled("couchdb-1.0")
        assertInstalled("distributedMap-1.0")
    }
    
    @Test
    /**
     * Install with the same feature in dependencies and server.xml
     */
    public void testInstallFeaturesDependenciesServerIdentical() {
        copyBuildFiles(new File(resourceDir, "install_features_dependencies.gradle"), buildDir)
        runTasks(buildDir, "libertyCreate")
        copyServer("server_a.xml")
        runTasks(buildDir, 'installFeature')
        assertInstalled("appSecurityClient-1.0")
        assertNotInstalled("beanValidation-2.0")
        assertNotInstalled("couchdb-1.0")
        assertNotInstalled("distributedMap-1.0")
    }
    
    @Test
    /**
     * Install with different features in dependencies and server.xml
     */
    public void testInstallFeaturesDependenciesServer() {
        copyBuildFiles(new File(resourceDir, "install_features_dependencies.gradle"), buildDir)
        runTasks(buildDir, "libertyCreate")
        copyServer("server_b.xml")
        runTasks(buildDir, 'installFeature')
        assertInstalled("appSecurityClient-1.0")
        assertInstalled("beanValidation-2.0")
        assertNotInstalled("couchdb-1.0")
        assertNotInstalled("distributedMap-1.0")
    }
    
    @Test
    /**
     * Install with different features in dependencies, server.xml, and plugin listed features
     */
    public void testInstallFeaturesDependenciesServerPluginList() {
        copyBuildFiles(new File(resourceDir, "install_features_dependencies_pluginlist.gradle"), buildDir)
        runTasks(buildDir, "libertyCreate")
        copyServer("server_c.xml")
        runTasks(buildDir, 'installFeature')
        assertInstalled("appSecurityClient-1.0")
        assertInstalled("beanValidation-2.0")
        assertInstalled("couchdb-1.0")
        assertNotInstalled("distributedMap-1.0")
    }
    
    @Test
    /**
     * Install with only plugin listed features
     */
    public void testInstallFeaturesPluginList() {
        copyBuildFiles(new File(resourceDir, "install_features_pluginlist.gradle"), buildDir)
        runTasks(buildDir, 'installFeature')
        assertNotInstalled("appSecurityClient-1.0")
        assertInstalled("beanValidation-2.0")
        assertNotInstalled("couchdb-1.0")
        assertNotInstalled("distributedMap-1.0")
    }
    
    @Test
    /**
     * Install with only server.xml features
     */
    public void testInstallFeaturesServer() {
        copyBuildFiles(new File(resourceDir, "install_features_server.gradle"), buildDir)
        runTasks(buildDir, "libertyCreate")
        copyServer("server_a.xml")
        runTasks(buildDir, "installFeature")
        assertInstalled("appSecurityClient-1.0")
        assertNotInstalled("beanValidation-2.0")
        assertNotInstalled("couchdb-1.0")
        assertNotInstalled("distributedMap-1.0")
    }
    
    @Test
    /**
     * Install with server.xml features specified in lowercase and were already installed
     */
    public void testInstallFeaturesServerAlreadyInstalledLowercase() {
        copyBuildFiles(new File(resourceDir, "install_features_server.gradle"), buildDir)
        runTasks(buildDir, "libertyCreate")
        copyServer("server_lowercase.xml")
        
        runTasks(buildDir, "installFeature")
        assertInstalled("appSecurityClient-1.0")
        assertInstalled("beanValidation-2.0")
        assertNotInstalled("couchdb-1.0")
        assertNotInstalled("distributedMap-1.0")
        
        runTasks(buildDir, "installFeature")
        assertInstalled("appSecurityClient-1.0")
        assertInstalled("beanValidation-2.0")
        assertNotInstalled("couchdb-1.0")
        assertNotInstalled("distributedMap-1.0")
    }
    
    @Test
    /**
     * Install OL features without accept license
     */
    public void testInstallOLFeaturesNoAcceptLicense() {
        copyBuildFiles(new File(resourceDir, "install_ol_features_no_accept_license.gradle"), buildDir)
        runTasks(buildDir, 'installFeature')
        assertInstalled("appSecurityClient-1.0")
        assertInstalled("beanValidation-2.0")
        assertNotInstalled("couchdb-1.0")
        assertNotInstalled("distributedMap-1.0")
        assertNotInstalled("servlet-3.0")
    }
        
    private copyServer(String serverFile) {
        copyFile(new File(resourceDir, serverFile), new File(buildDir, "build/wlp/usr/servers/defaultServer/server.xml"))
    }

    private void assertInstallStatus(String feature, boolean expectation) throws Exception {
        String expectationString = (expectation ? "installed" : "not installed");
        assertEquals("Feature " + feature + " was expected to be " + expectationString + " in the lib/features directory", expectation, existsInFeaturesDirectory(feature));
        String featureInfo = getFeatureInfo();
        assertEquals("Feature " + feature + " was expected to be " + expectationString + " according to productInfo featureInfo: " + featureInfo, expectation, featureInfo.contains(feature));
    }

    protected void assertInstalled(String feature) throws Exception {
        assertInstallStatus(feature, true);
    }
    
    protected void assertNotInstalled(String feature) throws Exception {
        assertInstallStatus(feature, false);
    }
    
    private boolean existsInFeaturesDirectory(String feature) {
        File[] features;
        File featuresDir = new File(buildDir, "build/wlp/lib/features")

        features = featuresDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().equals("com.ibm.websphere.appserver." + feature.toLowerCase() + ".mf");
                    }
                });

        return features.size() >= 1;
    }
    
    private String getFeatureInfo() throws Exception {
        File installDirectory = new File(buildDir, "build/wlp")
        return InstallFeatureUtil.productInfo(installDirectory, "featureInfo");
    }

}
