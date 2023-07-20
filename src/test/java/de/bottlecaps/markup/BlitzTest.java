package de.bottlecaps.markup;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class BlitzTest {
  public static String resourceContent(String resource) throws IOException, MalformedURLException {
    URL url = BlitzTest.class.getClassLoader().getResource(resource);
    return Blitz.urlContent(url);
  }
}
