package de.bottlecaps.markup.blitz.character;

public class Range implements Comparable<Range> {
  private final int firstCodePoint;
  private final int lastCodePoint;

  public Range(int firstCodePoint, int lastCodePoint) {
    this.firstCodePoint = firstCodePoint;
    this.lastCodePoint = lastCodePoint;
    if (firstCodePoint > lastCodePoint)
      throw new IllegalArgumentException("invalid range: " + this); // TODO: error message
  }

  public Range(int codePoint) {
    this(codePoint, codePoint);
  }

  public int getFirstCodePoint() {
    return firstCodePoint;
  }

  public int getLastCodePoint() {
    return lastCodePoint;
  }

  public boolean overlaps(Range other) {
    return firstCodePoint <= other.lastCodePoint
        && lastCodePoint >= other.firstCodePoint;
  }

  @Override
  public String toString() {
    return firstCodePoint == lastCodePoint
        ? toString(firstCodePoint)
        : toString(firstCodePoint) + "-" + toString(lastCodePoint);
  }

  private String toString(int codePoint) {
    if (codePoint == '\'')
      return "\"'\"";
    else if (codePoint >= ' ' && codePoint <= '~')
      return "'" + (char) codePoint + "'";
    else
      return "#" + Integer.toHexString(codePoint);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + firstCodePoint;
    result = prime * result + lastCodePoint;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof Range))
      return false;
    Range other = (Range) obj;
    if (firstCodePoint != other.firstCodePoint)
      return false;
    if (lastCodePoint != other.lastCodePoint)
      return false;
    return true;
  }

  @Override
  public int compareTo(Range other) {
    if (firstCodePoint < other.firstCodePoint) return -1;
    if (firstCodePoint > other.firstCodePoint) return  1;
    if ( lastCodePoint < other. lastCodePoint) return -1;
    if ( lastCodePoint > other. lastCodePoint) return  1;
                                               return  0;
  }
}