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

class LibertyApplicationConfigurationTest extends AbstractIntegrationTest {
    static File resourceDir = new File("build/resources/test/app-configuration-test")
    static File buildDir = new File(integTestDir, "/LibertyApplicationConfigurationTest")
    static String buildFilename = "appConfigurationTest.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
        try {
            runTasks(buildDir, 'deploy')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy.", e)
        }
    }

    @Test
    public void checkAppInstalled() {
        assert new File(buildDir, 'build/wlp/usr/servers/defaultServer/apps/test-war.war').exists()
    }
}