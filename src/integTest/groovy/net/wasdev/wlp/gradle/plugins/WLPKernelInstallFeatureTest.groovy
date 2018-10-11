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

import org.junit.BeforeClass
import org.junit.Test

class WLPKernelInstallFeatureTest extends KernelInstallFeatureTest{

    @BeforeClass
    @Override
    public static void setup() {
        buildDir = new File(integTestDir, "/wlp-kernel-install-feature-test")
        buildFilename = "build_wlp.gradle"
        super.setup();
    }

    @Test
    /**
     * Install WLP features with accept license
     */
    public void testInstallWLPFeaturesAcceptLicense() {
        copyBuildFiles(new File(resourceDir, "install_wlp_features_accept_license.gradle"), buildDir)
        runTasks(buildDir, 'installFeature')
        assertInstalled("appSecurityClient-1.0")
        assertNotInstalled("beanValidation-2.0")
        assertNotInstalled("couchdb-1.0")
        assertNotInstalled("distributedMap-1.0")
        assertInstalled("servlet-3.0")
    }

}
