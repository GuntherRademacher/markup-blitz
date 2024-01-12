// Copyright (c) 2023-2024 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.basex.core.Context;
import org.basex.io.IO;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.value.node.DBNode;

import de.bottlecaps.markup.Blitz.Option;
import de.bottlecaps.markup.blitz.Parser;
import de.bottlecaps.markup.blitz.codepoints.Range;

public class TestBase {
  private static Map<URL, String> cache = Collections.synchronizedMap(new HashMap<>());
  private static Map<Map.Entry<String, Set<Option>>, Parser> parserCache = Collections.synchronizedMap(new HashMap<>());

  protected static String cachedUrlContent(URL url) {
    return cache.computeIfAbsent(url, u -> {
      try {
        return Blitz.urlContent(u);
      }
      catch (IOException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    });
  }

  protected static String resourceContent(String resource) {
    return cachedUrlContent(TestBase.class.getClassLoader().getResource(resource));
  }

  protected static String fileContent(File file) {
    try {
      return cachedUrlContent(file.toURI().toURL());
    }
    catch (MalformedURLException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  protected static Parser generate(String grammar, Option... blitzOptions) {
    Set<Option> options = Set.of(blitzOptions);
    Map.Entry<String, Set<Option>> key = Map.entry(grammar, options);
    return parserCache.computeIfAbsent(key, k ->
      Blitz.generate(k.getKey(), k.getValue().toArray(Option[]::new))
    );
  }

  protected static Parser generateFromXml(String grammar, Option... blitzOptions) {
    Set<Option> options = Set.of(blitzOptions);
    Map.Entry<String, Set<Option>> key = Map.entry(grammar, options);
    return parserCache.computeIfAbsent(key, k ->
      Blitz.generateFromXml(k.getKey(), k.getValue().toArray(Option[]::new))
    );
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

  private static XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

  protected static boolean isXml(File file) {
    try (FileInputStream fileInputStream = new FileInputStream(file)) {
      XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(fileInputStream);
      try {
        while (reader.hasNext()) {
          if (reader.next() == XMLStreamConstants.START_ELEMENT)
            return true;
        }
        return false;
      }
      finally {
        reader.close();
      }
    }
    catch (XMLStreamException e) {
      return false;
    }
    catch (IOException e) {
      throw new BlitzException("Failed to read file " + file, e);
    }
  }

}
