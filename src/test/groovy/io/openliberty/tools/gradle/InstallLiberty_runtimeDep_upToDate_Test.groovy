/*
 * (C) Copyright IBM Corporation 2021.
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
class InstallLiberty_runtimeDep_upToDate_Test extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/liberty-test")
    static File buildDir = new File(integTestDir, "/InstallLiberty_upToDate_Test")
    static String runTimeDep_UpToDate_buildFilename = "installLiberty_upToDate_runtimeDep_Test.gradle"
    static File wsLaunchJar = new File(buildDir, "build/wlp/lib/ws-launch.jar")

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, runTimeDep_UpToDate_buildFilename, true)
        try {
            runTasks(buildDir, 'installLiberty')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task installLiberty. "+ e)
        }
    }

    @Test
    public void test_installLiberty_upToDate() {
        // same version as installed in the setup method, so upToDate check should return true
        assert runTaskCheckForUpToDate(buildDir, 'installLiberty', "-PlibertyVersion=21.0.0.1")

        // changing the libertyVersion causes the upToDate check to return false
        assertFalse runTaskCheckForUpToDate(buildDir, 'installLiberty', "-PlibertyVersion=21.0.0.2")

        // verify upToDate check returns true for same libertyVersion
        assert runTaskCheckForUpToDate(buildDir, 'installLiberty', "-PlibertyVersion=21.0.0.2")

        // now delete the ws-launch.jar from the installation and ensure the upToDate check returns false even though the libertyVersion did not change
        wsLaunchJar.delete()
        assertFalse runTaskCheckForUpToDate(buildDir, 'installLiberty', "-PlibertyVersion=21.0.0.2")

    }
}
