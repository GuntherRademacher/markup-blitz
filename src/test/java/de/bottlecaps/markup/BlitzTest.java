package de.bottlecaps.markup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import de.bottlecaps.markup.blitz.parser.Parser;

public class BlitzTest {
  public static String resourceContent(String resource) {
    URL url = BlitzTest.class.getClassLoader().getResource(resource);
    try {
      return Blitz.urlContent(url);
    }
    catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  @Test
  public void testAddress() {
    Parser parser = Blitz.generate(resourceContent("address.ixml"));
    String xml = parser.parse(resourceContent("address.input"), BlitzOption.INDENT);
    assertEquals(resourceContent("address.xml"), xml);
  }
}
