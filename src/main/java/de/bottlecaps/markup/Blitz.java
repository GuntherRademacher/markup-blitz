// Copyright (c) 2023-2024 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup;

import static de.bottlecaps.markup.blitz.grammar.Ixml.parse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import de.bottlecaps.markup.blitz.Parser;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.transform.BNF;
import de.bottlecaps.markup.blitz.transform.Generator;
import de.bottlecaps.markup.blitz.xml.XmlGrammarInput;

/**
 * The Markup Blitz main class. It provides static methods for parser
 * generation and the main method for command line execution.
 *
 * @author Gunther Rademacher
 */
public class Blitz {
  /** The ixml grammar resource. */
  public final static String IXML_GRAMMAR_RESOURCE = "de/bottlecaps/markup/blitz/ixml.ixml";

  /** Parser and generator options. */
  public enum Option {
    /**    Parser option: Generate XML with indentation.             */ INDENT,
    /**    Parser option: Print parser trace.                        */ TRACE,
    /**    Parser option: Fail on parsing error.                     */ FAIL_ON_ERROR,
    /**    Parser option: Partial parsing, accepting first match.    */ FIRST_MATCH,
    /** Generator option: Partial parsing, accepting longest match.  */ LONGEST_MATCH,
    /** Generator option: Partial parsing, accepting shortest match. */ SHORTEST_MATCH,
    /** Generator option: Print timing information.                  */ TIMING,
    /** Generator option: Print information on intermediate results. */ VERBOSE;
  }

  /**
   * Generate a parser from an Invisible XML grammar in ixml notation.
   *
   * @param grammar the Invisible XML grammar in ixml notation.
   * @param blitzOptions options for use at generation time and parsing time
   * @return the generated parser
   * @throws BlitzException if any error is detected while generating the parser
   */
  public static Parser generate(String grammar, Blitz.Option... blitzOptions) throws BlitzException {
    long t0 = 0, t1 = 0, t2 = 0, t3 = 0;
    Set<Blitz.Option> options = Set.of(blitzOptions);
    boolean timing = options.contains(Blitz.Option.TIMING);
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
   * @param blitzOptions options for use at generation time and parsing time
   * @return the generated parser
   * @throws BlitzException if any error is detected while generating the parser
   */
  public static Parser generateFromXml(InputStream xml, Option... blitzOptions) throws BlitzException {
    return generate(new XmlGrammarInput(xml).toIxml(), blitzOptions);
  }

  /**
   * Generate a parser from an Invisible XML grammar in XML, passed as a String.
   *
   * @param xml the Invisible XML grammar in XML
   * @param blitzOptions options for use at generation time and parsing time
   * @return the generated parser
   * @throws BlitzException if any error is detected while generating the parser
   */
  public static Parser generateFromXml(String xml, Option... blitzOptions) throws BlitzException {
    return generate(new XmlGrammarInput(xml).toIxml(), blitzOptions);
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

    Set<Option> options = new HashSet<>();
    int i = 0;
    for (; i < args.length; ++i) {
      if (args[i].equals("--indent"))
        options.add(Option.INDENT);
      else if (args[i].equals("--trace"))
        options.add(Option.TRACE);
      else if (args[i].equals("--fail-on-error"))
        options.add(Option.FAIL_ON_ERROR);
      else if (args[i].equals("--first-match"))
        options.add(Option.FIRST_MATCH);
      else if (args[i].equals("--longest-match"))
        options.add(Option.LONGEST_MATCH);
      else if (args[i].equals("--shortest-match"))
        options.add(Option.SHORTEST_MATCH);
      else if (args[i].equals("--timing"))
        options.add(Option.TIMING);
      else if (args[i].equals("--verbose"))
        options.add(Option.VERBOSE);
      else if (args[i].startsWith("-"))
        usage(1);
      else
        break;
    }

    if (i != args.length - 2 && i != args.length - 1)
      usage(1);
    String grammar = i == args.length - 1
        ? "!" + ixmlGrammar()
        : args[i];
    String input = args[args.length - 1];

    String grammarString = grammar.startsWith("!")
                         ? grammar.substring(1)
                         : urlContent(url(grammar));
    Parser parser = generate(grammarString, options.toArray(Option[]::new));
    String inputString = input.startsWith("!")
                       ? input.substring(1)
                       : urlContent(url(input));
    String result = parser.parse(inputString);
    System.out.print("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    if (options.contains(Option.INDENT))
      System.out.println();
    System.out.print(result);
  }

  private static void usage(int exitCode) {
    String resource = Blitz.class.getResource("/" + Blitz.class.getName().replace('.',  '/') + ".class").toString();
    final String origin = resource.startsWith("jar:")
      ? "-jar " + resource.replaceFirst("^.*/([^/]+.jar)!.*$", "$1")
      : Blitz.class.getName();

    System.err.println("Usage: java " + origin + " [<OPTION>...] [<GRAMMAR>] <INPUT>");
    System.err.println();
    System.err.println("  Compile an Invisible XML grammar, and parse input with the resulting parser.");
    System.err.println();
    System.err.println("  <GRAMMAR>           the grammar (literal, file name or URL), in ixml notation.");
    System.err.println("                      When omitted, the ixml grammar will be used.");
    System.err.println("  <INPUT>             the input (literal, file name or URL).");
    System.err.println();
    System.err.println("  <OPTION>:");
    System.err.println("    --indent          generate resulting xml with indentation.");
    System.err.println("    --trace           print parser trace.");
    System.err.println("    --fail-on-error   throw an exception instead of returning an error document.");
    System.err.println("    --longest-match   partial parsing, accepting the longest match.");
    System.err.println("    --shortest-match  partial parsing, accepting the shortest match.");
    System.err.println("    --timing          print timing information.");
    System.err.println("    --verbose         print intermediate results.");
    System.err.println();
    System.err.println("  A literal grammar or input must be preceded by an exclamation point (!).");
    System.err.println("  All inputs must be presented in UTF-8 encoding, and output is written in");
    System.err.println("  UTF-8 as well. Resulting XML goes to standard output, all diagnostics go");
    System.err.println("  to standard error.");
    System.err.println();
    System.exit(exitCode);
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
