/**
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

package net.wasdev.wlp.gradle.plugins.extensions

import org.gradle.api.GradleException
import org.gradle.api.GradleScriptException
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.util.ConfigureUtil

class ServerExtension{

    ServerExtension(Project project){
        configDirectory =         new File(project.projectDir, '/src/main/liberty')

        bootstrapPropertiesFile = findInConfigSourceset("bootstrap.properties", project)
        configFile =              findInConfigSourceset("server.xml", project)
        jvmOptionsFile =          findInConfigSourceset("jvm.options", project)
        serverEnv =               findInConfigSourceset("server.env", project)
    }

    /**
     * This method is configured to only find one particular file within the configuration sourceset.  If it
     * finds multiple, it will throw an error.
     *
     * @param configFile
     * @return
     */
    private File findInConfigSourceset(String configFile, Project project){

        FileTree filtered = project.sourceSets.libertyConfig.allLibertyConfig.matching {
            include configFile
        }

        switch (filtered.files.size()) {
            case 1:
                return filtered.files.getAt(0)
                break
            case { it > 1}:
                throw new GradleException("More than one ${configFile} found in config sourceset".toString())
                break
            default:
                Set<File> srcDirs = project.sourceSets.libertyConfig.allLibertyConfig.getSrcDirs()
                return new File(srcDirs[0], configFile)
                break
        }
    }

    //Server properties
    String name = "defaultServer"
    String outputDir

    String appsDirectory = "apps"
    boolean stripVersion = false
    boolean looseApplication = true

    File configDirectory
    File configFile
    File bootstrapPropertiesFile
    File jvmOptionsFile
    File serverEnv

    Map<String, Object> bootstrapProperties
    List<String> jvmOptions

    List<Object> apps
    List<Object> dropins

    boolean clean = false
    String timeout
    String template

    int verifyAppStartTimeout = 0

    def numberOfClosures = 0

    FeatureExtension features = new FeatureExtension()
    UninstallFeatureExtension uninstallfeatures = new UninstallFeatureExtension()
    CleanExtension cleanDir = new CleanExtension()

    DeployExtension deploy = new DeployExtension()
    UndeployExtension undeploy = new UndeployExtension()

    PackageAndDumpExtension packageLiberty = new PackageAndDumpExtension()
    PackageAndDumpExtension dumpLiberty = new PackageAndDumpExtension()
    PackageAndDumpExtension javaDumpLiberty = new PackageAndDumpExtension()

    def uninstallfeatures(Closure closure) {
        ConfigureUtil.configure(closure, uninstallfeatures)
    }

    def features(Closure closure) {
        ConfigureUtil.configure(closure, features)
    }

    def cleanDir(Closure closure) {
        ConfigureUtil.configure(closure, cleanDir)
    }

    def deploy(Closure closure) {
        if (numberOfClosures > 0){
            deploy.listOfClosures.add(deploy.clone())
            deploy.file = null
        }
        ConfigureUtil.configure(closure, deploy)
        numberOfClosures++
    }

    def undeploy(Closure closure) {
        ConfigureUtil.configure(closure, undeploy)
    }

    def packageLiberty(Closure closure) {
        ConfigureUtil.configure(closure, packageLiberty)
    }

    def dumpLiberty(Closure closure) {
        ConfigureUtil.configure(closure, dumpLiberty)
    }

    def javaDumpLiberty(Closure closure) {
        ConfigureUtil.configure(closure, javaDumpLiberty)
    }
}
