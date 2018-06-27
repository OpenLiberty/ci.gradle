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

import static junit.framework.Assert.assertFalse
import static junit.framework.Assert.assertTrue
import static org.junit.Assert.*

import org.junit.After
import org.junit.BeforeClass
import org.junit.Test

import net.wasdev.wlp.common.plugins.util.InstallFeatureUtil

class OpenLibertyInstallFeatureTest extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/openliberty-install-feature-test")
    static File buildDir = new File(integTestDir, "/openliberty-install-feature-test")
    static String buildFilename = "build.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
        try {
            runTasks(buildDir, "installLiberty", "overwriteServer", "libertyPackage")
            deleteDir(new File(buildDir, "build/wlp"));
        } catch (Exception e) {
            throw new AssertionError("Failed to package Open Liberty kernel.", e)
        }
    }
    
    @After
    public void tearDown() {
        deleteDir(new File(buildDir, "build/wlp"));
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
        assertInstalled("a-1.0")
    }
    
    @Test
    /**
     * Install with more dependencies
     */
    public void testInstallFeaturesDependenciesInstallMore() {
        copyBuildFiles(new File(resourceDir, "install_features_dependencies.gradle"), buildDir)
        runTasks(buildDir, 'installFeature')
        assertInstalled("a-1.0")
        
        copyBuildFiles(new File(resourceDir, "install_features_dependencies_install_more.gradle"), buildDir)
        runTasks(buildDir, 'installFeature')
        assertInstalled("a-1.0")
        assertInstalled("c-1.0")
        assertInstalled("d-1.0")
    }
    
    @Test
    /**
     * Install with dependencies
     */
    public void testInstallFeaturesDependencies() {
        copyBuildFiles(new File(resourceDir, "install_features_dependencies.gradle"), buildDir)
        runTasks(buildDir, 'installFeature')
        assertInstalled("a-1.0")
        
        // sanity check
        assertFalse("Feature b-1.0 should not have been installed", getFeatureInfo().contains("b-1.0"));
    }
    
    @Test
    /**
     * Install with dependencies and an empty server.xml
     */
    public void testInstallFeaturesDependenciesEmptyServer() {
        copyBuildFiles(new File(resourceDir, "install_features_dependencies.gradle"), buildDir)
        runTasks(buildDir, "libertyCreate")
        copyServer("server_empty.xml")
        runTasks(buildDir, 'installFeature')
        assertInstalled("a-1.0")
        
        // sanity check
        assertFalse("Feature b-1.0 should not have been installed", getFeatureInfo().contains("b-1.0"));
    }
    
    @Test
    /**
     * Install with dependencies and plugin listed features, and an empty server.xml
     */
    public void testInstallFeaturesDependenciesPluginListEmptyServer() {
        copyBuildFiles(new File(resourceDir, "install_features_dependencies_pluginlist.gradle"), buildDir)
        runTasks(buildDir, "libertyCreate")
        copyServer("server_empty.xml")
        runTasks(buildDir, 'installFeature')
        assertInstalled("a-1.0")
        assertInstalled("b-1.0")
    }
    
    @Test
    /**
     * Install with dependencies and plugin listed features, then again with more plugin listed features
     */
    public void testInstallFeaturesDependenciesPluginListInstallMore() {
        copyBuildFiles(new File(resourceDir, "install_features_dependencies_pluginlist.gradle"), buildDir)
        runTasks(buildDir, 'installFeature')
        assertInstalled("a-1.0")
        assertInstalled("b-1.0")
        
        copyBuildFiles(new File(resourceDir, "install_features_dependencies_pluginlist_install_more.gradle"), buildDir)
        runTasks(buildDir, 'installFeature')
        assertInstalled("a-1.0")
        assertInstalled("b-1.0")
        assertInstalled("c-1.0")
        assertInstalled("d-1.0")
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
        assertInstalled("a-1.0")
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
        assertInstalled("a-1.0")
        assertInstalled("b-1.0")
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
        assertInstalled("a-1.0")
        assertInstalled("b-1.0")
        assertInstalled("c-1.0")
    }
    
    @Test
    /**
     * Install with only plugin listed features
     */
    public void testInstallFeaturesPluginList() {
        copyBuildFiles(new File(resourceDir, "install_features_pluginlist.gradle"), buildDir)
        runTasks(buildDir, 'installFeature')
        assertInstalled("b-1.0")
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
        assertInstalled("a-1.0")
    }
    
    private copyServer(String serverFile) {
        copyFile(new File(resourceDir, serverFile), new File(buildDir, "build/wlp/usr/servers/dummy/server.xml"))
    }

    private void assertInstalled(String feature) throws Exception {
        assertTrue("Feature " + feature + " was not installed into the lib/features directory", existsInFeaturesDirectory(feature));
        String featureInfo = getFeatureInfo();
        assertTrue("Feature " + feature + " was not installed according to productInfo featureInfo: " + featureInfo, featureInfo.contains(feature));
    }
    
    private boolean existsInFeaturesDirectory(String feature) {
        File[] features;
        File featuresDir = new File(buildDir, "build/wlp/lib/features")

        features = featuresDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith("." + feature + ".mf");
                    }
                });

        return features.size() >= 1;
    }
    
    private String getFeatureInfo() throws Exception {
        File installDirectory = new File(buildDir, "build/wlp")
        return InstallFeatureUtil.productInfo(installDirectory, "featureInfo");
    }

}
