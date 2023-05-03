package de.bottlecaps.markupblitz;

public class Literal extends Term {
  protected final boolean deleted;
  protected final String value;
  protected final Boolean isHex;

  public boolean isDeleted() {
    return deleted;
  }

  public String getValue() {
    return value;
  }

  public Boolean getIsHex() {
    return isHex;
  }

  public Literal(boolean deleted, String value, boolean isHex) {
    this.deleted = deleted;
    this.value = value;
    this.isHex = isHex;
  }

  @Override
  public String toString() {
    return
        (deleted ? "-" : "") +
        (isHex ? value : "'" + value.replace("'", "''") + "'");
  }

  @Override
  public void accept(Visitor v) {
  }
}