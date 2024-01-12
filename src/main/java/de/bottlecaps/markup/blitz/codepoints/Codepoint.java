// Copyright (c) 2023-2024 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.codepoints;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.bottlecaps.markup.blitz.Errors;

public class Codepoint {
  private static Pattern hexPattern = Pattern.compile("^0*(0|(?:[1-9A-Fa-f][0-9A-Fa-f]*))$");

  public static int of(String hex) {
    return of(hex, true);
  }

  static int of(String hex, boolean validate) {
    Matcher matcher = hexPattern.matcher(hex);
    if (! matcher.find())
      Errors.S06.thro(hex); // invalid characters
    String stripped = matcher.group(1);
    if (stripped.length() > 6)
      Errors.S07.thro(hex); // out of Unicode character range
    final int codepoint = Integer.parseInt(hex, 16);
    if (validate) {
      if (codepoint > Character.MAX_CODE_POINT)
        Errors.S07.thro(hex); // out of Unicode character range
      if (! UnicodeCategory.ALPHABET.containsCodepoint(codepoint) || UnicodeCategory.isSurrogate(codepoint))
        Errors.S08.thro(hex); // noncharacter or surrogate
    }
    return codepoint;
  }

  public static boolean isAscii(int codepoint) {
    return codepoint >= ' ' && codepoint <= '~';
  }

  public static String toJava(int codepoint) {
    if (codepoint == '\'')
      return "'\\''";
    else if (codepoint == '\\')
      return "'\\\\'";
    else if (isAscii(codepoint))
      return "'" + (char) codepoint + "'";
    else
      return "0x" + Integer.toHexString(codepoint);
  }

  public static String toString(int codepoint) {
    if (codepoint == '\'')
      return "\"'\"";
    else if (isAscii(codepoint))
      return "'" + (char) codepoint + "'";
    else
      return "#" + Integer.toHexString(codepoint);
  }

}
