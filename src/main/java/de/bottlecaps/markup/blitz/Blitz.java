package de.bottlecaps.markup.blitz;

import static de.bottlecaps.markup.blitz.grammar.Ixml.parse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.parser.Parser;
import de.bottlecaps.markup.blitz.transform.BNF;
import de.bottlecaps.markup.blitz.transform.Generator;

public class Blitz {
  public static void main(String[] args) throws MalformedURLException, IOException, URISyntaxException {
    Set<BlitzOption> options = new HashSet<>();
    int i = 0;
    for (; i < args.length; ++i) {
      if (args[i].equals("-v") || args[i].equals("--verbose"))
        options.add(BlitzOption.VERBOSE);
      else if (args[i].equals("-t") || args[i].equals("--trace"))
        options.add(BlitzOption.TRACE);
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
    parser.parse(urlContent(url(input)));
  }

  private static void usage(int exitCode) {
    System.err.println("Usage: java " + Blitz.class.getName() + " [<OPTION>...] <GRAMMAR> <INPUT>");
    System.err.println();
    System.err.println("  compile an Invisible XML grammar, and parse input with the resulting parser.");
    System.err.println();
    System.err.println("  <GRAMMAR>          the grammar (file name or URL).");
    System.err.println("  <INPUT>            the input (file name or URL).");
    System.err.println();
    System.err.println("  Options:");
    System.err.println("    -v, --verbose    print intermediate results to standard output.");
    System.err.println("    -t, --trace      print parser trace to standard error.");
    System.err.println("    -?, -h, --help   print this information.");
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
    Grammar tree = parse(grammar);
    Grammar bnf = BNF.process(tree);
    return Generator.generate(bnf, Set.of(blitzOptions));
  }
}
