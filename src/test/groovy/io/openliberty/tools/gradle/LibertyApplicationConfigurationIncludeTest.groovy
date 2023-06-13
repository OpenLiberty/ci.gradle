/*
 * (C) Copyright IBM Corporation 2023.
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
import org.junit.AfterClass
import org.junit.Test
import java.io.File
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.Scanner;
import java.util.regex.Pattern;

// Test that the appLocation variable referenced in server.xml and defined in an include file is used for the location of the 
// application. This requires the ${server.config.dir} variable to get resolved correctly in both the include location and the app location.
class LibertyApplicationConfigurationIncludeTest extends AbstractIntegrationTest {
    static File resourceDir = new File("build/resources/test/app-configuration-include-test")
    static File buildDir = new File(integTestDir, "/LibertyApplicationConfigurationIncludeTest")
    static String buildFilename = "appConfigurationIncludeTest.gradle"
    static String CONFIG_DROPINS_XML="build/wlp/usr/servers/defaultServer/configDropins/defaults/install_apps_configuration_1491924271.xml"
    static String MESSAGES_LOG="build/wlp/usr/servers/defaultServer/logs/messages.log"

    static String INCLUDE_REGEX_MESSAGE = ".* CWWKG0028A: Processing included configuration resource: .*/|\\\\target/|\\\\liberty/|\\\\usr/|\\\\servers/|\\\\defaultServer/|\\\\environment\\.xml";
    static String APP_STARTED_MESSAGE = ".* CWWKZ0001I: Application servlet started.*";

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
        try {
            //Installing the war built by the other gradle project in the src dir
            runTasks(new File(buildDir, 'test-maven-war'), 'publishToMavenLocal')
            //Then installing that war from m2 to the apps directory through the libertyApp configuration
            runTasks(buildDir, 'deploy', 'libertyStart')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy.", e)
        }
    }

    @AfterClass
    public static void cleanup() {
        try {
            runTasks(buildDir, 'libertyStop')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy.", e)
        }
    }

    @Test
    public void checkAppInstalled() {
        assert new File(buildDir, 'build/wlp/usr/servers/defaultServer/apps/test-maven-war.war').exists()
    }


    @Test
    public void testApplicationNotConfiguredInConfigDropins()  {
        assert !(new File(buildDir, CONFIG_DROPINS_XML).exists())
    }

    @Test
    public void checkMessagesLog() {
        File messagesLog = new File(buildDir, MESSAGES_LOG)
        assert messagesLog.exists()
 
        InputStream serverOutput = null
        InputStreamReader in = null
        Scanner s = null

        boolean includeFound = false
        boolean appStartedFound = false

        try {
            // Read file and search
            serverOutput = new FileInputStream(messagesLog)
            in = new InputStreamReader(serverOutput)
            s = new Scanner(in)

            Pattern pattern1 = Pattern.compile(INCLUDE_REGEX_MESSAGE)
            Pattern pattern2 = Pattern.compile(APP_STARTED_MESSAGE)

            while (s.hasNextLine()) {
                String line = s.nextLine()
                if (pattern1.matcher(line).find()) {
                    includeFound = true
                } else if (pattern2.matcher(line).find()) {
                    appStartedFound = true
                }
            }
        } catch (Exception e) {

        }
        s.close();
        serverOutput.close()
        in.close()

        assert includeFound
        assert appStartedFound
    }

}