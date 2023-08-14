package de.bottlecaps.markup.blitz.ixml;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URL;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.AnnotationConsumer;

import de.bottlecaps.markup.TestBase;
import de.bottlecaps.markup.test.IxmlTest;

public class IxmlCommunityTest extends TestBase {
  private static final String thisProject = "markup-blitz";
  private static final String ixmlProject = "ixml";
  private static File ixmlFolder;

  @BeforeAll
  public static void beforeAll() throws Exception {
    String thisPath = "/" + IxmlTest.class.getName().replace(".", "/") + ".class";
    URL thisResource = IxmlTest.class.getResource(thisPath);
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

  @Test
  public void testBug() throws Exception {
    TestCatalog testCatalog = new TestCatalog(new File("test-catalog2.xml"));
    testCatalog.getTestCases();
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

  private void test(TestCase testCase) {
    System.out.println(testCase);
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
    ;

    private String path;

    private Catalog(String path) {
      this.path = path;
    }

    public String getPath() {
      return path;
    }
  };

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
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
      TestCatalog testCatalog = new TestCatalog(new File(ixmlFolder, catalog.getPath()));
      return testCatalog.getTestCases().stream()
        .map(testCase -> Arguments.of(testCase.getName(), testCase));
    }
  }
}
