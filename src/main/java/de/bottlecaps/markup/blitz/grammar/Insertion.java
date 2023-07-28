package de.bottlecaps.markup.blitz.grammar;

import java.util.Arrays;

import de.bottlecaps.markup.blitz.transform.Visitor;

public class Insertion extends Term {
  private final String value;
  private final boolean isHex;
  private int[] codepoints;
  private final int hashCode;

  public Insertion(String value, boolean isHex) {
    this.value = value;
    this.isHex = isHex;
    this.codepoints = isHex
        ? new int[] {Integer.parseInt(value.substring(1), 16)}
        : value.codePoints().toArray();
    final int prime = 31;
    int h = 1;
    h = prime * h + Arrays.hashCode(codepoints);
    this.hashCode = h;
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
    return "+"
         + (isHex ? value : "'" + value.replace("'", "''") + "'");
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