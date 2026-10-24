// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.transform;

import static de.bottlecaps.markup.Blitz.normalizeEol;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import de.bottlecaps.markup.Blitz;
import de.bottlecaps.markup.TestBase;
import de.bottlecaps.markup.blitz.grammar.Ixml;

public class TestToREx extends TestBase {
  private static final String jsonIxmlResource = "json.ixml";

  private static String ixmlIxmlResourceContent;
  private static String jsonIxmlResourceContent;

  @BeforeAll
  public static void beforeAll() throws URISyntaxException, IOException {
    ixmlIxmlResourceContent = resourceContent(Blitz.IXML_GRAMMAR_RESOURCE);
    jsonIxmlResourceContent = resourceContent(jsonIxmlResource);
  }

  /**
   * Convert an ixml grammar to REx, and compare the result to the expected result, which is
   * found in the test resource named after the test class, the test method, and extension .ebnf.
   */
  private static void assertREx(TestInfo testInfo, String ixml) {
    String testMethod = testInfo.getTestMethod().orElseThrow().getName();
    String resource = TestToREx.class.getName().replace('.', '/') + "/" + testMethod + ".ebnf";
    String expectedResult = normalizeEol(resourceContent(resource));
    String result = ToREx.process(BNF.process(Ixml.parse(normalizeEol(ixml)))) + "\n";
    assertEquals(expectedResult, normalizeEol(result));
  }

  @Test
  public void testIxmlResource(TestInfo testInfo) throws Exception {
    assertREx(testInfo, ixmlIxmlResourceContent);
  }

  @Test
  public void testJsonResource(TestInfo testInfo) throws Exception {
    assertREx(testInfo, jsonIxmlResourceContent);
  }

  @Test
  public void testAlphabet(TestInfo testInfo) throws Exception {
    assertREx(testInfo, "S: ~[].");
  }

  @Test
  public void testDeletedTerminal(TestInfo testInfo) throws Exception {
    // the alternatives yield different XML, so they must not look the same
    assertREx(testInfo, "S: 'a'; -'a'.");
  }

  @Test
  public void testInsertions(TestInfo testInfo) throws Exception {
    assertREx(testInfo, "S: 'a', +'x'; 'a', +'y'.");
  }

  @Test
  public void testMarks(TestInfo testInfo) throws Exception {
    assertREx(testInfo, "S: A; @A. A: 'a'.");
  }

  @Test
  public void testHiddenNonterminal(TestInfo testInfo) throws Exception {
    // -A yields the content of A as text, while A yields an element. The alternative that
    // is just a character set needs no annotation, but the other one is annotated.
    assertREx(testInfo, "S: A; -A. A: 'a'; 'b'.");
  }

  @Test
  public void testAbbreviatedAnnotation(TestInfo testInfo) throws Exception {
    // the annotation of an alternative that is just a charset is abbreviated, if it is long
    String ixml = "S: ['a'-'z'; 'A'-'Z'; '0'-'9'; '_'; '-'; '.'; #e000-#f000]; ['a'-'z'], 'x'.";
    assertREx(testInfo, ixml);
    for (String line : ToREx.process(BNF.process(Ixml.parse(ixml))).split("\n"))
      assertTrue(line.length() <= 100, line);
  }

  @Test
  public void testAnnotationEndInsideAlternative() throws Exception {
    // an annotation must not contain "?>", so it is split
    String result = ToREx.process(BNF.process(Ixml.parse("S: 'a', +'?>'.")));
    assertTrue(result.contains("<?ixml ['a'], +'??> <?ixml >'?>"), result);
  }
}
