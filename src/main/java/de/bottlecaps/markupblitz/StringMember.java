package de.bottlecaps.markupblitz;

class StringMember extends Member {
  private final boolean isHex;
  private final String value;

  public StringMember(String value, boolean isHex) {
    this.isHex = isHex;
    this.value = value;
  }

  public boolean isHex() {
    return isHex;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return isHex ? value : "'" + value.replace("'", "''") + "'";
  }

  @Override
  public void accept(Visitor v) {
  }
}