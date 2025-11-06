// Copyright (c) 2023-2025 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup;

import static de.bottlecaps.markup.blitz.grammar.Ixml.parse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.bottlecaps.markup.blitz.Option;
import de.bottlecaps.markup.blitz.Parser;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.transform.BNF;
import de.bottlecaps.markup.blitz.transform.Generator;
import de.bottlecaps.markup.blitz.xml.XmlGrammarInput;
import de.bottlecaps.markup.blitz.Version;

/**
 * The Markup Blitz main class. It provides static methods for parser
 * generation and the main method for command line execution.
 *
 * @author Gunther Rademacher
 */
public class Blitz {
  /** The ixml grammar resource. */
  public final static String IXML_GRAMMAR_RESOURCE = "de/bottlecaps/markup/blitz/ixml.ixml";

  /**
   * Generate a parser from an Invisible XML grammar in ixml notation.
   *
   * @param grammar the Invisible XML grammar in ixml notation.
   * @return the generated parser
   * @throws BlitzException if any error is detected while generating the parser
   */
  public static Parser generate(String grammar) throws BlitzException {
    return generate(grammar, Collections.emptyMap());
  }

  /**
   * Generate a parser from an Invisible XML grammar in ixml notation.
   *
   * @param grammar the Invisible XML grammar in ixml notation.
   * @param options options for use at generation time and parsing time
   * @return the generated parser
   * @throws BlitzException if any error is detected while generating the parser
   */
  public static Parser generate(String grammar, Map<Option, Object> options) throws BlitzException {
    long t0 = 0, t1 = 0, t2 = 0, t3 = 0;
    Option.validate(options);
    boolean timing = Option.TIMING.is(true, options);
    if (timing)
      t0 = System.currentTimeMillis();
    Grammar tree = parse(grammar);
    if (timing)
      t1 = System.currentTimeMillis();
    Grammar bnf = BNF.process(tree, options);
    if (timing)
      t2 = System.currentTimeMillis();
    Parser parser = Generator.generate(bnf, options);
    if (timing) {
      t3 = System.currentTimeMillis();
      System.err.println("             parsing time: " + (t1 - t0) + " msec");
      System.err.println("  BNF transformation time: " + (t2 - t1) + " msec");
      System.err.println("LALR(1) construction time: " + (t3 - t2) + " msec");
    }
    return parser;
  }

  /**
   * Generate a parser from an Invisible XML grammar in XML, passed as an InputStream.
   *
   * @param xml the Invisible XML grammar in XML
   * @param options options for use at generation time and parsing time
   * @return the generated parser
   * @throws BlitzException if any error is detected while generating the parser
   */
  public static Parser generateFromXml(InputStream xml, Map<Option, Object> options) throws BlitzException {
    return generate(new XmlGrammarInput(xml).toIxml(), options);
  }

  /**
   * Generate a parser from an Invisible XML grammar in XML, passed as a String.
   *
   * @param xml the Invisible XML grammar in XML
   * @param options options for use at generation time and parsing time
   * @return the generated parser
   * @throws BlitzException if any error is detected while generating the parser
   */
  public static Parser generateFromXml(String xml, Map<Option, Object> options) throws BlitzException {
    return generate(new XmlGrammarInput(xml).toIxml(), options);
  }

  /**
   * Process a command line in order to generate a parser from an Invisible XML grammar, in
   * ixml notation, and parse some input using the generated parser. Write the resulting XML
   * to standard output. Grammars and inputs must be presented in UTF-8 encoding, and the
   * XML output is in UTF-8, too.
   *
   * @param args command line arguments
   * @throws IOException if any input cannot be accessed
   */
  public static void main(String[] args) throws IOException {
    System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

    Map<Option, Object> options = new HashMap<>();
    int i = 0;
    for (String arg : args) {
      if (! arg.startsWith("-"))
        break;
      ++i;
      if (! Option.addTo(options, arg)) {
        System.err.println("Unsupported option: " + arg);
        System.err.println();
        usage();
      }
    }

    if (i != args.length - 2 && i != args.length - 1)
      usage();

    String grammar = i == args.length - 1
        ? "!" + ixmlGrammar()
        : args[i];
    String input = args[args.length - 1];

    String grammarString = grammar.startsWith("!")
                         ? grammar.substring(1)
                         : urlContent(url(grammar));
    Parser parser = generate(grammarString, options);
    String inputString = input.startsWith("!")
                       ? input.substring(1)
                       : urlContent(url(input));
    String result = parser.parse(inputString);
    System.out.print("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    if (Option.INDENT.is(true, options))
      System.out.print("\n");
    System.out.print(result);
  }

  private static void usage() {
    String resource = Blitz.class.getResource("/" + Blitz.class.getName().replace('.',  '/') + ".class").toString();
    final String origin = resource.startsWith("jar:")
      ? "-jar " + resource.replaceFirst("^.*/([^/]+.jar)!.*$", "$1")
      : Blitz.class.getName();

    System.err.println("Markup Blitz - Invisible XML processor");
    System.err.println();
    System.err.println("  version " + Version.VALUE);
    System.err.println("  built " + Version.DATE);
    System.err.println();
    System.err.println("Usage: java " + origin + " [<OPTION>...] [<GRAMMAR>] <INPUT>");
    System.err.println();
    System.err.println("  Compile an Invisible XML grammar, and parse input with the resulting parser.");
    System.err.println();
    System.err.println("  <GRAMMAR>          the grammar (literal, file name or URL), in ixml notation.");
    System.err.println("                     When omitted, the ixml grammar will be used.");
    System.err.println("  <INPUT>            the input (literal, file name or URL).");
    System.err.println();
    System.err.println("  <OPTION>:");
    System.err.println("    --indent         generate resulting xml with indentation.");
    System.err.println("    --trace          print parser trace.");
    System.err.println("    --timing         print timing information.");
    System.err.println("    --verbose        print intermediate results.");
    System.err.println("    --fail-on-error  throw an exception instead of returning an error document.");
    System.err.println("    --leading-content-policy:<VALUE>    handling of leading unmatched content.");
    System.err.println("          <VALUE>:");
    System.err.println("              complete-match    disallow leading unmatched content.");
    System.err.println("              first-match       skip unmatched content, accept first match.");
    System.err.println("    --trailing-content-policy:<VALUE>   handling of trailing unmatched content.");
    System.err.println("          <VALUE>:");
    System.err.println("              complete-match    disallow trailing unmatched content.");
    System.err.println("              longest-match     accept longest match, ignore remainder.");
    System.err.println("              shortest-match    accept shortest match, ignore remainder.");
    System.err.println();
    System.err.println("  A literal grammar or input must be preceded by an exclamation point (!).");
    System.err.println("  All inputs must be presented in UTF-8 encoding, and output is written in");
    System.err.println("  UTF-8 as well. Resulting XML goes to standard output, all diagnostics go");
    System.err.println("  to standard error.");
    System.exit(1);
  }

  /**
   * Get the content behind a URL as a string.
   *
   * @param url the URL
   * @return the string
   * @throws IOException if the content cannot be accessed
   */
  public static String urlContent(URL url) throws IOException {
    String input;
    try (InputStream in = url.openStream()) {
      input = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    return input.replaceFirst("^\uFEFF", "");
  }

  /**
   * Normalize line endings to be just LF, similar to XML
   * <a href="https://www.w3.org/TR/2008/REC-xml-20081126/#sec-line-ends">End-of-Line Handling</a>.
   * This is necessary when reading resources from this project, when it was checked out on Windows
   * using the default Git configuration (which is core.autocrlf=true on Windows).
   *
   * @param input string to be normalized
   * @return the normalized string
   */
  public static String normalizeEol(String input) {
    return input
        .replace("\r\n", "\n")
        .replace("\r", "\n");
  }

  /**
   * Convert a file name string or URL string to a URL object.
   *
   * @param input the string
   * @return the URL
   */
  public static URL url(final String input) {
    URI uri = null;
    try {
      File file = new File(input);
      if (file.exists())
        uri = file.toURI();
    }
    catch (Exception e) {
    }
    try {
      if (uri == null)
        uri = new URI(input);
      return uri.toURL();
    }
    catch (Exception e) {
      throw new BlitzException("Failed to process URL: " + input, e);
    }
  }

  /**
   * Return the ixml grammar as a string.
   *
   * @return the ixml grammar
   */
  public static String ixmlGrammar() {
    try {
      String grammar = urlContent(Blitz.class.getClassLoader().getResource(IXML_GRAMMAR_RESOURCE));
      return normalizeEol(grammar);
    }
    catch (IOException e) {
      throw new BlitzException("Failed to access ixml grammar resource " + IXML_GRAMMAR_RESOURCE, e);
    }
  }
}
