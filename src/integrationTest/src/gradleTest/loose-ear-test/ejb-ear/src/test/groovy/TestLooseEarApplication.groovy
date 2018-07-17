
import org.junit.Test

import java.io.File;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.net.HttpURLConnection;
import java.net.URL;

public class TestLooseEarApplication {
/*
  Expected output to the XML
  <?xml version="1.0" encoding="UTF-8"?>
  <archive>
      <file sourceOnDisk="/Users/jjvilleg/Desktop/ci.gradle/build/testBuilds/test-loose-ear-application/ejb-ear/build/tmp/ear/application.xml" targetInArchive="/META-INF/application.xml"/>
      <archive targetInArchive="/ejb-ejb.jar">
          <dir sourceOnDisk="/Users/jjvilleg/Desktop/ci.gradle/build/testBuilds/test-loose-ear-application/ejb-ejb/build/classes/java/main" targetInArchive="/"/>
          <file sourceOnDisk="/Users/jjvilleg/Desktop/ci.gradle/build/testBuilds/test-loose-ear-application/ejb-ejb/build/tmp/jar/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
      </archive>
      <archive targetInArchive="/ejb-war.war">
          <dir sourceOnDisk="/Users/jjvilleg/Desktop/ci.gradle/build/testBuilds/test-loose-ear-application/ejb-war/build/classes/java/main" targetInArchive="/WEB-INF/classes"/>
          <file sourceOnDisk="/Users/jjvilleg/Desktop/ci.gradle/build/testBuilds/test-loose-ear-application/ejb-war/build/tmp/jar/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
      </archive>
      <file sourceOnDisk="/Users/jjvilleg/Desktop/ci.gradle/build/testBuilds/test-loose-ear-application/ejb-ear/build/tmp/ear/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
  </archive>

*/
    @Test
    public void test_loose_config_file_contents_are_correct(){
      File projectDir = new File(".");

      File on = new File(projectDir, "build/wlp/usr/servers/ejbServer/apps/ejb-ear.ear.xml");
      FileInputStream input = new FileInputStream(on);

      // get input XML Document
      DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance();
      inputBuilderFactory.setIgnoringComments(true);
      inputBuilderFactory.setCoalescing(true);
      inputBuilderFactory.setIgnoringElementContentWhitespace(true);
      inputBuilderFactory.setValidating(false);
      DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder();
      Document inputDoc=inputBuilder.parse(input);

      // parse input XML Document
      XPath xPath = XPathFactory.newInstance().newXPath();
      String expression = "/archive/dir";
      NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
      Assert.assertEquals("Number of <dir/> element ==>", 0, nodes.getLength());

      expression = "/archive/archive";
      nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
      Assert.assertEquals("Number of <archive/> element ==>", 2, nodes.getLength());

      String ejbJar = "/ejb-ejb.jar"
      String ejbWar = "/ejb-war.war"

      Assert.assertTrue(
            ejbWar.equals(nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue()) ||
              ejbJar.equals(nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue()))

      Assert.assertTrue(
            ejbWar.equals(nodes.item(1).getAttributes().getNamedItem("targetInArchive").getNodeValue()) ||
              ejbJar.equals(nodes.item(1).getAttributes().getNamedItem("targetInArchive").getNodeValue()))

      expression = "/archive/file";
      nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
      Assert.assertEquals("Number of <file/> element ==>", 2, nodes.getLength());

    }

    @Test
    public void test_start_with_timeout_success() {
        try {
            URL url = new URL("http://localhost:9080/ejb-war");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            int code = connection.getResponseCode();
            Assert.assertTrue(code == 200);
        } catch (Exception e) {
            throw new AssertionError ("Fail on task libertyStart. "+ e)
        }
    }

}
