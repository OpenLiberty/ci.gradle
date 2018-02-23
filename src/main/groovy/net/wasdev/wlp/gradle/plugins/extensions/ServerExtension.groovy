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

import java.nio.file.Paths

class ServerExtension{

    ServerExtension(Project project){
        Set<File> srcDirs = project.sourceSets.libertyConfig.allLibertyConfig.getSrcDirs()

        configDirectory =         srcDirs[0]

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
                return Paths.get(project.projectDir.absolutePath, srcDirs[0].absolutePath, configFile).toFile()
                break
        }
    }

    //Server properties
    String name = "defaultServer"
    String outputDir

    String appsDirectory = "apps"
    boolean stripVersion = false
    boolean looseApplication = true
    boolean autoConfigure = true

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
    String template

    int verifyAppStartTimeout = 30

    def numberOfClosures = 0

    FeatureExtension features = new FeatureExtension()
    UninstallFeatureExtension uninstallfeatures = new UninstallFeatureExtension()
    CleanExtension cleanDir = new CleanExtension()

    private List<DeployExtension> deploys = new ArrayList<DeployExtension>()

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
        DeployExtension deploy = new DeployExtension()
        ConfigureUtil.configure(closure, deploy)
        deploys << deploy
    }

    def getDeploys() {
        return deploys
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
