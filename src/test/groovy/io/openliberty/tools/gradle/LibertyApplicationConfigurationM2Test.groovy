/*
 * (C) Copyright IBM Corporation 2019.
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
import org.junit.Test
import java.io.File

// Test that the appLocation variable in server.xml with a default value is not used for the 
// location of the application when overridden by a bootstrapProperties property in the build.gradle
class LibertyApplicationConfigurationM2Test extends AbstractIntegrationTest {
    static File resourceDir = new File("build/resources/test/app-configuration-m2-test")
    static File buildDir = new File(integTestDir, "/LibertyApplicationConfigurationM2Test")
    static String buildFilename = "appConfigurationM2Test.gradle"
    static String CONFIG_DROPINS_XML="build/wlp/usr/servers/defaultServer/configDropins/defaults/install_apps_configuration_1491924271.xml"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
        try {
            //Installing the war built by the other gradle project in the src dir
            runTasks(new File(buildDir, 'test-maven-war'), 'install')
            //Then installing that war from m2 to the apps directory through the libertyApp configuration
            runTasks(buildDir, 'deploy')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy. "+ e)
        }
    }

    @Test
    public void checkAppInstalled() {
        assert new File(buildDir, 'build/wlp/usr/servers/defaultServer/apps/test-maven-war-1.0-SNAPSHOT.war').exists()
    }

    @Test
    public void testApplicationNotConfiguredInConfigDropins() {
        assert !(new File(buildDir, CONFIG_DROPINS_XML).exists())
    }

}