package net.wasdev.wlp.gradle.plugins.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class CreateServerDefaultXmlTask extends AbstractServerTask {

  String configFilename = "server.xml"

  @OutputFile
  File getServerXmlFile() {
    return new File(getServerDir(project), configFilename)
  }

  @Input
  def xmlFile = """
    <server description="new server">

    <!-- Enable features -->
    <featureManager>
        <feature>jsp-2.3</feature>
    </featureManager>

    <!-- To access this server from a remote client add a host attribute to the following element, e.g. host="*" -->
    <httpEndpoint id="defaultHttpEndpoint"
                  httpPort="9080"
                  httpsPort="9443" />
                  
    <!-- Automatically expand WAR files and EAR files -->
    <applicationManager autoExpand="true"/>

    </server>"""

  @TaskAction
  void createServerConfig() {

    serverXmlFile.write xmlFile
  }
}
