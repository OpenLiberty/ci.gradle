
package net.wasdev.wlp.gradle.plugins.extensions

import org.gradle.util.ConfigureUtil

class ServerExtension{
    //Server properties
    String serverName = "defaultServer"
    String outputDir

    File configDirectory
    File configFile = new File("default")
    File bootstrapPropertiesFile = new File("default")
    File jvmOptionsFile = new File("default")
    File serverEnv = new File("default")

    Map<String, String> bootstrapProperties
    List<String> jvmOptions

    boolean clean = false
    String timeout
    String template

    int verifyTimeout = 30
    String applications

    def numberOfClosures = 0

    DeployExtension deploy = new DeployExtension()
    UndeployExtension undeploy = new UndeployExtension()

    PackageAndDumpExtension packageLiberty = new PackageAndDumpExtension()
    PackageAndDumpExtension dumpLiberty = new PackageAndDumpExtension()
    PackageAndDumpExtension javaDumpLiberty = new PackageAndDumpExtension()

    InstallAppsExtension installapps = new InstallAppsExtension()

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

    def installapps(Closure closure) {
        ConfigureUtil.configure(closure, installapps)
    }
}
