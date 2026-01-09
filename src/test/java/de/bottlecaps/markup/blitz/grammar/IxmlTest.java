// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.grammar;

import static de.bottlecaps.markup.Blitz.normalizeEol;
import static de.bottlecaps.markup.blitz.grammar.Ixml.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.bottlecaps.markup.Blitz;
import de.bottlecaps.markup.TestBase;

public class IxmlTest extends TestBase {
    private static final String invisiblexmlOrgUrl = "https://invisiblexml.org/current/ixml.ixml";

    private static final String githubJsonIxmlUrl = "https://raw.githubusercontent.com/GuntherRademacher/rex-parser-benchmark/main/src/main/resources/de/bottlecaps/rex/benchmark/json/parsers/xquery/json.ixml";
    private static final String jsonIxmlResource = "json.ixml";

    private static String ixmlIxmlResourceContent;
    private static String jsonIxmlResourceContent;

    @BeforeAll
    public static void beforeAll() throws URISyntaxException, IOException {
      ixmlIxmlResourceContent = normalizeEol(resourceContent(Blitz.IXML_GRAMMAR_RESOURCE));
      jsonIxmlResourceContent = normalizeEol(resourceContent(jsonIxmlResource));
    }

    @Test
    public void testIxmlResource() throws Exception {
      Grammar grammar = parse(ixmlIxmlResourceContent);
      String expectedResult = ixmlIxmlResourceContent
          .replaceAll("^\\{[^\n]*\n", "")
          .replaceAll("\\. \\{[^\n]*\n", ".\n");
      assertEquals(expectedResult, grammar.toString(), "roundtrip failed for " + Blitz.IXML_GRAMMAR_RESOURCE);
    }

    @Test
    public void testJsonIxmlResource() throws Exception {
      Grammar grammar = parse(jsonIxmlResourceContent);
      assertEquals(jsonIxmlResourceContent, grammar.toString(), "roundtrip failed for " + jsonIxmlResource);
    }

    @Test
    public void testGithubJsonIxmlUrl() throws Exception {
      String expectedResult = jsonIxmlResourceContent
          .replaceAll("^\\{[^\n]*\n", "")
          .replaceAll("\\. \\{[^\n]*\n", ".\n");
      testUrlContent(githubJsonIxmlUrl, jsonIxmlResource, expectedResult);
    }

    @Test
    public void testInvisiblexmlOrgUrlContent() throws Exception {
      String expectedResult = ixmlIxmlResourceContent
          .replaceAll("^\\{[^\n]*\n", "")
          .replaceAll("\\. \\{[^\n]*\n", ".\n");
      testUrlContent(invisiblexmlOrgUrl, Blitz.IXML_GRAMMAR_RESOURCE, expectedResult);
    }

    private void testUrlContent(String url, String resource, String expectedResult)
        throws MalformedURLException, URISyntaxException {

      try {
        Grammar grammar = Ixml.parse(cachedUrlContent(new URI(url).toURL()));
        assertEquals(expectedResult, grammar.toString(), "parsing content of " + url + " did not yield " + resource);
      }
      catch (RuntimeException e) {
        assumeFalse(e.getCause() instanceof UnknownHostException,
            "Test skipped due to " + e.getCause().getClass().getSimpleName() + " - presumably there is no Internet connection");
        throw e;
      }
    }
}
