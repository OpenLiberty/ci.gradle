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

import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.jar.JarFile

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LibertyPackage_archiveJarDirExist_Test extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/liberty-package-test")
    static File buildDir = new File(integTestDir, "liberty-package-archiveJarDirExist-test")
    static File buildFilename = new File(resourceDir, "liberty-package-archiveJarDirExist.gradle")

    @BeforeClass
    public static void setup() {
        copyBuildFiles(buildFilename, buildDir)
        try {
            runTasks(buildDir, 'installLiberty', 'libertyStart', 'libertyStop')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task installLiberty. "+ e)
        }
    }

    @Test
    public void test_package_archiveJarDirExist() {
        try{
           runTasks(buildDir, 'libertyPackage')

           def file = new File(buildDir, 'build/liberty-package-archiveJarDirExist-test.jar')

           assert file.exists() : "file not found"
           assert file.canRead() : "file cannot be read"

           // test package contents - non-runnable Jar since the include property was not specified
           try {
              JarFile fileToProcess = new JarFile(file.getAbsoluteFile(), false)
            
              // If a manifest file has an invalid format, an IOException is thrown.
              // Catch the exception so that the rest of the archive can be processed.
              Manifest mf = fileToProcess.getManifest()
              assert mf != null : "Manifest.mf file not found in Jar. Cannot verify packaged Jar is non-runnable."

              Attributes manifestMap = mf.getMainAttributes()
              assert manifestMap != null : "Manifest.mf attributes are null. Cannot verify Main-Class attribute value."

              def value = manifestMap.getValue("Main-Class")
              assert value != null : "Manifest.mf does not contain Main-Class attribute."
              assert value.equals("wlp.lib.extract.SelfExtract") : "Expected Main-Class manifest value for non-runnable jar not found."
           } catch (Exception e) {
                 throw new AssertionError ("Unexpected exception when checking the Jar Manifest for Main-Class attribute. "+e)
           }
        } catch (Exception e) {
           throw new AssertionError ("Fail on task libertyPackage. "+e)
        }
    }
}
