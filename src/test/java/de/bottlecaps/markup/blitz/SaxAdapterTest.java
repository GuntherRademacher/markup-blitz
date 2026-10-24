// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import de.bottlecaps.markup.TestBase;

public class SaxAdapterTest extends TestBase {

  private static final String GRAMMAR =
        "doc: item+.\n"
      + "item: -\"(\", @id, -\")\", [\"a\"-\"z\"]+.\n"
      + "id: [\"0\"-\"9\"]+.\n";

  /** Elements, nested repetition, attributes and text, no namespaces. */
  @Test
  public void testElementsAttributesAndText() {
    Parser parser = generate(GRAMMAR);
    String input = "(1)foo(2)bar";
    String expected = parser.parse(input);
    Document dom = toDom(parser, input);
    assertTrue(deepEqual(expected, serialize(dom)),
        () -> "expected:\n" + expected + "\nbut got:\n" + serialize(dom));
  }

  /** The error document exercises namespace-aware handling (xmlns:ixml + prefixed attributes). */
  @Test
  public void testNamespaces() {
    Parser parser = generate(GRAMMAR);
    String input = "(x)foo"; // 'x' is not a digit: yields an ixml:state="failed" document
    String expected = parser.parse(input);
    Document dom = toDom(parser, input);
    assertTrue(deepEqual(expected, serialize(dom)),
        () -> "expected:\n" + expected + "\nbut got:\n" + serialize(dom));

    // the prefixed attribute must be resolved to the ixml namespace, not left in no namespace
    Node stateAttr = dom.getDocumentElement().getAttributeNodeNS(Parser.IXML_NAMESPACE, "state");
    assertTrue(stateAttr != null, "ixml:state should be namespace-qualified");
    assertEquals(Parser.IXML_NAMESPACE, stateAttr.getNamespaceURI());
    assertEquals("state", stateAttr.getLocalName());
  }

  /** Feed the parse result straight into the built-in XSLT engine, no extra dependency. */
  @Test
  public void testXsltTransform() {
    Parser parser = generate(GRAMMAR);
    String stylesheet =
          "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>\n"
        + "  <xsl:output method='text'/>\n"
        + "  <xsl:template match='/'><xsl:value-of select='count(//item)'/></xsl:template>\n"
        + "</xsl:stylesheet>";
    try {
      SAXTransformerFactory factory = (SAXTransformerFactory) TransformerFactory.newInstance();
      Templates templates = factory.newTemplates(new StreamSource(new StringReader(stylesheet)));
      TransformerHandler handler = factory.newTransformerHandler(templates);
      StringWriter writer = new StringWriter();
      handler.setResult(new StreamResult(writer));
      parser.parse("(1)foo(2)bar", new SaxAdapter(handler));
      assertEquals("2", writer.toString());
    }
    catch (Exception e) {
      throw new RuntimeException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
    }
  }

  /** Feed the parse result straight into a built-in W3C XML Schema validator, no extra dependency. */
  @Test
  public void testSchemaValidation() {
    Parser parser = generate(GRAMMAR);
    String schema =
          "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>\n"
        + "  <xs:element name='doc'>\n"
        + "    <xs:complexType><xs:sequence>\n"
        + "      <xs:element name='item' maxOccurs='unbounded'>\n"
        + "        <xs:complexType><xs:simpleContent>\n"
        + "          <xs:extension base='xs:string'><xs:attribute name='id' use='required'/></xs:extension>\n"
        + "        </xs:simpleContent></xs:complexType>\n"
        + "      </xs:element>\n"
        + "    </xs:sequence></xs:complexType>\n"
        + "  </xs:element>\n"
        + "</xs:schema>";
    assertTrue(validate(parser, "(1)foo(2)bar", schema),
        "document should be valid against the schema");

    // requiring integer content makes the letter content of the same document invalid
    String strictSchema = schema.replace("base='xs:string'", "base='xs:integer'");
    assertFalse(validate(parser, "(1)foo", strictSchema),
        "letter content should be invalid against an integer-typed schema");
  }

  /**
   * Validate the parse result against a schema, collecting validation errors rather than
   * throwing on the first one (the idiomatic {@link ValidatorHandler} usage).
   *
   * @return whether the document is valid
   */
  private static boolean validate(Parser parser, String input, String schemaSource) {
    try {
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Schema schema = schemaFactory.newSchema(new StreamSource(new StringReader(schemaSource)));
      ValidatorHandler validatorHandler = schema.newValidatorHandler();
      List<SAXParseException> errors = new ArrayList<>();
      validatorHandler.setErrorHandler(new ErrorHandler() {
        @Override public void warning(SAXParseException e) { }
        @Override public void error(SAXParseException e) { errors.add(e); }
        @Override public void fatalError(SAXParseException e) { errors.add(e); }
      });
      parser.parse(input, new SaxAdapter(validatorHandler));
      return errors.isEmpty();
    }
    catch (SAXException e) {
      throw new RuntimeException(e); // schema compilation failure: a test setup problem
    }
  }

  /**
   * A content handler that throws is reported as a {@link SaxAdapterException} and propagates
   * out of {@link Parser#parse}, rather than being swallowed into an ixml error document.
   */
  @Test
  public void testHandlerExceptionPropagates() {
    Parser parser = generate(GRAMMAR);
    ContentHandler failing = new DefaultHandler() {
      @Override
      public void startElement(String uri, String localName, String qName, Attributes atts)
          throws SAXException {
        throw new SAXException("boom");
      }
    };
    assertThrows(SaxAdapterException.class, () -> parser.parse("(1)foo", new SaxAdapter(failing)));
  }

  /** Build a DOM tree from the parse result, using only standard APIs plus {@link SaxAdapter}. */
  private static Document toDom(Parser parser, String input) {
    try {
      SAXTransformerFactory factory = (SAXTransformerFactory) TransformerFactory.newInstance();
      TransformerHandler handler = factory.newTransformerHandler();
      DOMResult result = new DOMResult();
      handler.setResult(result);
      parser.parse(input, new SaxAdapter(handler));
      return (Document) result.getNode();
    }
    catch (Exception e) {
      throw new RuntimeException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
    }
  }

  private static String serialize(Node node) {
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(node), new StreamResult(writer));
      return writer.toString();
    }
    catch (Exception e) {
      throw new RuntimeException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
    }
  }
}
