package net.wasdev.wlp.gradle.plugins.tasks

import net.wasdev.wlp.gradle.plugins.ILibertyDefinitions
import net.wasdev.wlp.gradle.plugins.utils.ApplicationXmlDocument
import net.wasdev.wlp.gradle.plugins.utils.ServerConfigDocument
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

import java.text.MessageFormat

class InstallAppsAutoConfigureTask extends InstallAppsTask implements ILibertyDefinitions {

  protected ApplicationXmlDocument applicationXml = new ApplicationXmlDocument()

  final String WARN_APP_NOT_DEFINED = """At least one application is not defined in the server configuration but 
    |  the build file indicates it should be installed in the apps folder. Application configuration is 
    |  being added to the target server configuration dropins folder by the plug-in."""
      .stripMargin().stripIndent()

  final String ERR_CONFIG = """The application, {0}, is configured in the server.xml and the plug-in is configured to 
    |  install the application in the dropins folder. A configured application must be installed to the apps folder."""
      .stripMargin().stripIndent()

  @TaskAction
  void autoConfigure() {

    for (File file in appsDir().listFiles()) {
      checkDeployedApps(file, DEPLOY_FOLDER_APPS)
    }

    for (File file in dropinsDir().listFiles()) {
      checkDeployedApps(file, DEPLOY_FOLDER_DROPINS)
    }

    if (applicationXml.hasChildElements()) {
      logger.warn(WARN_APP_NOT_DEFINED)
      applicationXml.writeApplicationXmlDocument(getServerDir(project))
    } else {
      if (ApplicationXmlDocument.getApplicationXmlFile(getServerDir(project)).exists()) {
        ApplicationXmlDocument.getApplicationXmlFile(getServerDir(project)).delete()
      }
    }
  }

  def checkDeployedApps(File filename, String dir){
    String basename = filename.getName().split("\\.")[0]
    validateAppConfig(filename.name, basename, dir)
  }

  protected void validateAppConfig(String fileName, String artifactId, String dir) throws Exception {
    String appsDir = dir
    if (appsDir.equalsIgnoreCase(DEPLOY_FOLDER_APPS) && !isAppConfiguredInSourceServerXml(fileName)) {
      applicationXml.createApplicationElement(fileName, artifactId)
    } else if (appsDir.equalsIgnoreCase(DEPLOY_FOLDER_DROPINS) && isAppConfiguredInSourceServerXml(fileName)) {
      throw new GradleException(MessageFormat.format(ERR_CONFIG, artifactId))
    }
  }

  protected boolean isAppConfiguredInSourceServerXml(String fileName) {
    boolean configured = false
    File serverConfigFile = new File(getServerDir(project), 'server.xml')
    if (serverConfigFile != null && serverConfigFile.exists()) {
      try {
        ServerConfigDocument scd = new ServerConfigDocument(serverConfigFile, server.configDirectory,
            server.bootstrapPropertiesFile, server.bootstrapProperties as Map<String, String>, server.serverEnv)
        if (scd != null && scd.getLocations().contains(fileName)) {
          logger.debug("Application configuration is found in server.xml : " + fileName)
          configured = true
        }
      }
      catch (Exception e) {
        logger.warn(e.getLocalizedMessage())
      }
    }
    return configured
  }
}
