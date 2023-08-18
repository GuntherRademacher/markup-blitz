package de.bottlecaps.markup.blitz.codepoints;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.bottlecaps.markup.blitz.codepoints.RangeSet.Builder;

public class UnicodeCategoryMap {

  public static void main(String[] args) throws Exception {
    // also see http://www.unicode.org/Public/UCD/latest/ucd/PropertyValueAliases.txt
    String unicodeDataUrl = "http://www.unicode.org/Public/UNIDATA/UnicodeData.txt";
    Map<String, Builder> categoryMap = parseUnicodeData(unicodeDataUrl);
    printCategoryMap(categoryMap);
  }

  private static Map<String, RangeSet.Builder> parseUnicodeData(String url) throws Exception {
    Map<String, RangeSet.Builder> categoryMap = new TreeMap<>();

    URL unicodeDataURL = new URL(url);
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
            int codepoint = Codepoint.of(fields[0]);
            String name = fields[1];
            String categoryName = fields[2];
            if (name.endsWith(", First>")) {
              String lastLine = reader.readLine();
              String[] lastFields = lastLine.split(";");
              int lastCodepoint = Codepoint.of(lastFields[0]);
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

  private static void printCategoryMap(Map<String, Builder> categoryMap) {
    for (Entry<String, Builder> entry : categoryMap.entrySet()) {
      String categoryName = entry.getKey();
      RangeSet codepointRangeSet = entry.getValue().build();
      System.out.println("codepointsByCode.put(\"" + categoryName + "\", " + codepointRangeSet.toJava() + ");");
    }
  }
}
