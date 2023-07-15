package de.bottlecaps.markup.blitz.transform;

import static de.bottlecaps.markup.blitz.Blitz.resourceContent;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.nineml.coffeefilter.InvisibleXml;
import org.nineml.coffeefilter.InvisibleXmlDocument;
import org.nineml.coffeefilter.InvisibleXmlParser;
import org.nineml.coffeegrinder.parser.GearleyResult;

import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Ixml;

public class TestCoffee {
  private static final String ixmlResource = "ixml.ixml";
  private static final String jsonIxmlResource = "json.ixml";
  private static final String jsonResource = "sample.json";

  private static String ixmlIxmlResourceContent;
  private static String jsonIxmlResourceContent;
  private static String jsonResourceContent;

  @BeforeAll
  public static void beforeAll() throws URISyntaxException, IOException {
    ixmlIxmlResourceContent = resourceContent(ixmlResource);
    jsonIxmlResourceContent = resourceContent(jsonIxmlResource);
    jsonResourceContent = resourceContent(jsonResource);
  }

  @Test
  public void testIxmlResource() throws Exception {
    String originalResult = runCoffee(ixmlIxmlResourceContent, ixmlIxmlResourceContent);

    Grammar grammar = Ixml.parse(ixmlIxmlResourceContent);
    Grammar bnf = BNF.process(grammar, true); // need to isolate charsets here, otherwise we risk an OOME

    String equivalentResult = runCoffee(bnf.toString(), ixmlIxmlResourceContent);
    assertEquals(originalResult, equivalentResult);
  }

  @Test
  public void testJsonResource() throws Exception {
    String originalResult = runCoffee(jsonIxmlResourceContent, jsonResourceContent);

    Grammar grammar = Ixml.parse(jsonIxmlResourceContent);
    Grammar bnf = BNF.process(grammar);

    String equivalentResult = runCoffee(bnf.toString(), jsonResourceContent);
    assertEquals(originalResult, equivalentResult);
  }

  private String runCoffee(String grammar, String input) throws Exception {
    final InvisibleXmlParser parser = new InvisibleXml().getParserFromIxml(grammar);
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
