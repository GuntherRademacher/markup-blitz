// Copyright (c) 2023-2024 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.codepoints;

import de.bottlecaps.markup.blitz.Errors;

public class Range implements Comparable<Range> {
  private final int firstCodepoint;
  private final int lastCodepoint;

  public Range(int firstCodepoint, int lastCodepoint) {
    this.firstCodepoint = firstCodepoint;
    this.lastCodepoint = lastCodepoint;
    if (firstCodepoint > lastCodepoint)
      Errors.S09.thro(this.toString());
  }

  public Range(int codepoint) {
    this(codepoint, codepoint);
  }

  public int getFirstCodepoint() {
    return firstCodepoint;
  }

  public int getLastCodepoint() {
    return lastCodepoint;
  }

  public boolean isSingleton() {
    return firstCodepoint == lastCodepoint;
  }

  public boolean overlaps(Range other) {
    return firstCodepoint <= other.lastCodepoint
        && lastCodepoint >= other.firstCodepoint;
  }

  @Override
  public String toString() {
    return firstCodepoint == lastCodepoint
        ? Codepoint.toString(firstCodepoint)
        : Codepoint.toString(firstCodepoint) + "-" + Codepoint.toString(lastCodepoint);
  }

  public String toJava() {
    return ".add("
        + Codepoint.toJava(firstCodepoint)
        + (firstCodepoint == lastCodepoint ? "" : (", " + Codepoint.toJava(lastCodepoint)))
        + ")";
  }

  public String toREx() {
    if (size() > 1 && "[^-]".indexOf(firstCodepoint) >= 0)
      return new Range(firstCodepoint).toREx() + " | " + new Range(firstCodepoint + 1, lastCodepoint).toREx();
    if (size() > 1 && "[^-]".indexOf(lastCodepoint) >= 0)
      return new Range(firstCodepoint, lastCodepoint - 1).toREx() + " | " + new Range(lastCodepoint).toREx();
    if (size() == 1)
      return ! Codepoint.isAscii(firstCodepoint)
          ? "#x" + Integer.toHexString(firstCodepoint)
          : firstCodepoint == '\''
              ? "\"" + (char) firstCodepoint + "\""
              : "'" + (char) firstCodepoint + "'";
    if (Codepoint.isAscii(firstCodepoint) && Codepoint.isAscii(lastCodepoint))
      return "[" + (char) firstCodepoint
           + "-" + (char) lastCodepoint
           + "]";
    return "[#x" + Integer.toHexString(firstCodepoint)
         + "-#x" + Integer.toHexString(lastCodepoint)
         + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + firstCodepoint;
    result = prime * result + lastCodepoint;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof Range))
      return false;
    Range other = (Range) obj;
    if (firstCodepoint != other.firstCodepoint)
      return false;
    if (lastCodepoint != other.lastCodepoint)
      return false;
    return true;
  }

  @Override
  public int compareTo(Range other) {
    if (firstCodepoint < other.firstCodepoint) return -1;
    if (firstCodepoint > other.firstCodepoint) return  1;
    if ( lastCodepoint < other. lastCodepoint) return -1;
    if ( lastCodepoint > other. lastCodepoint) return  1;
                                               return  0;
  }

  public int size() {
    return lastCodepoint - firstCodepoint + 1;
  }

}