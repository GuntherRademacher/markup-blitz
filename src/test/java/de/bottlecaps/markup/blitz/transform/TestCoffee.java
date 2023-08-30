package de.bottlecaps.markup.blitz.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.nineml.coffeefilter.InvisibleXml;
import org.nineml.coffeefilter.InvisibleXmlDocument;
import org.nineml.coffeefilter.InvisibleXmlParser;
import org.nineml.coffeefilter.ParserOptions;
import org.nineml.coffeegrinder.parser.GearleyResult;

import de.bottlecaps.markup.TestBase;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Ixml;

public class TestCoffee extends TestBase {
  private static final String ixmlResource = "ixml.ixml";
  private static final String jsonIxmlResource = "json.ixml";
  private static final String jsonResource = "sample.json";
  private static final String addressIxmlResource = "address.ixml";
  private static final String addressResource = "address.input";

  private static String ixmlIxmlResourceContent;
  private static String jsonIxmlResourceContent;
  private static String jsonResourceContent;
  private static String addressIxmlResourceContent;
  private static String addressResourceContent;

  @BeforeAll
  public static void beforeAll() throws URISyntaxException, IOException {
    ixmlIxmlResourceContent = resourceContent(ixmlResource);
    jsonIxmlResourceContent = resourceContent(jsonIxmlResource);
    jsonResourceContent = resourceContent(jsonResource);
    addressIxmlResourceContent = resourceContent(addressIxmlResource);
    addressResourceContent = resourceContent(addressResource);
  }

  @Test
  @Disabled
  public void testIxmlResource() throws Exception {
    String originalResult = runCoffee(ixmlIxmlResourceContent, ixmlIxmlResourceContent);

    Grammar grammar = Ixml.parse(ixmlIxmlResourceContent);
    Grammar bnf = BNF.process(grammar, true, Collections.emptySet()); // need to isolate charsets here, otherwise we risk an OOME

    String equivalentResult = runCoffee(bnf.toString(), ixmlIxmlResourceContent);
    assertEquals(originalResult, equivalentResult);
  }

  @Test
  @Disabled
  public void testJsonResource() throws Exception {
    String originalResult = runCoffee(jsonIxmlResourceContent, jsonResourceContent);

    Grammar grammar = Ixml.parse(jsonIxmlResourceContent);
    Grammar bnf = BNF.process(grammar);

    String equivalentResult = runCoffee(bnf.toString(), jsonResourceContent);
    assertEquals(originalResult, equivalentResult);

    String expectedResult = "<map><member><key>string</key><value><string>Hello, World!</string></value></member><member><key>number</key><value><number>42</number></value></member><member><key>boolean</key><value><boolean>true</boolean></value></member><member><key>nullValue</key><value><null/></value></member><member><key>arrayEmpty</key><value><array/></value></member><member><key>arraySingle</key><value><array><number>1</number></array></value></member><member><key>arrayMultiple</key><value><array><number>1</number><number>2</number></array></value></member><member><key>object</key><value><map><member><key>property</key><value><string>value</string></value></member></map></value></member><member><key>escapedString</key><value><string>This string contains escape sequences: \" \\ / \n &#xD; &#x9;</string></value></member><member><key>unicodeString</key><value><string><unicode code=\"20AC\"/></string></value></member></map>";
    assertEquals(expectedResult.replace("&#x9;", "\t"), originalResult);
  }

  @Test
  public void testAddressResource() throws Exception {
    String originalResult = runCoffee(addressIxmlResourceContent, addressResourceContent);

    Grammar grammar = Ixml.parse(addressIxmlResourceContent);
    Grammar bnf = BNF.process(grammar);

    String equivalentResult = runCoffee(bnf.toString(), addressResourceContent);
    assertEquals(originalResult, equivalentResult);
  }

  private String runCoffee(String grammar, String input) throws Exception {
    ParserOptions options = new ParserOptions();
//    options.setPrettyPrint(true);
    InvisibleXml invisibleXml = new InvisibleXml(options);
    final InvisibleXmlParser parser = invisibleXml.getParserFromIxml(grammar);
    if (! parser.constructed()) {
      final Exception ex = parser.getException();
      if (ex != null) throw ex;
      final InvisibleXmlDocument generated = parser.getFailedParse();
      final GearleyResult result = generated.getResult();
      throw new RuntimeException("Parser generation failed on " + result.getLastToken() + ", line " + generated.getLineNumber() + ", column " + generated.getColumnNumber());
    }
    final InvisibleXmlDocument parsed = parser.parse(input);
    if (! parsed.succeeded()) {
      final GearleyResult result = parsed.getResult();
      throw new RuntimeException("Parsing failed on " + result.getLastToken() + ", line " + parsed.getLineNumber() + ", column " + parsed.getColumnNumber());
    }
    return parsed.getTree();
  }
}
