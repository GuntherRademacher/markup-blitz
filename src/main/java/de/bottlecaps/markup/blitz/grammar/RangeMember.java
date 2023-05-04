package de.bottlecaps.markup.blitz.grammar;

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

  private boolean isHex(String value) {
    return value.startsWith("#") && value.length() > 1;
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  private String toString(String value) {
    if (isHex(value))
      return value;
    else
      return "'" + value.replace("'", "''") + "'";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((firstValue == null) ? 0 : firstValue.hashCode());
    result = prime * result + ((lastValue == null) ? 0 : lastValue.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof RangeMember))
      return false;
    RangeMember other = (RangeMember) obj;
    if (firstValue == null) {
      if (other.firstValue != null)
        return false;
    }
    else if (!firstValue.equals(other.firstValue))
      return false;
    if (lastValue == null) {
      if (other.lastValue != null)
        return false;
    }
    else if (!lastValue.equals(other.lastValue))
      return false;
    return true;
  }
}