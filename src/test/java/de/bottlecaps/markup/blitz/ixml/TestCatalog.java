package de.bottlecaps.markup.blitz.ixml;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

  private final Document doc;
  private final Collection<TestCase> testCases;

  public TestCatalog(File uri) throws Exception {
    InputSource is = new InputSource(new FileInputStream(uri));
    doc = docBuilder.parse(is);

    testCases = new ArrayList<>();
    NodeList elements = doc.getElementsByTagName("*");
    for (int i = 0, length = elements.getLength(); i < length; ++i) {
        Node node = elements.item(i);
        Assertions.assertEquals(Node.ELEMENT_NODE, node.getNodeType());
        Element element = (Element) elements.item(i);
        String localName = node.getLocalName();
        if (namespace.equals(node.getNamespaceURI())
         && ("test-case".equals(localName) || "grammar-test".equals(localName)))
          testCases.add(new TestCase(element, uri.getParentFile()));
    }
  }

  public Collection<TestCase> getTestCases() {
    return testCases;
  }

  private static List<Element> getChildElementsByTagNameNS(Element parent, String namespaceUri, String localName) {
    List<Element> childElements = new ArrayList<>();
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element
       && namespaceUri.equals(child.getNamespaceURI())
       && localName.equals(child.getNodeName()))
        childElements.add((Element) child);
    }
    return childElements;
  }

  public static void main(String[] args) throws Exception {
//    TestCatalog catalog = new TestCatalog("C:/etc/github/GuntherRademacher/ixml/tests/correct/test-catalog.xml");
//    TestCatalog catalog = new TestCatalog(new File("C:/etc/github/GuntherRademacher/ixml/tests/test-catalog.xml"));
    TestCatalog catalog = new TestCatalog(new File("C:/etc/github/GuntherRademacher/markup-blitz/test-catalog2.xml"));
    System.out.println(catalog.getTestCases().size() + " " + " testcases");
    for (TestCase testCase : catalog.getTestCases()) {
      System.out.println(testCase.getName() + " " + (testCase.getGrammar() != null));
    }
  }
}
