package de.bottlecaps.markupblitz;

class RangeMember extends Member {
  private final String firstValue;
  private final String lastValue;
  private final int firstCodePoint;
  private final int lastCodePoint;

  RangeMember(String firstValue, String lastValue) {
    this.firstValue = firstValue;
    this.lastValue = lastValue;
    firstCodePoint = codePoint(firstValue);
    lastCodePoint = codePoint(lastValue);
  }

  public String getFirstValue() {
    return firstValue;
  }

  public String getLastValue() {
    return lastValue;
  }

  public int getFirstCodePoint() {
    return firstCodePoint;
  }

  public int getLastCodePoint() {
    return lastCodePoint;
  }

  private int codePoint(String firstValue) {
    return isHex(firstValue) ? Integer.parseInt(firstValue.substring(1), 16) : firstValue.codePointAt(0);
  }

  @Override
  public String toString() {
    return toString(firstValue) + "-" + toString(lastValue);
  }

  private String toString(String value) {
    if (isHex(value))
      return value;
    else
      return "'" + value.replace("'", "''") + "'";
  }

  private boolean isHex(String value) {
    return value.startsWith("#") && value.length() > 1;
  }

  @Override
  public void accept(Visitor v) {
  }
}