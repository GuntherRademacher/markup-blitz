package de.bottlecaps.markup.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.basex.core.Context;
import org.basex.io.IO;
import org.basex.query.QueryProcessor;
import org.basex.query.value.node.DBNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.bottlecaps.markup.Blitz;
import de.bottlecaps.markup.TestBase;
import de.bottlecaps.markup.blitz.parser.Parser;

public class IxmlTest extends TestBase {
  private static final String thisProject = "markup-blitz";
  private static final String ixmlCorrectPath = "ixml/tests/correct";
  private static final String ixmlResource = "ixml.ixml";
  private static Parser ixmlParser;

  @BeforeAll
  public static void beforeAll() throws URISyntaxException, IOException {
    ixmlParser = Blitz.generate(resourceContent(ixmlResource));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  public void testCorrect(String name, String ixmlGrammar, String expectedXmlGrammar, String input, List<String> expectedOutputs) throws Exception {
//    System.out.println(
//           "name: " + name
//        + ", ixmlGrammar: " + (ixmlGrammar == null ? "no" : "yes")
//        + ", xmlGrammar: " + (xmlGrammar == null ? "no" : "yes")
//        + ", input: " + (input == null ? "no" : "yes")
//        + ", expectedResult: " + (expectedResults == null || expectedResults.isEmpty() ? "no" : "yes"));

    if (expectedXmlGrammar != null) {
      String actualXmlGrammar = ixmlParser.parse(ixmlGrammar);
      assertTrue(deepEqual(expectedXmlGrammar, actualXmlGrammar));
    }

    assertNotNull(ixmlGrammar);
    Parser parser = Blitz.generate(ixmlGrammar);

    if (input != null) {
      String actualOutput = parser.parse(input);
      if (expectedOutputs != null) {
        switch (expectedOutputs.size()) {
        case 0:
          break;
        case 1:
          assertTrue(deepEqual(expectedOutputs.get(0), actualOutput));
          break;
        default:
          boolean succeeded = false;
          for (String expectedOutput : expectedOutputs)
            if (deepEqual(expectedOutput, actualOutput)) {
              succeeded = true;
              break;
            }
          if (! succeeded)
            fail("unexpected output for testcase " + name + ":\n" + actualOutput);
          break;
        }
      }
    }
  }

  public static Stream<Arguments> testCorrect() throws Exception {
    String thisPath = "/" + IxmlTest.class.getName().replace(".", "/") + ".class";
    URL thisResource = IxmlTest.class.getResource(thisPath);
    assertNotNull(thisResource);

    String thisUrl = thisResource.toString();
    assertTrue(thisUrl.contains(thisProject));

    String ixmlUrl = thisUrl.substring(0, thisUrl.indexOf("/" + thisProject + "/"))
                   + "/"
                   + ixmlCorrectPath;

    File ixmlCorrectFolder = new File(new URL(ixmlUrl).toURI());
    assumeTrue(ixmlCorrectFolder.exists(),
          IxmlTest.class.getSimpleName() + ".testCorrect was not executed,\n"
        + "because this folder does not exist: " + ixmlCorrectFolder + "\n"
        + "For running this test, please make sure that the ixml project is\n"
        + "available in the same location as the " + thisProject + " project.");

    Map<String, Set<String>> testCases = new TreeMap<>();
    for (File file : ixmlCorrectFolder.listFiles()) {
      String fileName = file.getName();
      String testName = fileName;

      // special handling for bringing together version-decl.2.ixml and version-decl-two.ixml.xml
      testName = testName.replace(".2.", "-two.");

      // TODO: execute tests based on test-catalog.xml
      // ignore test-catalog.xml, for the time being
      if (testName.equals("test-catalog.xml"))
        continue;

      int extensionOffset = testName.indexOf('.');
      if (extensionOffset < 0)
        extensionOffset = testName.length();
      testName = testName.substring(0, extensionOffset);
      testCases.compute(testName, (k, v) -> {
        if (v == null)
          v = new TreeSet<>();
        v.add(fileName);
        return v;
      });
    }
    return testCases.entrySet().stream()
        .map(e -> {
          String ixmlGrammar = null;
          String xmlGrammar = null;
          String input = null;
          List<String> outputs = new ArrayList<>();
          Set<String> files = e.getValue();
          for (String file : files) {
            if (file.endsWith(".ixml.xml")) {
              assertNull(xmlGrammar, "multiple xml grammars: " + files);
              xmlGrammar = file;
            }
            else if (file.endsWith(".ixml")) {
              assertNull(ixmlGrammar, "multiple ixml grammars: " + files);
              ixmlGrammar = file;
            }
            else if (file.endsWith(".inp")) {
              assertNull(input, "multiple inputs: " + files);
              input = file;
            }
            else if (file.endsWith(".xml")) {
              outputs.add(file);
            }
            else {
              fail("unsupported extension: " + file);
            }
          }
//          return Arguments.of(
//              e.getKey(), ixmlGrammar, xmlGrammar, input, outputs);
          return Arguments.of(
              e.getKey(),
              ixmlGrammar == null ? null : fileContent(new File(ixmlCorrectFolder, ixmlGrammar)),
              xmlGrammar == null ? null : fileContent(new File(ixmlCorrectFolder, xmlGrammar)),
              input == null ? null : fileContent(new File(ixmlCorrectFolder, input)),
              outputs == null ? null : outputs.stream()
                  .map(o -> new File(ixmlCorrectFolder, o))
                  .map(TestBase::fileContent)
                  .collect(Collectors.toList()));
        });
  }

  private boolean deepEqual(String xml1, String xml2) throws Exception {
    String query =
          "declare variable $xml1 external;\n"
        + "declare variable $xml2 external;\n"
        + "deep-equal($xml1, $xml2)";
    try (QueryProcessor proc = new QueryProcessor(query, new Context())) {

      proc.variable("xml1", new DBNode(IO.get(xml1)));
      proc.variable("xml2", new DBNode(IO.get(xml1)));
      return (boolean) proc.value().toJava();
    }
  }
}
