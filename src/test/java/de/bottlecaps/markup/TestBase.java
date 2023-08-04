package de.bottlecaps.markup;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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

}
