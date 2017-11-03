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
import java.io.FileNotFoundException;
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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ServerConfigDocument {
    private  ServerConfigDocument instance;
    private  DocumentBuilder docBuilder;

    private  File configDirectory;
    private  File serverFile;

    private  Set<String> locations;
    private  Set<String> names;
    private  Set<String> namelessLocations;
    private  Properties props;

    private static final XPathExpression XPATH_SERVER_APPLICATION;
    private static final XPathExpression XPATH_SERVER_WEB_APPLICATION;
    private static final XPathExpression XPATH_SERVER_ENTERPRISE_APPLICATION;
    private static final XPathExpression XPATH_SERVER_INCLUDE;
    private static final XPathExpression XPATH_SERVER_VARIABLE;

    static {
            try {
                XPath xPath = XPathFactory.newInstance().newXPath();
                XPATH_SERVER_APPLICATION = xPath.compile("/server/application");
                XPATH_SERVER_WEB_APPLICATION = xPath.compile("/server/webApplication");
                XPATH_SERVER_ENTERPRISE_APPLICATION = xPath.compile("/server/enterpriseApplication");
                XPATH_SERVER_INCLUDE = xPath.compile("/server/include");
                XPATH_SERVER_VARIABLE = xPath.compile("/server/variable");
            } catch (XPathExpressionException ex) {
                //These XPath expressions should all compile statically.  Compilation failures mean the expressions are not syntactically correct
                throw new RuntimeException(ex);
            }
    }

    public  Set<String> getLocations() {
        return locations;
    }

    public  Set<String> getNames() {
        return names;
    }

    public Set<String> getNamelessLocations() {
        return namelessLocations;
    }

    public  Properties getProperties() {
        return props;
    }

    private  File getServerFile() {
        return serverFile;
    }

    public ServerConfigDocument(File serverXML, File configDir, File bootstrapFile,
    		Map<String, String> bootstrapProp, File serverEnvFile) {
        initializeAppsLocation(serverXML, configDir, bootstrapFile, bootstrapProp, serverEnvFile);
    }

    private  DocumentBuilder getDocumentBuilder() {
        if (docBuilder == null) {
            // get input XML Document
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setIgnoringComments(true);
            docBuilderFactory.setCoalescing(true);
            docBuilderFactory.setIgnoringElementContentWhitespace(true);
            docBuilderFactory.setValidating(false);
            try {
                docBuilder = docBuilderFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                //fail catastrophically if we can't create a document builder
                throw new RuntimeException(e);
}
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

            parseApplication(doc, XPATH_SERVER_APPLICATION);
            parseApplication(doc, XPATH_SERVER_WEB_APPLICATION);
            parseApplication(doc, XPATH_SERVER_ENTERPRISE_APPLICATION);
            parseNames(doc, "/server/application | /server/webApplication | /server/enterpriseApplication");
            parseInclude(doc);
            parseConfigDropinsDir();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private  void parseApplication(Document doc, XPathExpression expression) throws XPathExpressionException {

        NodeList nodeList = (NodeList) expression.evaluate(doc, XPathConstants.NODESET);

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

    private  void parseInclude(Document doc) throws XPathExpressionException, IOException, SAXException {
        // parse include document in source server xml
        NodeList nodeList = (NodeList) XPATH_SERVER_INCLUDE.evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); i++) {
            String nodeValue = nodeList.item(i).getAttributes().getNamedItem("location").getNodeValue();

            if (!nodeValue.isEmpty()) {
                Document docIncl = getIncludeDoc(nodeValue);

                if (docIncl != null) {
                    // handle nested include elements
                    parseApplication(docIncl, XPATH_SERVER_APPLICATION);
                    parseApplication(docIncl, XPATH_SERVER_WEB_APPLICATION);
                    parseApplication(docIncl, XPATH_SERVER_ENTERPRISE_APPLICATION);
                    parseInclude(docIncl);
                }
            }
        }
    }

    //Checks for application names in the document. Will add locations without names to a Set
    private void parseNames(Document doc, String expression) throws XPathExpressionException, IOException, SAXException {
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

     private void parseConfigDropinsDir() throws XPathExpressionException, IOException, SAXException {
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
                parseDropinsFiles(overrides.listFiles());
            }

            File defaults = new File(configDropins, "defaults");
            if (defaults.exists()) {
                parseDropinsFiles(defaults.listFiles());
            }
        }
    }

    private void parseDropinsFiles(File[] files) throws XPathExpressionException, IOException, SAXException {
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                parseDropinsFile(files[i]);
            }
        }
    }

    private Document parseDropinsXMLFile(File file) throws FileNotFoundException, IOException {
        try {

            FileInputStream is = new FileInputStream(file);
            Document d = parseDocument(is);
            is.close();
            return d;
            } catch (SAXException ex) {
                //If the file was not valid XML, assume it was some other non XML file in dropins.
                System.out.println("Dropins file " + file.getAbsolutePath() + " was not parseable as XML");
                return null;
            }
    }

    private void parseDropinsFile(File file) throws IOException, XPathExpressionException, SAXException {
        // get input XML Document
        Document doc = parseDropinsXMLFile(file);
        if (doc != null) {
            parseApplication(doc, XPATH_SERVER_APPLICATION);
            parseApplication(doc, XPATH_SERVER_WEB_APPLICATION);
            parseApplication(doc, XPATH_SERVER_ENTERPRISE_APPLICATION);
            parseInclude(doc);
        }
    }

    private Document getIncludeDoc(String loc) throws IOException, SAXException {

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

    private Document parseDocument(InputStream in) throws SAXException, IOException {
         try { //ins will be auto-closed
            InputStream ins = in;
            Document d = getDocumentBuilder().parse(ins);
            ins.close();
            return d;
         }
        catch(Exception e){}
        return null;
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

        Pattern varNamePattern = Pattern.compile(VARIABLE_NAME_PATTERN);
        Matcher varNameMatcher = varNamePattern.matcher(nodeValue);

        while (varNameMatcher.find()) {
            String key = varNameMatcher.group(1);
            String variable = getProperties().getProperty(key);
            java.util.Iterator it = getProperties().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                if(pair.getKey().equals(key)) {
                    return pair.getValue().toString();
                }
            }
        }
        return nodeValue;
    }

    private void parseVariables(Document doc) throws XPathExpressionException {
        // parse input document
        NodeList nodeList = (NodeList) XPATH_SERVER_VARIABLE.evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); i++) {
            String varName = nodeList.item(i).getAttributes().getNamedItem("name").getNodeValue();
            String varValue = nodeList.item(i).getAttributes().getNamedItem("value").getNodeValue();
            // add unique values only
            if (!varName.isEmpty() && !varValue.isEmpty()) {
                props.put(varName, varValue);
            }
        }
    }

    private void parseIncludeVariables(Document doc) throws XPathExpressionException, IOException, SAXException {
        // parse include document in source server xml
        NodeList nodeList = (NodeList) XPATH_SERVER_INCLUDE.evaluate(doc, XPathConstants.NODESET);

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

 private void parseConfigDropinsDirVariables(String inDir) throws XPathExpressionException, SAXException, IOException {
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

    private void parseDropinsFilesVariables(File file) throws SAXException, IOException, XPathExpressionException {
        // get input XML Document
        Document doc = parseDropinsXMLFile(file);
        if (doc != null) {
	        parseVariables(doc);
	        parseIncludeVariables(doc);
        }
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
