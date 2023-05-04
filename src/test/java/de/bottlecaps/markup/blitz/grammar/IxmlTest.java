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
      Ixml parser = new Ixml(ixmlIxmlResourceContent);
      try
      {
        parser.parse_ixml();
      }
      catch (ParseException pe)
      {
        throw new RuntimeException("ParseException while processing " + ixmlResource + ":\n" + parser.getErrorMessage(pe), pe);
      }
      assertEquals(ixmlIxmlResourceContent, parser.grammar().toString(), "roundtrip failed for " + ixmlResource);
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

    private void testUrlContent(String url, String resource, String resourceContent) throws IOException, MalformedURLException {
      String input = urlContent(new URL(url));
      Ixml parser = new Ixml(input);
      try
      {
        parser.parse_ixml();
      }
      catch (ParseException pe)
      {
        throw new RuntimeException("ParseException while processing " + url + ":\n" + parser.getErrorMessage(pe), pe);
      }
      assertEquals(resourceContent, parser.grammar().toString(), "parsing content of " + url + " did not yield " + resource);
    }
}
