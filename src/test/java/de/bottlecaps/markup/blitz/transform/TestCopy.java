// Copyright (c) 2023-2025 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.transform;

import static de.bottlecaps.markup.blitz.grammar.Ixml.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.bottlecaps.markup.Blitz;
import de.bottlecaps.markup.TestBase;
import de.bottlecaps.markup.blitz.grammar.Grammar;

public class TestCopy extends TestBase {
  private static final String jsonIxmlResource = "json.ixml";

  private static String ixmlIxmlResourceContent;
  private static String jsonIxmlResourceContent;

  @BeforeAll
  public static void beforeAll() throws URISyntaxException, IOException {
    ixmlIxmlResourceContent = resourceContent(Blitz.IXML_GRAMMAR_RESOURCE);
    jsonIxmlResourceContent = resourceContent(jsonIxmlResource);
  }

  @Test
  public void testCopyIxmlResource() {
    testCopy(parse(ixmlIxmlResourceContent));
  }

  @Test
  public void testCopyJsonResource() {
    testCopy(parse(jsonIxmlResourceContent));
  }

  private void testCopy(Grammar grammar) {
    Grammar copy1 = grammar.copy();
    assertEquals(grammar.toString(), copy1.toString());
    assertEquals(grammar, copy1);
    Grammar copy2 = Copy.process(grammar);
    assertEquals(grammar.toString(), copy2.toString());
    assertEquals(grammar, copy2);
  }
}
