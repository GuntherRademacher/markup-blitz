package de.bottlecaps.markup;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.basex.core.Context;
import org.basex.io.IO;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.value.node.DBNode;

import de.bottlecaps.markup.blitz.codepoints.Range;

public class TestBase {
  protected static String resourceContent(String resource) {
    URL url = BlitzTest.class.getClassLoader().getResource(resource);
    try {
      return Blitz.urlContent(url);
    }
    catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  protected static String fileContent(File file) {
    try {
      return Blitz.urlContent(file.toURI().toURL());
    }
    catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  protected static List<Range> transformIntegerToBitRanges(int num) {
    List<Range> bitRanges = new ArrayList<>();
    int start = -1;
    int end = -1;
    int bitPosition = 0;
    while (num > 0) {
      if ((num & 1) == 1) {
        if (start == -1)
          start = bitPosition;
        end = bitPosition;
      }
      else if (start != -1) {
        bitRanges.add(new Range(start, end));
        start = -1;
        end = -1;
      }
      num >>= 1;
      ++bitPosition;
    }
    if (start != -1)
      bitRanges.add(new Range(start, end));
    return bitRanges;
  }

  protected boolean deepEqual(String xml1, String xml2) {
    String query =
          "declare variable $xml1 external;\n"
        + "declare variable $xml2 external;\n"
        + "deep-equal($xml1, $xml2)";
    try (QueryProcessor proc = new QueryProcessor(query, new Context())) {
      proc.variable("xml1", new DBNode(IO.get(xml1)));
      proc.variable("xml2", new DBNode(IO.get(xml2)));
      return (boolean) proc.value().toJava();
    }
    catch (IOException | QueryException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public static void main(String[] args) throws Exception {
//    String xml = "<?xml version=\"1.0\" encoding=\"UTF-16\"?><ixml xmlns=\"\"><prolog><version string=\"1.3\"/></prolog><rule name=\"P\"><alt><inclusion><member from=\"B\" to=\"D\"/></inclusion></alt></rule></ixml>";
    String xml = "<x/>";
    System.err.println("without XML declaration: " + new DBNode(IO.get(xml)));
    System.err.println("UTF-8: " + new DBNode(IO.get("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xml)));
    System.err.println("ASCII: " + new DBNode(IO.get("<?xml version=\"1.0\" encoding=\"ASCII\"?>" + xml)));
    System.err.println("UTF-16: "+ new DBNode(IO.get("<?xml version=\"1.0\" encoding=\"UTF-16\"?>" + xml)));
  }
}
