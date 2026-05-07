/**
 * (C) Copyright IBM Corporation 2026.
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
package io.openliberty.tools.gradle.tasks

import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlNodePrinter
import groovy.xml.XmlParser
import io.openliberty.tools.common.plugins.util.PrepareConfigUtil
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import groovy.xml.MarkupBuilder

class PrepareConfigTask extends AbstractServerTask {

    /**
     * Name of the temporary directory used for mock Liberty server structures.
     * This directory is created under the build output directory (build/).
     * Default value is "liberty-var-cache".
     *
     * Example: If set to "my-temp", the mock server will be created at:
     * build/my-temp/wlp/usr/servers/{serverName}
     */
    @Input
    String prepareConfigTempDir = PrepareConfigUtil.DEFAULT_TEMP_DIR_NAME

    PrepareConfigTask() {
        configure({
            description = 'Prepare Liberty configuration and generate liberty-plugin-config.xml'
            group = 'Liberty'
        })
    }

    @TaskAction
    void prepareConfig() {
        logger.info("Preparing Liberty configuration...")
        
        File buildDir = project.getLayout().getBuildDirectory().getAsFile().get()
        
        // Validate and use the configured temp directory name
        String tempDirName = (prepareConfigTempDir != null && !prepareConfigTempDir.trim().isEmpty())
            ? prepareConfigTempDir.trim()
            : PrepareConfigUtil.DEFAULT_TEMP_DIR_NAME
        
        // Create mock Liberty server structure
        File mockServerDir = PrepareConfigUtil.createMockLibertyServerStructure(buildDir, server.name, tempDirName)
        
        // Create initial liberty-plugin-config.xml file (required by copyConfigFiles)
        createInitialConfigFile(buildDir)
        
        // Temporarily override userDirectory to point to mock location
        File originalUserDir = project.liberty.userDir
        File mockUserDir = PrepareConfigUtil.getMockUserDirectory(buildDir, tempDirName)
        
        try {
            // Set userDir to mock location so getServerDir() returns mock server directory
            project.liberty.userDir = mockUserDir
            
            // Copy config files to mock server (same as libertyCreate)
            copyConfigFiles()
            
        } finally {
            // Restore original userDir
            project.liberty.userDir = originalUserDir
        }
        
        // Add installDirectory if missing
        addInstallDirectoryIfMissing(buildDir, tempDirName)
        
        logger.info("Liberty configuration file generated: ${buildDir}/liberty-plugin-config.xml")
        logger.info("Mock Liberty server structure created: ${mockServerDir.absolutePath}")
    }


    private void createInitialConfigFile(File buildDir) {
        File configFile = new File(buildDir, "liberty-plugin-config.xml")
        configFile.withWriter('UTF-8') { writer ->
            def xmlDoc = new MarkupBuilder(writer)
            xmlDoc.mkp.xmlDeclaration(version: '1.0', encoding: 'UTF-8')
            xmlDoc.'liberty-plugin-config'('version':'2.0') {
                // Empty - will be populated by copyConfigFiles
            }
        }
    }

    private void addInstallDirectoryIfMissing(File buildDir, String tempDirName) {
        File configFile = new File(buildDir, "liberty-plugin-config.xml")
        File mockInstallDir = PrepareConfigUtil.getMockInstallDirectory(buildDir, tempDirName)
        
        def pluginXmlParser = new XmlParser()
        def libertyPluginConfig = pluginXmlParser.parse(configFile)
        
        // Check if installDirectory already exists
        if (libertyPluginConfig.getAt('installDirectory').isEmpty()) {
            // Add installDirectory as first child
            libertyPluginConfig.children().add(0, new Node(null, 'installDirectory', mockInstallDir.toString()))
            
            // Write back to file
            configFile.withWriter('UTF-8') { output ->
                output << new StreamingMarkupBuilder().bind {
                    mkp.xmlDeclaration(encoding: 'UTF-8', version: '1.0' )
                }
                def printer = new XmlNodePrinter(new PrintWriter(output))
                printer.preserveWhitespace = true
                printer.print(libertyPluginConfig)
            }
        }
    }
}
