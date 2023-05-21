package de.bottlecaps.markup.blitz;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Ixml;
import de.bottlecaps.markup.blitz.grammar.Ixml.ParseException;
import de.bottlecaps.markup.blitz.transform.BNF;
import de.bottlecaps.markup.blitz.transform.PostProcess;

public class Blitz {
  public static void main(String[] args) throws MalformedURLException, IOException, URISyntaxException {
//    if (args.length != 1)
//      usage();

    URI uri = null;
    try {
      File file = new File(args[0]);
      if (file.exists())
        uri = file.toURI();
    }
    catch (Exception e) {
    }
    if (uri == null)
      uri = new URI(args[0]);

    String input = urlContent(uri.toURL());
    Ixml parser = new Ixml(input);
    try {
      parser.parse_ixml();
    }
    catch (ParseException pe) {
      throw new RuntimeException("ParseException while processing " + args[0] + ":\n" + parser.getErrorMessage(pe), pe);
    }

    Grammar grammar = parser.grammar();
    PostProcess.process(grammar);

    grammar = BNF.process(grammar);

    System.out.println(grammar);
  }

  private static void usage() {
    System.err.println("Usage: java " + Blitz.class.getName() + " <FILE>");
    System.err.println();
    System.err.println("  parse an Invisible XML grammar.");
  }

  public static String resourceContent(String resource) throws IOException, MalformedURLException {
    return urlContent(Blitz.class.getClassLoader().getResource(resource));
  }

  public static String fileContent(String filePath) throws URISyntaxException, IOException {
    return urlContent(new java.io.File(filePath).toURI().toURL());
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
}
