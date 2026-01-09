// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.codepoints;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.bottlecaps.markup.blitz.codepoints.RangeSet.Builder;

/**
 * Generate Unicode category map from UnicodeData.txt
 * @see de.bottlecaps.markup.blitz.codepoints.UnicodeCategory#codepointsByCode
 * @see <a href="https://www.unicode.org/Public/UNIDATA/UnicodeData.txt">UnicodeData.txt</a>
 * @see <a href="https://www.unicode.org/Public/17.0.0/ucd/UnicodeData.txt">UnicodeData.txt 16.0.0</a>
 * @see <a href="https://unicode.org/versions/">Unicode versions</a>
 * @see <a href="http://www.unicode.org/Public/UCD/latest/ucd/PropertyValueAliases.txt">PropertyValueAliases</a>
 */
public class UnicodeCategoryMap {

  public static void main(String[] args) throws Exception {
    String unicodeDataUrl = "https://www.unicode.org/Public/17.0.0/ucd/UnicodeData.txt";
    Map<String, Builder> categoryMap = parseUnicodeData(unicodeDataUrl);
    for (Entry<String, Builder> entry : categoryMap.entrySet()) {
      String categoryName = entry.getKey();
      RangeSet codepointRangeSet = entry.getValue().build();
      System.err.println("codepointsByCode.put(\"" + categoryName + "\", " + codepointRangeSet.toJava() + ");");
    }
  }

  private static Map<String, RangeSet.Builder> parseUnicodeData(String url) throws Exception {
    Map<String, RangeSet.Builder> categoryMap = new TreeMap<>();

    URL unicodeDataURL = new URI(url).toURL();
    HttpURLConnection connection = (HttpURLConnection) unicodeDataURL.openConnection();
    connection.setRequestMethod("GET");
    connection.connect();
    try {
      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
        throw new IOException("Failed to fetch UnicodeData.txt. Response code: " + connection.getResponseCode());
      }
      else {
        try (InputStream inputStream = connection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader)) {
          for (String line; (line = reader.readLine()) != null;) {
            String[] fields = line.split(";");
            int codepoint = Codepoint.of(fields[0], false);
            String name = fields[1];
            String categoryName = fields[2];
            if (name.endsWith(", First>")) {
              String lastLine = reader.readLine();
              String[] lastFields = lastLine.split(";");
              int lastCodepoint = Codepoint.of(lastFields[0], false);
              String lastName = lastFields[1];
              String lastCategoryName = lastFields[2];
              if (! lastName.endsWith(", Last>")
               || ! lastCategoryName.equals(categoryName)
               || lastCodepoint <= codepoint) {
                throw new IllegalArgumentException("non-matching range limits:\n" + line + "\n" + lastLine);
              }
              categoryMap.compute(categoryName, (k, v) -> {
                if (v == null)
                  v = RangeSet.builder();
                v.add(codepoint, lastCodepoint);
                return v;
              });
            }
            else {
              categoryMap.compute(categoryName, (k, v) -> {
                if (v == null)
                  v = RangeSet.builder();
                v.add(codepoint);
                return v;
              });
            }
          }
        }
      }
    }
    finally {
      connection.disconnect();
    }
    return categoryMap;
  }

}
