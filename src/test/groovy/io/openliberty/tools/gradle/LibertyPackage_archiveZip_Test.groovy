/*
 * (C) Copyright IBM Corporation 2017.
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

import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LibertyPackage_archiveZip_Test extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/liberty-package-test")
    static File buildDir = new File(integTestDir, "/liberty-package-archiveZip-test")
    static File buildFilename = new File(resourceDir, "liberty-package-archiveZip.gradle")

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        copyBuildFiles(buildFilename, buildDir)
        copySettingsFile(resourceDir, buildDir)
        try {
            runTasks(buildDir, 'installLiberty', 'libertyStart', 'libertyStop')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task installLiberty. "+ e)
        }
    }

    @Test
    public void test_package_archiveZip() {
        try{
           runTasks(buildDir, 'libertyPackage')

           def file = new File(buildDir, 'testPackage.zip')

           assert file.exists() : "file not found"
           assert file.canRead() : "file cannot be read"

           // test package contents
           try {
               ZipFile fileToProcess = new ZipFile(file.getAbsoluteFile())

               Enumeration<? extends ZipEntry> entries = fileToProcess.entries()
               while (entries.hasMoreElements()) {
                   ZipEntry entry = entries.nextElement()
                   def entryName = entry.getName()
                
                   if (entry.isDirectory()) {
                       assert entryName.startsWith("myServerRoot") : "Zip file server root is not correct."
                       break
                   }
               }
           } catch (Exception e) {
               throw new AssertionError ("Unexpected exception when checking the zip server root folder. "+e)
           }

        } catch (Exception e) {
           throw new AssertionError ("Fail on task libertyPackage. "+e)
        }
    }
}
