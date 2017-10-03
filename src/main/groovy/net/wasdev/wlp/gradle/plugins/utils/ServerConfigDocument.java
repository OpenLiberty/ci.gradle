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
package net.wasdev.wlp.gradle.plugins.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class ServerConfigDocument {

    private DocumentBuilder docBuilder;

    private File configDirectory;
    private File serverFile;

    private Set<String> locations;
    private Set<String> names;
    private Set<String> namelessLocations;
    private Properties props;

    public Set<String> getLocations() {
        return locations;
    }

    public Set<String> getNames() {
        return names;
    }

    public Set<String> getNamelessLocations() {
        return namelessLocations;
    }

    public Properties getProperties() {
        return props;
    }

    private File getServerFile() {
        return serverFile;
    }

    public ServerConfigDocument(File serverXML, File configDir, File bootstrapFile,
    		Map<String, String> bootstrapProp, File serverEnvFile) {
        initializeAppsLocation(serverXML, configDir, bootstrapFile, bootstrapProp, serverEnvFile);
    }

    private DocumentBuilder getDocumentBuilder() throws Exception {
        if (docBuilder == null) {
            // get input XML Document
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setIgnoringComments(true);
            docBuilderFactory.setCoalescing(true);
            docBuilderFactory.setIgnoringElementContentWhitespace(true);
            docBuilderFactory.setValidating(false);
            docBuilder = docBuilderFactory.newDocumentBuilder();
        }
        return docBuilder;
    }

    private void initializeAppsLocation(File serverXML, File configDir, File bootstrapFile,
            Map<String, String> bootstrapProp, File serverEnvFile) {
        try {
            serverFile = serverXML;
            configDirectory = configDir;

            locations = new HashSet<String>();
            names = new HashSet<String>();
            namelessLocations = new HashSet<String>();
            props = new Properties();

            Document doc = parseDocument(new FileInputStream(serverFile));

            // Server variable precedence in ascending order if defined in multiple locations.
            //
            // 1. variables from 'server.env'
            // 2. variables from 'bootstrap.properties'
            // 3. variables defined in <include/> files
            // 4. variables from configDropins/defaults/<file_name>
            // 5. variables defined in server.xml
            //    e.g. <variable name="myVarName" value="myVarValue" />
            // 6. variables from configDropins/overrides/<file_name>

            Properties fProps;
            // get variables from server.env
            File cfgDirFile = getFileFromConfigDirectory("server.env");

            if (cfgDirFile != null) {
                fProps = parseProperties(new FileInputStream(cfgDirFile));
                props.putAll(fProps);
            } else if (serverEnvFile.exists()) {
                fProps = parseProperties(new FileInputStream(serverEnvFile));
                props.putAll(fProps);
            }

            cfgDirFile = getFileFromConfigDirectory("bootstrap.properties");

            if (cfgDirFile != null) {
                fProps = parseProperties(new FileInputStream(cfgDirFile));
                props.putAll(fProps);
            } else if (bootstrapProp != null && !bootstrapProp.isEmpty()) {
                props.putAll(bootstrapProp);
            } else if (bootstrapFile.exists()) {
                fProps = parseProperties(new FileInputStream(bootstrapFile));
                props.putAll(fProps);
            }

            parseIncludeVariables(doc);
            parseConfigDropinsDirVariables("defaults");
            parseVariables(doc);
            parseConfigDropinsDirVariables("overrides");

            parseApplication(doc, "/server/application | /server/webApplication | /server/enterpriseApplication");
            parseNames(doc, "/server/application | /server/webApplication | /server/enterpriseApplication");
            parseInclude(doc);
            parseConfigDropinsDir();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseApplication(Document doc, String expression) throws Exception {
        // parse input document
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); i++) {
            String nodeValue = nodeList.item(i).getAttributes().getNamedItem("location").getNodeValue();

            // add unique values only
            if (!nodeValue.isEmpty()) {
                String resolved = getResolvedVariable(nodeValue);
                if (!locations.contains(resolved)) {
                    locations.add(resolved);
                }
            }
        }
    }

    private void parseInclude(Document doc) throws Exception {
        // parse include document in source server xml
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xPath.compile("/server/include").evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); i++) {
            String nodeValue = nodeList.item(i).getAttributes().getNamedItem("location").getNodeValue();

            if (!nodeValue.isEmpty()) {
                Document docIncl = getIncludeDoc(nodeValue);

                if (docIncl != null) {
                    parseApplication(docIncl, "/server/application | /server/webApplication | /server/enterpriseApplication");
                    // handle nested include elements
                    parseInclude(docIncl);
                }
            }
        }
    }

    //Checks for application names in the document. Will add locations without names to a Set
    private void parseNames(Document doc, String expression) throws Exception {
        // parse input document
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getAttributes().getNamedItem("name") != null) {
                String nodeValue = nodeList.item(i).getAttributes().getNamedItem("name").getNodeValue();

                // add unique values only
                if (!nodeValue.isEmpty()) {
                    String resolved = getResolvedVariable(nodeValue);
                    if (!names.contains(resolved)) {
                        names.add(resolved);
                    }
                }
            }
            else {
                String nodeValue = nodeList.item(i).getAttributes().getNamedItem("location").getNodeValue();

                // add unique values only
                if (!nodeValue.isEmpty()) {
                    String resolved = getResolvedVariable(nodeValue);
                    if (!namelessLocations.contains(resolved)) {
                        namelessLocations.add(resolved);
                    }
                }
            }
        }
    }

    private void parseConfigDropinsDir() throws Exception {
        File configDropins = null;

        // if configDirectory exists and contains configDropins directory,
        // its configDropins has higher precedence.
        if (configDirectory != null && configDirectory.exists()) {
            configDropins = new File(configDirectory, "configDropins");
        }

        if (configDropins == null || !configDropins.exists()) {
            configDropins = new File(getServerFile().getParent(), "configDropins");
        }

        if (configDropins != null && configDropins.exists()) {
            File overrides = new File(configDropins, "overrides");

            if (overrides.exists()) {
                File[] cfgFiles = overrides.listFiles();

                for (int i = 0; i < cfgFiles.length; i++) {
                    if (cfgFiles[i].isFile()) {
                        parseDropinsFiles(cfgFiles[i]);
                    }
                }
            }

            File defaults = new File(configDropins, "defaults");
            if (defaults.exists()) {
                File[] cfgFiles = defaults.listFiles();

                for (int i = 0; i < cfgFiles.length; i++) {
                    if (cfgFiles[i].isFile()) {
                        parseDropinsFiles(cfgFiles[i]);
                    }
                }
            }
        }
    }

    private void parseDropinsFiles(File file) throws Exception {
        // get input XML Document

        Document doc = parseDocument(new FileInputStream(file));
        if(doc != null) {
            //We are not calling parseApplication for configDropins server.xml documents configured by the plugin
            //because we do not want to add locations defined here to the list of locations defined in the other server.xml documents.
            if (!file.getName().equals(ApplicationXmlDocument.APP_XML_FILENAME)) {
                parseApplication(doc, "/server/application | /server/webApplication | /server/enterpriseApplication");
            }
            parseInclude(doc);
            parseNames(doc, "/server/application | /server/webApplication | /server/enterpriseApplication");
        }
    }

    private Document getIncludeDoc(String loc) throws Exception {

        Document doc = null;
        File locFile = null;

        if (loc.startsWith("http:") || loc.startsWith("https:")) {
            if (isValidURL(loc)) {
                URL url = new URL(loc);
                URLConnection connection = url.openConnection();
                doc = parseDocument(connection.getInputStream());
            }
        }
        else if (loc.startsWith("file:")) {
           if (isValidURL(loc)) {
               locFile = new File(loc);
               if (locFile.exists()) {
                   InputStream inputStream = new FileInputStream(locFile.getCanonicalPath());
                   doc = parseDocument(inputStream);
               }
           }
       }
       else if (loc.startsWith("ftp:")) {
           // TODO handle ftp protocol
       }
       else {
           locFile = new File(loc);

           // check if absolute file
           if (locFile.isAbsolute()) {
               InputStream inputStream = new FileInputStream(locFile.getCanonicalPath());
               doc = parseDocument(inputStream);
           }
           else {
               // check configDirectory first if exists
               if (configDirectory != null && configDirectory.exists()) {
                   locFile = new File(configDirectory, loc);
               }

               if (locFile == null || !locFile.exists()) {
                   locFile = new File(getServerFile().getParentFile(), loc);
               }

               if (locFile != null && locFile.exists()) {
                   InputStream inputStream = new FileInputStream(locFile.getCanonicalPath());
                   doc = parseDocument(inputStream);
               }
           }
        }
        return doc;
    }

    private Document parseDocument(InputStream ins) throws Exception {
        Document doc = null;
        try {
            doc = getDocumentBuilder().parse(ins);
        } catch (Exception e) {
            throw e;
        } finally {
            if (ins != null) {
                ins.close();
            }
        }
        return doc;
    }

    private Properties parseProperties(InputStream ins) throws Exception {
        Properties props = null;
        try {
            props = new Properties();
            props.load(ins);
        } catch (Exception e) {
            throw e;
        } finally {
            if (ins != null) {
                ins.close();
            }
        }
        return props;
    }

    private boolean isValidURL(String url) {
        try {
            URL testURL = new URL(url);
            testURL.toURI();
            return true;
        }
        catch (Exception exception) {
            return false;
        }
    }

    private String getResolvedVariable(String nodeValue) {
        final String VARIABLE_NAME_PATTERN = "\\$\\{(.*?)\\}";

        String resolved = nodeValue;
        Pattern varNamePattern = Pattern.compile(VARIABLE_NAME_PATTERN);
        Matcher varNameMatcher = varNamePattern.matcher(nodeValue);

        while (varNameMatcher.find()) {
            String variable = getProperties().getProperty(varNameMatcher.group(1));

            if (variable != null && !variable.isEmpty()) {
                resolved = resolved.replaceAll("\\$\\{" +  varNameMatcher.group(1) + "\\}", variable);
            }
        }
        return resolved;
    }

    private void parseVariables(Document doc) throws Exception {
        // parse input document
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xPath.compile("/server/variable").evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); i++) {
            String varName = nodeList.item(i).getAttributes().getNamedItem("name").getNodeValue();
            String varValue = nodeList.item(i).getAttributes().getNamedItem("value").getNodeValue();

            // add unique values only
            if (!varName.isEmpty() && !varValue.isEmpty()) {
                props.put(varName, varValue);
            }
        }
    }

    private void parseIncludeVariables(Document doc) throws Exception {
        // parse include document in source server xml
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xPath.compile("/server/include").evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); i++) {
            String nodeValue = nodeList.item(i).getAttributes().getNamedItem("location").getNodeValue();

            if (!nodeValue.isEmpty()) {
                Document docIncl = getIncludeDoc(nodeValue);

                if (docIncl != null) {
                    parseVariables(docIncl);
                    // handle nested include elements
                    parseIncludeVariables(docIncl);
                }
            }
        }
    }

    private void parseConfigDropinsDirVariables(String inDir) throws Exception {
        File configDropins = null;

        // if configDirectory exists and contains configDropins directory,
        // its configDropins has higher precedence.
        if (configDirectory != null && configDirectory.exists()) {
            configDropins = new File(configDirectory, "configDropins");
        }

        if (configDropins == null || !configDropins.exists()) {
            configDropins = new File(getServerFile().getParent(), "configDropins");
        }

        if (configDropins != null && configDropins.exists()) {
            File dir = new File(configDropins, inDir);

            if (dir.exists()) {
                File[] cfgFiles = dir.listFiles();

                for (int i = 0; i < cfgFiles.length; i++) {
                    if (cfgFiles[i].isFile()) {
                        parseDropinsFilesVariables(cfgFiles[i]);
                    }
                }
            }
        }
    }

    private void parseDropinsFilesVariables(File file) throws Exception {
        // get input XML Document
        Document doc = parseDocument(new FileInputStream(file));

        parseVariables(doc);
        parseIncludeVariables(doc);
    }

    /*
     * Get the file from configDrectory if it exists;
     * otherwise return def only if it exists, or null if not
     */
    private File getFileFromConfigDirectory(String file, File def) {
        File f = new File(configDirectory, file);
        if (configDirectory != null && f.exists()) {
            return f;
        }
        if (def != null && def.exists()) {
            return def;
        }
        return null;
    }

    private File getFileFromConfigDirectory(String file) {
        return getFileFromConfigDirectory(file, null);
    }
}
