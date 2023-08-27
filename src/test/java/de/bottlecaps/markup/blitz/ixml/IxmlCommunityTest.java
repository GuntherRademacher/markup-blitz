package de.bottlecaps.markup.blitz.ixml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.support.AnnotationConsumer;

import de.bottlecaps.markup.Blitz;
import de.bottlecaps.markup.BlitzException;
import de.bottlecaps.markup.TestBase;
import de.bottlecaps.markup.blitz.parser.Parser;

//Status: 3091/35/0/0
//@Disabled
public class IxmlCommunityTest extends TestBase {
  private static final String thisProject = "markup-blitz";
  private static final String ixmlProject = "ixml";
  private static File ixmlFolder;
  private static XMLInputFactory xmlInputFactory;
  private static Parser ixmlParser;

  private static enum SkipReason {
    SUCCESS_BUT_TOO_LONG("Test runs successfully, but takes to long to be with each execution."),
    TOO_MUCH_MEMORY("Test has never been completed within given memory limits."),
    ;
    private String detail;

    private SkipReason(String detail) {
      this.detail = detail;
    }
  }

  private static Map<String, SkipReason> skipReasons = new HashMap<>();
  static {
    skipReasons.put("Evens and odds/evens-odds/P-8192", SkipReason.SUCCESS_BUT_TOO_LONG);
    skipReasons.put("Evens and odds/evens-odds/N-8192", SkipReason.SUCCESS_BUT_TOO_LONG);
    skipReasons.put("Evens and odds/evens-odds/P-8193", SkipReason.SUCCESS_BUT_TOO_LONG);
    skipReasons.put("Evens and odds/evens-odds/N-8193", SkipReason.SUCCESS_BUT_TOO_LONG);
    skipReasons.put("Evens and odds/evens-odds/P-16384", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Evens and odds/evens-odds/N-16384", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Evens and odds/evens-odds/P-16385", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Evens and odds/evens-odds/N-16385", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests/sample.grammar.05/g05.c01", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests/sample.grammar.06/g06.c02", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests/sample.grammar.06/g06.c03", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests/sample.grammar.10/g10c01", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests/sample.grammar.11/g11c01", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests/sample.grammar.11/g11c02", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests/sample.grammar.19/g19c01", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests/sample.grammar.20/g20c01", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 2/sample.grammar.22/g22.c01", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 2/sample.grammar.22/g22.c03", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 2/sample.grammar.22/g22.c04", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 2/sample.grammar.22/g22.c05", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 2/sample.grammar.23/g23.c03", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 2/sample.grammar.23/g23.r05", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 2/sample.grammar.24/g24.c03", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 2/sample.grammar.25/g25.c02", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 2/sample.grammar.26/g26.c02", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 2/sample.grammar.27/g27.c03", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 2/sample.grammar.28/g28.c02", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 2/sample.grammar.29/g29.c03", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 3/sample.grammar.50/g50.c03", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 3/sample.grammar.50/g50.c04", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 3/sample.grammar.50/g50.c05", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 3/sample.grammar.50/g50.c06", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 3/sample.grammar.52/g52.c01", SkipReason.TOO_MUCH_MEMORY);
    skipReasons.put("Misc tests 3/sample.grammar.52/g52.c02", SkipReason.TOO_MUCH_MEMORY);
  }

  public static enum Catalog {
    ambiguous("tests/ambiguous/test-catalog.xml"),
    chars("tests/chars/test-catalog.xml"),
    correct("tests/correct/test-catalog.xml"),
    error("tests/error/test-catalog.xml"),
    grammar_misc_insertion("tests/grammar-misc/insertion-tests.xml"),
    grammar_misc_prolog("tests/grammar-misc/prolog-tests.xml"),
    grammar_misc("tests/grammar-misc/test-catalog.xml"),
    ixml("tests/ixml/test-catalog.xml"),
    misc_001_020("tests/misc/misc-001-020-catalog.xml"),
    misc_021_040("tests/misc/misc-021-040-catalog.xml"),
    misc_041_060("tests/misc/misc-041-060-catalog.xml"),
    parse("tests/parse/test-catalog.xml"),
    performance_a_star_doubling("tests/performance/a-star/doubling-test-catalog.xml"),
    performance_a_star_tenfold("tests/performance/a-star/tenfold-test-catalog.xml"),
    performance_evens_and_odds("tests/performance/evens-and-odds/test-catalog.xml"),
    syntax_catalog_as_grammar("tests/syntax/catalog-as-grammar-tests.xml"),
    syntax_catalog_as_instance_tests_ixml("tests/syntax/catalog-as-instance-tests-ixml.xml"),
    syntax_catalog_as_instance_tests_xml("tests/syntax/catalog-as-instance-tests-xml.xml"),
    syntax_catalog_of_correct_tests("tests/syntax/catalog-of-correct-tests.xml"),
    ;

    private final String path;

    private Catalog(String path) {
      this.path = path;
    }
  };

  @BeforeAll
  public static void beforeAll() throws Exception {
    xmlInputFactory = XMLInputFactory.newInstance();
    String thisPath = "/" + IxmlCommunityTest.class.getName().replace(".", "/") + ".class";
    URL thisResource = IxmlCommunityTest.class.getResource(thisPath);
    assertNotNull(thisResource);

    String thisUrl = thisResource.toString();
    assertTrue(thisUrl.contains(thisProject));

    String ixmlUrl = thisUrl.substring(0, thisUrl.indexOf("/" + thisProject + "/"))
                   + "/"
                   + ixmlProject;

    ixmlFolder = new File(new URL(ixmlUrl).toURI());
    assumeTrue(ixmlFolder.exists(),
          IxmlCommunityTest.class.getSimpleName() + " was not executed,\n"
        + "because this folder does not exist: " + ixmlFolder + "\n"
        + "For running this test, please make sure that the " + ixmlProject + " project is\n"
        + "available in the same location as the " + thisProject + " project.");
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.ambiguous)
  public void ambiguous(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.chars)
  public void chars(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.correct)
  public void correct(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.error)
  public void error(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.grammar_misc_insertion)
  public void grammar_misc_insertion(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.grammar_misc_prolog)
  public void grammar_misc_prolog(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.grammar_misc)
  public void grammar_misc(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.ixml)
  public void ixml(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.misc_001_020)
  public void misc_001_020(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.misc_021_040)
  public void misc_021_040(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.misc_041_060)
  public void misc_041_060(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.parse)
  public void parse(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.performance_a_star_doubling)
  public void performance_a_star_doubling(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.performance_a_star_tenfold)
  public void performance_a_star_tenfold(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.performance_evens_and_odds)
  public void performance_evens_and_odds(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.syntax_catalog_as_grammar)
  public void syntax_catalog_as_grammar(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.syntax_catalog_as_instance_tests_ixml)
  public void syntax_catalog_as_instance_tests_ixml(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.syntax_catalog_as_instance_tests_xml)
  public void syntax_catalog_as_instance_tests_xml(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @CatalogSource(catalog = Catalog.syntax_catalog_of_correct_tests)
  public void syntax_catalog_of_correct_tests_xml(String name, TestCase testCase) {
    test(testCase);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  public void other(String name, TestCase testCase) {
    test(testCase);
  }

  public static Stream<Arguments> other() throws IOException {
    URI baseUri = ixmlFolder.toURI();
    Set<String> testCatalogs = Files.walk(ixmlFolder.toPath())
      .filter(path -> path.getFileName().toString().endsWith(".xml"))
      .map(Path::toFile)
      .filter(xmlFile -> isTestCatalog(xmlFile))
      .map(xmlFile -> baseUri.relativize(xmlFile.toURI()))
      .map(URI::toString)
      .collect(Collectors.toSet());
    for (Catalog catalog : Catalog.values()) {
      assertTrue(testCatalogs.contains(catalog.path), "Missing test-catalog: " + catalog);
      testCatalogs.remove(catalog.path);
    }
    assertFalse(testCatalogs.isEmpty(), "No addtional test catalogs found, not even the empty ones.");
    List<TestCase> testCases = testCatalogs.stream()
      .map(catalogPath -> new TestCatalog(ixmlFolder, catalogPath))
      .filter(catalog -> ! catalog.getTestCases().isEmpty())
      .map(catalog -> {
        System.err.println("Running test cases from yet unknown test-catalog " + catalog.getPath() + ".");
        return catalog.getTestCases();
      })
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
    assumeFalse(testCases.isEmpty(), "Nothing to test here, because new unknown test catalogs were found.");
    return testCases.stream()
        .map(testCase -> Arguments.of(testCase.getName(), testCase));
  }

  private static boolean isTestCatalog(File xmlFile) {
    try (FileInputStream fileInputStream = new FileInputStream(xmlFile)) {
      XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(fileInputStream);
      try {
        while (reader.hasNext()) {
          int event = reader.next();
          if (event == XMLStreamConstants.START_ELEMENT) {
            QName rootElementName = reader.getName();
            return TestCatalog.namespace.equals(rootElementName.getNamespaceURI())
                && "test-catalog".equals(rootElementName.getLocalPart());
          }
        }
      }
      catch (XMLStreamException e) {
        return false;
      }
      finally {
        reader.close();
      }
      return false;
    }
    catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  @Documented
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @ArgumentsSource(CatalogArgumentsProvider.class)
  public static @interface CatalogSource {
    Catalog catalog();
  }

  public static class CatalogArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<CatalogSource> {
    private IxmlCommunityTest.Catalog catalog;

    @Override
    public void accept(CatalogSource t) {
      this.catalog = t.catalog();
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return new TestCatalog(ixmlFolder, catalog.path)
          .getTestCases()
          .stream()
          .map(testCase -> Arguments.of(testCase.getName(), testCase));
    }
  }

  static int n = 0;

  private void test(TestCase testCase) {
    assumeFalse(skipReasons.containsKey(testCase.getName()),
        () -> {
          SkipReason skipReason = skipReasons.get(testCase.getName());
          return "Test was skipped: [" + skipReason + "] " + skipReason.detail;
        });

    String input = testCase.getInput();
    Parser parser;
    try {
      parser = testCase.isXmlGrammar()
          ? Blitz.generateFromXml(testCase.getGrammar())
          : Blitz.generate(testCase.getGrammar());
    }
    catch (BlitzException e) {
      if (TestCase.Assertion.assert_not_a_grammar != testCase.getAssertion())
        throw new RuntimeException(
            "Generating parer for test case with assertion " + testCase.getAssertion()
          + " failed with exception", e);
      Set<String> expected = testCase.getErrorCodes();
      if (! expected.isEmpty()
       && ! expected.stream().anyMatch(c -> e.getMessage().startsWith("[" + c + "] ")))
          assertEquals(expected, e.getMessage());
      return;
    }

    if (testCase.isGrammarTest()) {
      assertNull(input, "unexpected input for grammar test " + testCase.getName());
      assertEquals(1, testCase.getOutputs().size(), "expected a single reference output for grammar test");
      if (ixmlParser == null) {
        String ixmlIxmlResourceContent = resourceContent("ixml.ixml");
        ixmlParser = Blitz.generate(ixmlIxmlResourceContent);
      }
      String xmlRepresentation = ixmlParser.parse(testCase.getGrammar());
      if (! deepEqual(testCase.getOutputs().get(0), xmlRepresentation))
        assertEquals(testCase.getOutputs().get(0), xmlRepresentation);
    }
    else {
      assertNotNull(input, "missing input");
      try {
        String xml = parser.parse(input);
        assertEquals(TestCase.Assertion.assert_xml, testCase.getAssertion());
        assertTrue(testCase.getOutputs().size() > 0, "missing reference output");
        if (! testCase.getOutputs().stream().anyMatch(o -> deepEqual(o, xml))) {
          if (testCase.getOutputs().size() == 1) {
            String expected = testCase.getOutputs().get(0);
            if (! expected.equals("<tbd/>\n"))
              assertEquals(expected, xml);
          }
          else {
            assertEquals(testCase.getOutputs(), xml);
          }
        }
      }
      catch (OutOfMemoryError e) {
        parser = null;
        System.gc();
        throw new RuntimeException(e.getMessage(), e);
      }
      catch (BlitzException e) {
        switch (testCase.getAssertion()) {
        case assert_not_a_sentence:
          assertNull(testCase.getErrorCodes());
          break;
        case assert_dynamic_error:
          Set<String> expected = testCase.getErrorCodes();
          if (! expected.isEmpty()
           && ! expected.stream().anyMatch(c -> e.getMessage().startsWith("[" + c + "] ")))
              assertEquals(expected, e.getMessage());
          break;
        default:
          throw new RuntimeException(
              "Parsing for test case with assertion " + testCase.getAssertion()
            + " failed with exception", e);
        }
      }
    }
  }

}