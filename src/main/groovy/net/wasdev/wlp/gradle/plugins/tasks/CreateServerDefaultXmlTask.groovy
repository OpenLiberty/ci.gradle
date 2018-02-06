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

  @TaskAction
  void createServerConfig() {

    serverXmlFile.write """
    <server description="Default Liberty server">

    <featureManager>
    </featureManager>

    <httpEndpoint httpPort="9081" httpsPort="9444" id="defaultHttpEndpoint">
    <tcpOptions soReuseAddr="true"/>
    </httpEndpoint>
    
    </server>"""
  }
}
