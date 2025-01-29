// Copyright (c) 2023-2025 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.xml;

import static java.util.Arrays.stream;

import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import de.bottlecaps.markup.BlitzException;
import de.bottlecaps.markup.blitz.Errors;

public class XmlGrammarInput {
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

  private static final Set<String> elements = Set.of(
      "alt", "alts", "comment", "exclusion", "inclusion", "insertion", "ixml", "literal", "member",
      "nonterminal", "option", "repeat0", "repeat1", "rule", "sep");

  private final Document doc;
  private String indent;

  private XmlGrammarInput(InputSource xml) {
    try {
      // TODO: add schema validation
      doc = docBuilder.parse(xml);
    }
    catch (Exception e) {
      throw new BlitzException("Failed to parse XML gramamr input", e);
    }
  }

  public XmlGrammarInput(InputStream xml) {
    this(new InputSource(xml));
  }

  public XmlGrammarInput(String xml) {
    this(new InputSource(new StringReader(xml)));
  }

  public String toIxml() {
    NodeList elementsByTagName = doc.getElementsByTagName("*");
    for (int i = 0, l = elementsByTagName.getLength(); i < l; ++i) {
      Element element = (Element) elementsByTagName.item(i);
      // verify no namespace
      if (element.getNamespaceURI() != null)
        throw new BlitzException("Unsupported element: Q{" + element.getNamespaceURI() + "}" + element.getLocalName());
      if (! elements.contains(element.getLocalName()))
        throw new BlitzException("Unsupported element: " + element.getLocalName());
      NodeList childNodes = element.getChildNodes();
      if (! "comment".equals(element.getLocalName())) {
        for (int j = 0, m = childNodes.getLength(); j < m; ++j) {
          Node childNode = childNodes.item(j);
          switch (childNode.getNodeType()) {
          case Node.ELEMENT_NODE:
          case Node.COMMENT_NODE:
          case Node.PROCESSING_INSTRUCTION_NODE:
            break;
          default:
            String textContent = childNode.getTextContent();
            if (! textContent.isBlank())
              throw new BlitzException("Unsupported content in element " + element.getLocalName() + ": " + textContent);
          }
        }
      }
    }

    int maxLength = 1 + stream(childElements(doc.getDocumentElement()))
      .mapToInt(e -> e.getAttribute("name").length())
      .max()
      .orElseThrow(() -> new BlitzException("No rules found in ixml grammar"));
    indent = IntStream.range(0, maxLength).mapToObj(i -> " ").collect(Collectors.joining());
    return process(doc.getDocumentElement());
  }

  private String process(Element element) {
    List<String> strings = new ArrayList<>();
    char repetition = '+';
    String charsetPrefix = "";
    switch (element.getLocalName()) {
    case "ixml":
      for (Element child : childElements(element))
        strings.add(process(child));
      return String.join("\n", strings);
    case "rule":
      String lhs = element.getAttribute("mark") + element.getAttribute("name");
      for (Element child : childElements(element))
        strings.add(process(child));
      return indent.substring(lhs.length()) + lhs + ": "
          + String.join(";\n" + indent + "  ", strings)
          + ".";
    case "alt":
      for (Element child : childElements(element))
        strings.add(process(child));
      return String.join(", ", strings);
    case "alts":
      for (Element child : childElements(element))
        strings.add(process(child));
      return "(" + String.join(", ", strings) + ")";
    case "nonterminal":
      return element.getAttribute("mark") + element.getAttribute("name");
    case "literal":
      if (! element.getAttribute("hex").isEmpty()) {
        if (! element.getAttribute("hex").matches("^[0-9A-Fa-f]+$"))
          Errors.S06.thro(element.getAttribute("hex"));
        return "#" + element.getAttribute("hex");
      }
      return element.getAttribute("tmark") + "'" + element.getAttribute("string").replace("'", "''") + "'";
    case "option":
      return process(singletonChildElement(element)) + "?";
    case "repeat0":
      repetition = '*';
      // fall through
    case "repeat1":
      Element[] childElements = childElements(element);
      if (childElements.length == 1)
        return process(childElements[0]) + repetition;
      if (childElements.length == 2)
        return process(childElements[0]) + repetition + repetition + process(childElements[1]);
      throw invalidChildElementsException(element, childElements);
    case "sep":
      return process(singletonChildElement(element));
    case "exclusion":
      charsetPrefix = "~";
      // fall through
    case "inclusion":
      for (Element child : childElements(element))
        strings.add(process(child));
      return element.getAttribute("tmark")
           + charsetPrefix
           + "[" + String.join("; ", strings) + "]";
    case "member":
      if (! element.getAttribute("hex").isEmpty()) {
        if (! element.getAttribute("hex").matches("^[0-9A-Fa-f]+$"))
          Errors.S06.thro(element.getAttribute("hex"));
        return "#" + element.getAttribute("hex");
      }
      String code = element.getAttribute("code");
      if (! code.isEmpty())
        return code;
      String from = element.getAttribute("from");
      String to = element.getAttribute("to");
      if (! from.isEmpty() && ! to.isEmpty()) {
        if (from.startsWith("#") && ! from.matches("^[0-9A-Fa-f]+$"))
          Errors.S06.thro(from.substring(1));
        if (to.startsWith("#") && ! to.matches("^[0-9A-Fa-f]+$"))
          Errors.S06.thro(to.substring(1));
        return (from.startsWith("#") ? from : "'" + from.replace("'", "''") + "'")
             + "-"
             + (to.startsWith("#") ? to : "'" + to.replace("'", "''") + "'");
      }
      return "'" + element.getAttribute("string").replace("'", "''") + "'";
    case "insertion":
      if (! element.getAttribute("hex").isEmpty()) {
        if (! element.getAttribute("hex").matches("^[0-9A-Fa-f]+$"))
          Errors.S06.thro(element.getAttribute("hex"));
        return "+#" + element.getAttribute("hex");
      }
      return "+'" + element.getAttribute("string").replace("'", "''") + "'";
    default:
      throw new BlitzException("Unsupported element: " + element.getLocalName());
    }
  }

  public static Element singletonChildElement(Element element) {
    Element[] childElements = childElements(element);
    if (childElements.length != 1)
      throw invalidChildElementsException(element, childElements);
    return childElements[0];
  }

  public static BlitzException invalidChildElementsException(Element element, Element[] childElements) {
    return new BlitzException("Invalid child elements of " + element.getLocalName() + ": " + stream(childElements).map(Element::getLocalName).collect(Collectors.joining(", ")));
  }

  public static Element[] childElements(Element element) {
    NodeList childNodes = element.getChildNodes();
    List<Element> childElements = new ArrayList<>();
    for (int i = 0, l = childNodes.getLength(); i < l; ++i) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeType() == Node.ELEMENT_NODE
       && ! "comment".equals(childNode.getLocalName()))
        childElements.add((Element) childNode);
    }
    return childElements.toArray(Element[]::new);
  }

}
