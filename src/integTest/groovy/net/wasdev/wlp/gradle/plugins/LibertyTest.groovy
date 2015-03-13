/*
 * (C) Copyright IBM Corporation 2015.
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

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runners.MethodSorters
import org.junit.FixMethodOrder
import static org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.Date

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LibertyTest extends AbstractIntegrationTest{

    static final String RESOURCES_DIR = "../resources/integrationTest"
    static final String USER_DIR = "../"

    def static date = new Date()
    def static dateFormat = new SimpleDateFormat("dd.MM.yy'_at_'HH.mm")
    def static TIME_STAMP = dateFormat.format(date)
    static final String SERVER_NAME = "myServer_"+TIME_STAMP

    @BeforeClass
    public static void createProjectAndApplyPlugin() {

        buildFile << """
apply plugin: 'liberty'

liberty {
    userDir = '$USER_DIR'
    wlpDir = '$WLP_DIR'
    serverName = '$SERVER_NAME'
} 
apply plugin : 'war'

war{
    destinationDir = new File('$RESOURCES_DIR')
    archiveName = 'test-war.war'
}       
"""
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runTasks(integTestDir, 'libertyStop')
        deleteDir(integTestDir)
    }

    @Test
    public void test1_Start() {
        try {
            runTasks(integTestDir, 'libertyStart')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStart. "+e)
        }
    }

    @Test
    public void test2_executeDeployTask() {
        try {
            runTasks(integTestDir, 'deployWar')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deployWar. "+e)
        }
    }

    @Test
    public void test3_executeUndeployTask() {
        try {
            runTasks(integTestDir, 'undeployWar')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task undeployWar. "+e)
        }
    }

    @Test
    public void test5_Stop() {
        try{
            runTasks(integTestDir, 'libertyStop')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStop. "+e)
        }
    }
}