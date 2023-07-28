package de.bottlecaps.markup.blitz.grammar;

import java.util.Arrays;

import de.bottlecaps.markup.blitz.transform.Visitor;

public class Literal extends Term {
  private final boolean deleted;
  private final String value;
  private final boolean isHex;
  private int[] codepoints;
  private final int hashCode;

  public Literal(boolean deleted, String value, boolean isHex) {
    this.deleted = deleted;
    this.value = value;
    this.isHex = isHex;
    this.codepoints = isHex
                    ? new int[] {Integer.parseInt(value.substring(1), 16)}
                    : value.codePoints().toArray();
    final int prime = 31;
    int h = 1;
    h = prime * h + Arrays.hashCode(codepoints);
    h = prime * h + (deleted ? 1231 : 1237);
    this.hashCode = h;
  }

  public boolean isDeleted() {
    return deleted;
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
    return
        (deleted ? "-" : "") +
        (isHex ? value : "'" + value.replace("'", "''") + "'");
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (! (obj instanceof Literal))
      return false;
    Literal other = (Literal) obj;
    if (deleted != other.deleted)
      return false;
    if (! Arrays.equals(codepoints, other.codepoints))
      return false;
    return true;
  }
}