// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.ixml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import de.bottlecaps.markup.TestBase;
import de.bottlecaps.markup.blitz.codepoints.UnicodeCategory;
import de.bottlecaps.markup.blitz.xml.XmlGrammarInput;

public class TestCase extends TestBase{
  private String name;
  private boolean isGrammarTest;
  private String grammar;
  private boolean isXmlGrammar;
  private Assertion assertion;
  private Set<String> errorCodes;
  private String input;
  private List<String> outputs;
  private String skippedBecause;

  public TestCase(Element element, File folder) {
    isGrammarTest = "grammar-test".equals(element.getLocalName());

    List<String> names = new ArrayList<>();
    for (Node node = element; node != null; node = node.getParentNode()) {
      NamedNodeMap attributes = node.getAttributes();
      if (attributes != null) {
        Node nameAttr = attributes.getNamedItem("name");
        if (nameAttr != null)
          names.add(nameAttr.getTextContent());
      }
    }
    assertFalse(names.isEmpty(), "missing test case name");
    Collections.reverse(names);
    this.name = String.join("/", names.toArray(String[]::new));

    try {
      this.isXmlGrammar = false;
      for (Node node = element; node != null; node = node.getParentNode()) {
        NodeList childNodes = node.getChildNodes();
        if (childNodes != null) {
          for (int i = 0; i < childNodes.getLength(); ++i) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE
             && TestCatalog.namespace.equals(childNode.getNamespaceURI())) {
              String grammar = null;
              switch (childNode.getLocalName()) {
              case "vxml-grammar":
                isXmlGrammar = true;
                // fall through
              case "ixml-grammar":
                grammar = childNode.getTextContent();
                break;
              case "vxml-grammar-ref":
                isXmlGrammar = true;
                // fall through
              case "ixml-grammar-ref":
                Node href = childNode.getAttributes().getNamedItemNS(null, "href");
                assertNotNull(href, "no href attribute in test-string-ref of test case " + name);
                grammar = fileContent(new File(folder, href.getTextContent()));
                break;
              }
              if (grammar != null) {
                assertNull(this.grammar, "more than one geammar for test case " + name);
                this.grammar = grammar;
              }
            }
          }
        }
      }

      assertNotNull(grammar, "Missing geammar for test case " + name);

      outputs = new ArrayList<>();
      NodeList testCaseChildNodes = element.getChildNodes();
      for (int i = 0, testCaseChildNodesLength = testCaseChildNodes.getLength(); i < testCaseChildNodesLength; ++i) {
        Node testCaseChildNode = testCaseChildNodes.item(i);
        if (testCaseChildNode.getNodeType() == Node.ELEMENT_NODE
         && TestCatalog.namespace.equals(testCaseChildNode.getNamespaceURI())) {
          String input = null;
          switch (testCaseChildNode.getLocalName()) {
          case "dependencies":
            Node unicodeVersion = testCaseChildNode.getAttributes().getNamedItemNS(null, "Unicode-version");
            assertNotNull(unicodeVersion, "no Unicode-version attribute in dependencies of test case " + name);
            final String version = unicodeVersion.getTextContent();
            if (! version.equals(UnicodeCategory.version))
              skippedBecause = "Skipped, because test case asks for Unicode version " + version + " while we support " + UnicodeCategory.version;
            break;
          case "test-string":
            input = testCaseChildNode.getTextContent();
            break;
          case "test-string-ref": {
              Node href = testCaseChildNode.getAttributes().getNamedItemNS(null, "href");
              assertNotNull(href, "no href attribute in test-string-ref of test case " + name);
              input = fileContent(new File(folder, href.getTextContent()));
            }
            break;
          case "result":
            NodeList resultChildNodes = testCaseChildNode.getChildNodes();
            for (int j = 0, resultChildNodesLength = resultChildNodes.getLength(); j < resultChildNodesLength; ++j) {
              Node resultChildNode = resultChildNodes.item(j);
              if (resultChildNode.getNodeType() == Node.ELEMENT_NODE
               && TestCatalog.namespace.equals(resultChildNode.getNamespaceURI())) {
                Node errorCodeAttr;
                switch (resultChildNode.getLocalName()) {
                case "assert-xml":
                  if (assertion == null)
                    assertion = Assertion.assert_xml;
                  else
                    assertEquals(Assertion.assert_xml, assertion, "contradicting assertions in test case '" + name);
                  assertion = Assertion.assert_xml;
                  Element xmlContent = XmlGrammarInput.singletonChildElement((Element) resultChildNode);
                  outputs.add(elementToString(xmlContent));
                  break;
                case "assert-xml-ref": {
                    if (assertion == null)
                      assertion = Assertion.assert_xml;
                    else
                      assertEquals(Assertion.assert_xml, assertion, "contradicting assertions");
                    Node href = resultChildNode.getAttributes().getNamedItemNS(null, "href");
                    assertNotNull(href, "no href attribute in assert-xml-ref of test case " + name);
                    outputs.add(fileContent(new File(folder, href.getTextContent())));
                  }
                  break;
                case "assert-not-a-sentence":
                  assertNull(assertion, "more than one assertion in test case " + name);
                  assertion = Assertion.assert_not_a_sentence;
                  break;
                case "assert-dynamic-error":
                  assertNull(assertion, "more than one assertion in test case " + name);
                  assertion = Assertion.assert_dynamic_error;
                  errorCodeAttr = resultChildNode
                      .getAttributes()
                      .getNamedItemNS(null, "error-code");
                  assertNotNull(errorCodeAttr, "missing error-code attribute in assert-dynamic-error in test case " + name);
                  errorCodes = Arrays.stream(errorCodeAttr.getTextContent().split(" "))
                      .collect(Collectors.toSet());
                  break;
                case "assert-not-a-grammar":
                  assertNull(assertion, "more than one assertion in test case " + name);
                  assertion = Assertion.assert_not_a_grammar;
                  errorCodeAttr = resultChildNode
                      .getAttributes()
                      .getNamedItemNS(null, "error-code");
                  assertNotNull(errorCodeAttr, "missing error-code attribute in assert-not-a-grammr in test case " + name);
                  errorCodes = Arrays.stream(errorCodeAttr.getTextContent().split(" "))
                      .filter(code -> ! "none".equals(code))
                      .collect(Collectors.toSet());
                  break;
                }
              }
            }

            ((Element) testCaseChildNode).getElementsByTagNameNS(TestCatalog.namespace, "");
          }
          if (input != null) {
            assertNull(this.input, "more than one input for test case " + name);
            this.input = input;
          }
        }
      }

      assertNotNull(assertion, "no assertion for test case " + name);
      if (assertion == Assertion.assert_xml)
        assertFalse(outputs.isEmpty(),
              "missing output specificaton for "
            + (isGrammarTest ? "grammar " : "")
            + "test case " + name);

      if (isGrammarTest) {
        assertNull(input, "unexpected input for grammar test case " + name);
      }
      else {
        assertNotNull(input, "missing input for test case " + name);
      }
    }
    catch (Exception e) {
      skippedBecause = "Failed to setup test case due to " + e.getClass().getSimpleName() + ": " + e.getMessage();
    }
  }

  public String getName() {
    return name;
  }

  public String getGrammar() {
    return grammar;
  }

  public boolean isXmlGrammar() {
    return isXmlGrammar;
  }

  public boolean isGrammarTest() {
    return isGrammarTest;
  }

  public String getInput() {
    return input;
  }

  public List<String> getOutputs() {
    return outputs;
  }

  public Assertion getAssertion() {
    return assertion;
  }

  public Set<String> getErrorCodes() {
    return errorCodes;
  }

  public String getSkippedBecause() {
    return skippedBecause;
  }

  @Override
  public String toString() {
    String indentation =
         "\n               ";
    return "\n"
         + "TestCase\n"
         + "         name: " + name + "\n"
         + "isGrammarTest: " + isGrammarTest + "\n"
         + "      grammar: " + grammar.replace("\n", indentation) + "\n"
         + " isXmlGrammar: " + isXmlGrammar + "\n"
         + "    assertion: " + assertion + "\n"
         + "   errorCodes: " + errorCodes + "\n"
         + "        input: " + (input == null ? "" : input.replace("\n", indentation) + "\n"
         + "       output: " + outputs.toString().replace("\n", indentation) + "\n");
  }

  public enum Assertion {
    assert_xml,
    assert_not_a_sentence,
    assert_not_a_grammar,
    assert_dynamic_error,
  }

  public static String elementToString(Element element) {
    DOMImplementationLS domImplementation = (DOMImplementationLS) element.getOwnerDocument().getImplementation();
    LSOutput lsOutput =  domImplementation.createLSOutput();
    lsOutput.setEncoding("UTF-8");
    Writer stringWriter = new StringWriter();
    lsOutput.setCharacterStream(stringWriter);
    LSSerializer lsSerializer = domImplementation.createLSSerializer();
    lsSerializer.write(element, lsOutput);
    return stringWriter.toString();
  }

}
