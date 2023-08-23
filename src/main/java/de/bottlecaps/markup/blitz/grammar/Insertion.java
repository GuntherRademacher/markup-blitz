package de.bottlecaps.markup.blitz.grammar;

import java.util.Arrays;
import java.util.stream.Collectors;

import de.bottlecaps.markup.blitz.codepoints.Codepoint;
import de.bottlecaps.markup.blitz.transform.Visitor;

public class Insertion extends Term {
  private final String value;
  private final boolean isHex;
  private int[] codepoints;
  private final int hashCode;

  public Insertion(int...codepoints) {
    this(null, false, codepoints);
  }

  public Insertion(String value, boolean isHex) {
    this(value, isHex,
         isHex
       ? new int[] {Codepoint.of(value.substring(1))}
       : value.codePoints().toArray());
  }

  private Insertion(String value, boolean isHex, int... codepoints) {
    this.codepoints = codepoints;
    final int prime = 31;
    int h = 1;
    h = prime * h + Arrays.hashCode(codepoints);
    this.hashCode = h;
    this.value = value;
    this.isHex = isHex;
  }

  public int[] getCodepoints() {
    return codepoints;
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  @Override
  public String toString() {
    if (isHex)
      return "+" + value;
    return Arrays.stream(codepoints).mapToObj(codepoint -> {
        if (codepoint == '\'')
          return "+''''";
        if (Codepoint.isAscii(codepoint))
          return "+'" + (char) codepoint + "'";
        return "+#" + Integer.toHexString(codepoint);
      })
      .collect(Collectors.joining(", "))
      .replace("', +'", "");
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (! (obj instanceof Insertion))
      return false;
    Insertion other = (Insertion) obj;
    if (! Arrays.equals(codepoints, other.codepoints))
      return false;
    return true;
  }
}