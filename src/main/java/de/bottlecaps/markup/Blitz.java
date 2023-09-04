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

import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.parser.Parser;
import de.bottlecaps.markup.blitz.transform.BNF;
import de.bottlecaps.markup.blitz.transform.Generator;
import de.bottlecaps.markup.blitz.xml.XmlGrammarInput;

public class Blitz {
  /**
   * Generate a parser from an Invisible XML grammar in ixml notation.
   *
   * @param grammar the Invisible XML grammar in ixml notation
   * @param blitzOptions options for use at generation time and parsing time
   * @return the generated parser
   * @throws BlitzException if any error is detected while generating the parser
   */
  public static Parser generate(String grammar, BlitzOption... blitzOptions) throws BlitzException {
    long t0 = 0, t1 = 0, t2 = 0, t3 = 0;
    Set<BlitzOption> options = Set.of(blitzOptions);
    boolean timing = options.contains(BlitzOption.TIMING);
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
   * Generate a parser from an Invisible XML grammar in XML, passed as a String.
   *
   * @param xml the Invisible XML grammar in XML
   * @param blitzOptions options for use at generation time and parsing time
   * @return the generated parser
   * @throws BlitzException if any error is detected while generating the parser
   */
  public static Parser generateFromXml(InputStream xml, BlitzOption... blitzOptions) throws BlitzException {
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
  public static Parser generateFromXml(String xml, BlitzOption... blitzOptions) throws BlitzException {
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

    Set<BlitzOption> options = new HashSet<>();
    int i = 0;
    for (; i < args.length; ++i) {
      if (args[i].equals("-v") || args[i].equals("--verbose"))
        options.add(BlitzOption.VERBOSE);
      else if (args[i].equals("-t") || args[i].equals("--trace"))
        options.add(BlitzOption.TRACE);
      else if (args[i].equals("-i") || args[i].equals("--indent"))
        options.add(BlitzOption.INDENT);
      else if (args[i].equals("-?") || args[i].equals("--help"))
        usage(0);
      else if (args[i].startsWith("-"))
        usage(1);
      else
        break;
    }

    if (i != args.length - 2)
      usage(1);
    String grammar = args[i];
    String input = args[i + 1];

    String grammarString = grammar.startsWith("!")
                         ? grammar.substring(1)
                         : urlContent(url(grammar));
    Parser parser = generate(grammarString, options.toArray(BlitzOption[]::new));
    String inputString = input.startsWith("!")
                       ? input.substring(1)
                       : urlContent(url(input));
    String result = parser.parse(inputString);
    System.out.print("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    if (options.contains(BlitzOption.INDENT))
      System.out.println();
    System.out.print(result);
  }

  private static void usage(int exitCode) {
    System.err.println("Usage: java -jar markup-blitz.jar [<OPTION>...] <GRAMMAR> <INPUT>");
    System.err.println();
    System.err.println("  Compile an Invisible XML grammar, and parse input with the resulting parser.");
    System.err.println();
    System.err.println("  <GRAMMAR>          the grammar (literal, file name or URL), in ixml notation.");
    System.err.println("  <INPUT>            the input (literal, file name or URL).");
    System.err.println();
    System.err.println("  Options:");
    System.err.println("    --verbose, -v    print intermediate results (to standard output).");
    System.err.println("    --timing         print timing information (to standard output).");
    System.err.println("    --indent, -i     generate resulting xml with indentation.");
    System.err.println("    --trace          print parser trace (to standard error).");
    System.err.println("    --help, -h, -?   print this information.");
    System.err.println();
    System.err.println("  A literal grammar or input must be preceded by an exclamation point (!).");
    System.err.println("  All inputs must be presented in UTF-8 encoding, and output is written in");
    System.err.println("  UTF-8 as well.");
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
    return input
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replaceFirst("^\uFEFF", "");
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
      throw new BlitzException("failed to process URL: " + input, e);
    }
  }

}
