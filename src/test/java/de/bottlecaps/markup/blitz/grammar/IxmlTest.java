package de.bottlecaps.markup.blitz.grammar;

import static de.bottlecaps.markup.blitz.Blitz.resourceContent;
import static de.bottlecaps.markup.blitz.Blitz.urlContent;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.bottlecaps.markup.blitz.grammar.Ixml.ParseException;

public class IxmlTest {
    private static final String invisiblexmlOrgUrl = "https://invisiblexml.org/1.0/ixml.ixml";
    private static final String ixmlResource = "ixml.ixml";

    private static final String githubJsonIxmlUrl = "https://raw.githubusercontent.com/GuntherRademacher/rex-parser-benchmark/main/src/main/resources/de/bottlecaps/rex/benchmark/json/parsers/xquery/json.ixml";
    private static final String jsonIxmlResource = "json.ixml";

    private static String ixmlIxmlResourceContent;
    private static String jsonIxmlResourceContent;

    @BeforeAll
    public static void beforeAll() throws URISyntaxException, IOException {
      ixmlIxmlResourceContent = resourceContent(ixmlResource);
      jsonIxmlResourceContent = resourceContent(jsonIxmlResource);
    }

    @Test
    public void testIxmlResource() throws Exception {
      Grammar grammar = parse(ixmlIxmlResourceContent, ixmlResource);
      assertEquals(ixmlIxmlResourceContent, grammar.toString(), "roundtrip failed for " + ixmlResource);
    }

    @Test
    public void testGithubJsonIxmlUrl() throws Exception {
      testUrlContent(githubJsonIxmlUrl, jsonIxmlResource, jsonIxmlResourceContent);
    }

    @Test
    @Disabled
    public void testInvisiblexmlOrgUrlContent() throws Exception {
      testUrlContent(invisiblexmlOrgUrl, ixmlResource, ixmlIxmlResourceContent);
    }

    @Test
    public void testCopyIxmlResource() {
      Grammar grammar = parse(ixmlIxmlResourceContent, ixmlResource);
//      Grammar copy = new Copy(grammar).get();
      Grammar copy = grammar.copy();
      assertEquals(grammar.toString(), copy.toString());
      assertEquals(grammar, copy);
    }

    @Test
    public void testCopyJsonResource() {
      Grammar grammar = parse(jsonIxmlResourceContent, jsonIxmlResource);
//      Grammar copy = new Copy(grammar).get();
      Grammar copy = grammar.copy();
      assertEquals(grammar.toString(), copy.toString());
      assertEquals(grammar, copy);
    }

    private void testUrlContent(String url, String resource, String resourceContent) throws IOException, MalformedURLException {
      Grammar grammar = parse(urlContent(new URL(url)), url);
      assertEquals(resourceContent, grammar.toString(), "parsing content of " + url + " did not yield " + resource);
    }

    private Grammar parse(String content, String source) {
      Ixml parser = new Ixml(content);
      try
      {
        parser.parse_ixml();
      }
      catch (ParseException pe)
      {
        throw new RuntimeException("ParseException while processing " + source + ":\n" + parser.getErrorMessage(pe), pe);
      }
      return parser.grammar();
    }
}
