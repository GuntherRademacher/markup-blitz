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
    else if (isAscii(codePoint))
      return "'" + (char) codePoint + "'";
    else
      return "#" + Integer.toHexString(codePoint);
  }

  public String toJava() {
    return ".add("
        + toJava(firstCodePoint)
        + (firstCodePoint == lastCodePoint ? "" : (", " + toJava(lastCodePoint)))
        + ")";
  }

  public String toREx() {
    if (size() > 1 && "[^-]".indexOf(firstCodePoint) >= 0)
      return new Range(firstCodePoint).toREx() + " | " + new Range(firstCodePoint + 1, lastCodePoint).toREx();
    if (size() > 1 && "[^-]".indexOf(lastCodePoint) >= 0)
      return new Range(firstCodePoint, lastCodePoint - 1).toREx() + " | " + new Range(lastCodePoint).toREx();
    if (size() == 1)
      return ! isAscii(firstCodePoint)
          ? "#x" + Integer.toHexString(firstCodePoint)
          : firstCodePoint == '\''
              ? "\"" + (char) firstCodePoint + "\""
              : "'" + (char) firstCodePoint + "'";
    if (isAscii(firstCodePoint) && isAscii(lastCodePoint))
      return "[" + (char) firstCodePoint
           + "-" + (char) lastCodePoint
           + "]";
    return "[#x" + Integer.toHexString(firstCodePoint)
         + "-#x" + Integer.toHexString(lastCodePoint)
         + "]";
  }

  private String toJava(int codePoint) {
    if (codePoint == '\'')
      return "";
    else if (isAscii(codePoint))
      return "'" + (char) codePoint + "'";
    else
      return "0x" + Integer.toHexString(codePoint);
  }

  public static boolean isAscii(int codePoint) {
    return codePoint >= ' ' && codePoint <= '~';
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

  public int size() {
    return lastCodePoint - firstCodePoint + 1;
  }
}