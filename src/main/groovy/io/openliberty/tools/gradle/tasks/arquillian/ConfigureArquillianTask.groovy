/**
 * (C) Copyright IBM Corporation 2017, 2021.
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

package io.openliberty.tools.gradle.tasks.arquillian;

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException
import javax.xml.xpath.XPathExpressionException
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.logging.LogLevel
import org.xml.sax.SAXException
import io.openliberty.tools.common.arquillian.objects.LibertyProperty;
import io.openliberty.tools.common.arquillian.objects.LibertyRemoteObject
import io.openliberty.tools.common.arquillian.objects.LibertyManagedObject
import io.openliberty.tools.common.arquillian.util.ArquillianConfigurationException
import io.openliberty.tools.common.arquillian.util.ArtifactCoordinates;
import io.openliberty.tools.common.arquillian.util.Constants
import io.openliberty.tools.common.arquillian.util.HttpPortUtil
import io.openliberty.tools.gradle.tasks.AbstractServerTask

class ConfigureArquillianTask extends AbstractServerTask {

    public TypeProperty type = TypeProperty.NOTFOUND
    public enum TypeProperty {
        MANAGED, REMOTE, NOTFOUND;
    }

    ConfigureArquillianTask() {
        configure({
            description "Automatically generates arquillian.xml for projects that use Arquillian Liberty Managed or Remote containers."
            group 'Liberty'
        })
    }

    public boolean skipIfArquillianXmlExists = false;

    public Map<String, String> arquillianProperties = null;

    @Input
    public boolean getSkipIfArquillianXmlExists() {
        return skipIfArquillianXmlExists
    }

    @Input
    @Optional
    public Map<String, String> getArquillianProperties() {
        return arquillianProperties
    }

    @TaskAction
    void doExecute() throws GradleException {
        File arquillianXml = new File(project.getBuildDir(), "resources/test/arquillian.xml");
        try {
            if (project.configurations.getByName('testCompile') != null) {
                project.configurations.testCompile.find {
                    for(ArtifactCoordinates coors : Constants.ARQUILLIAN_REMOTE_DEPENDENCY) {
                        String artifactId = coors.getArtifactId();
                        if (it.toString().contains(artifactId)) {
                            type = TypeProperty.REMOTE;
                            logger.info("Automatically detected the Arquillian Liberty Remote container artifact: " + artifactId + ".")
                            return true;
                        }
                    }
                    for(ArtifactCoordinates coors : Constants.ARQUILLIAN_MANAGED_DEPENDENCY) {
                        String artifactId = coors.getArtifactId();
                        if (it.toString().contains(artifactId)) {
                            type = TypeProperty.MANAGED
                            logger.info("Automatically detected the Arquillian Liberty Managed container artifact: " + artifactId + ".")
                            return true;
                        }
                    }
                    return false;
                }
            }
        } catch (UnknownConfigurationException uce) {
            logger.debug("No testCompile configuration detected when executing the configureArquillian task.")
        }

        if(type == TypeProperty.NOTFOUND) {
            logger.warn("Arquillian Liberty Managed and Remote dependencies were not found. Defaulting to use the Liberty Managed container.");
            type = TypeProperty.MANAGED;
        }

        if (skipIfArquillianXmlExists && arquillianXml.exists()) {
            logger.info("Skipping configure-arquillian task because arquillian.xml already exists in \"build/resources/test\".");
        } else {
            switch (type) {
                case TypeProperty.REMOTE:
                    configureArquillianRemote(arquillianXml);
                    break;
                default:
                    configureArquillianManaged(arquillianXml);
            }
        }
    }

    private void configureArquillianManaged(File arquillianXml) throws GradleException {
        try {
            LibertyManagedObject arquillianManaged = new LibertyManagedObject(getInstallDir(project).getCanonicalPath(), server.name,
                    getHttpPort(), LibertyProperty.getArquillianProperties(arquillianProperties, LibertyManagedObject.LibertyManagedProperty.class));
            arquillianManaged.build(arquillianXml);
        } catch (Exception e) {
            throw new GradleException("Error configuring Arquillian.", e);
        }
    }

    private void configureArquillianRemote(File arquillianXml) throws GradleException {
        try {
            LibertyRemoteObject arquillianRemote = new LibertyRemoteObject(LibertyProperty.getArquillianProperties(arquillianProperties, LibertyRemoteObject.LibertyRemoteProperty.class));
            arquillianRemote.build(arquillianXml);
        } catch (Exception e) {
            throw new GradleException("Error configuring Arquillian.", e);
        }
    }

    /**
     * @return the HTTP port that the managed Liberty server is running on.
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws XPathExpressionException
     * @throws FileNotFoundException
     * @throws ArquillianConfigurationException
     */
     private int getHttpPort() throws FileNotFoundException, XPathExpressionException, IOException, ParserConfigurationException, SAXException, ArquillianConfigurationException {
        String serverDirectory = getServerDir(project);
        File serverXML = new File(serverDirectory + "/server.xml");
        File configVariableXML = new File(serverDirectory + "/configDropins/overrides/liberty-plugin-variable-config.xml")
        File bootstrapProperties = new File(serverDirectory + "/bootstrap.properties");
        return HttpPortUtil.getHttpPort(serverXML, bootstrapProperties, configVariableXML);
    }
}
