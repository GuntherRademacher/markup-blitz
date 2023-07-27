package de.bottlecaps.markup.blitz.grammar;

import static de.bottlecaps.markup.Blitz.resourceContent;
import static de.bottlecaps.markup.Blitz.urlContent;
import static de.bottlecaps.markup.blitz.grammar.Ixml.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
      Grammar grammar = parse(ixmlIxmlResourceContent);
      String expectedResult = ixmlIxmlResourceContent
          .replaceAll("^\\{[^\n]*\n", "")
          .replaceAll("\\. \\{[^\n]*\n", ".\n");
      assertEquals(expectedResult, grammar.toString(), "roundtrip failed for " + ixmlResource);
    }

    @Test
    public void testJsonIxmlResource() throws Exception {
      Grammar grammar = parse(jsonIxmlResourceContent);
      assertEquals(jsonIxmlResourceContent, grammar.toString(), "roundtrip failed for " + jsonIxmlResource);
    }

    @Test
    @Disabled
    public void testGithubJsonIxmlUrl() throws Exception {
      String expectedResult = jsonIxmlResourceContent
          .replaceAll("^\\{[^\n]*\n", "")
          .replaceAll("\\. \\{[^\n]*\n", ".\n");
      testUrlContent(githubJsonIxmlUrl, jsonIxmlResource, expectedResult);
    }

    @Test
    @Disabled
    public void testInvisiblexmlOrgUrlContent() throws Exception {
      String expectedResult = ixmlIxmlResourceContent
          .replaceAll("^\\{[^\n]*\n", "")
          .replaceAll("\\. \\{[^\n]*\n", ".\n");
      testUrlContent(invisiblexmlOrgUrl, ixmlResource, expectedResult);
    }

    private void testUrlContent(String url, String resource, String expectedResult) throws IOException, MalformedURLException {
      Grammar grammar = Ixml.parse(urlContent(new URL(url)));
      assertEquals(expectedResult, grammar.toString(), "parsing content of " + url + " did not yield " + resource);
    }
}
