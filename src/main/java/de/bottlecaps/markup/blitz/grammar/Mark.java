package de.bottlecaps.markup.blitz.grammar;

public enum Mark {
  ELEMENT("^"),
  ATTRIBUTE("@"),
  DELETED("-"),
  NONE("");

  private String string;

  private Mark(String string) {
    this.string = string;
  }

  @Override
  public String toString() {
    return string;
  }
}
