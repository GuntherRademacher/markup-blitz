package de.bottlecaps.markup;

import static de.bottlecaps.markup.blitz.grammar.Ixml.parse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import de.bottlecaps.markup.blitz.character.RangeSet;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.parser.Parser;
import de.bottlecaps.markup.blitz.transform.BNF;
import de.bottlecaps.markup.blitz.transform.Generator;

public class Blitz {
  public static void main(String[] args) throws MalformedURLException, IOException, URISyntaxException {
    System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

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

    Parser parser = generate(urlContent(url(grammar)), options.toArray(BlitzOption[]::new));
    String result = parser.parse(urlContent(url(input)));
    System.out.print("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    if (options.contains(BlitzOption.INDENT))
      System.out.println();
    System.out.print(result);
  }

  private static void usage(int exitCode) {
    System.err.println("Usage: java " + Blitz.class.getName() + " [<OPTION>...] <GRAMMAR> <INPUT>");
    System.err.println();
    System.err.println("  Compile an Invisible XML grammar, and parse input with the resulting parser.");
    System.err.println();
    System.err.println("  <GRAMMAR>          the grammar (file name or URL).");
    System.err.println("  <INPUT>            the input (file name or URL).");
    System.err.println();
    System.err.println("  Options:");
    System.err.println("    -v,     --verbose   print intermediate results (to standard output).");
    System.err.println("            --timing    print timing information (to standard output).");
    System.err.println("    -i,     --indent    generate resulting xml with indentation.");
    System.err.println("            --trace     print parser trace (to standard error).");
    System.err.println("    -?, -h, --help      print this information.");
    System.err.println();
    System.err.println();
    System.exit(exitCode);
  }

  public static String resourceContent(String resource) throws IOException, MalformedURLException {
    return urlContent(Blitz.class.getClassLoader().getResource(resource));
  }

  private static URL url(final String input) throws URISyntaxException, MalformedURLException {
    URI uri = null;
    try {
      File file = new File(input);
      if (file.exists())
        uri = file.toURI();
    }
    catch (Exception e) {
    }
    if (uri == null)
      uri = new URI(input);
    URL url = uri.toURL();
    return url;
  }

  public static String urlContent(URL url) throws IOException, MalformedURLException {
    String input;
    try (InputStream in = url.openStream()) {
      input = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    return input
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replaceFirst("^\uFEFF", "");
  }

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
      System.err.println("     RangeSet build calls: " + RangeSet.buildCalls);
      RangeSet.buildCalls = 0;
    }
    return parser;
  }
}