// Copyright (c) 2023-2024 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.ixml;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Assertions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class TestCatalog {
  public static final String namespace = "https://github.com/invisibleXML/ixml/test-catalog";

  private static final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
  private static final DocumentBuilder docBuilder;
  static {
    try {
      dbFactory.setValidating(false);
      dbFactory.setNamespaceAware(true);
      dbFactory.setFeature("http://xml.org/sax/features/validation", false);
      dbFactory.setFeature("http://xml.org/sax/features/namespaces", true);
      dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
      dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      docBuilder = dbFactory.newDocumentBuilder();
    }
    catch (ParserConfigurationException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private final String path;
  private final Document doc;
  private final Collection<TestCase> testCases;

  public TestCatalog(File folder, String relativePath) {
    this.path = relativePath;
    File file = new File(folder, relativePath);
    InputSource is;
    try {
      is = new InputSource(new FileInputStream(file));
      doc = docBuilder.parse(is);
    }
    catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }

    testCases = new ArrayList<>();
    NodeList elements = doc.getElementsByTagName("*");
    for (int i = 0, length = elements.getLength(); i < length; ++i) {
        Node node = elements.item(i);
        Assertions.assertEquals(Node.ELEMENT_NODE, node.getNodeType());
        Element element = (Element) elements.item(i);
        String localName = node.getLocalName();
        if (namespace.equals(node.getNamespaceURI())
         && ("test-case".equals(localName) || "grammar-test".equals(localName)))
          testCases.add(new TestCase(element, file.getParentFile()));
    }
  }

  public String getPath() {
    return path;
  }

  public Collection<TestCase> getTestCases() {
    return testCases;
  }

}
