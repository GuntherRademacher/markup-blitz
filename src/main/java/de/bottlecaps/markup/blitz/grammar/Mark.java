package de.bottlecaps.markup.blitz.grammar;

public enum Mark {
  NODE("^"),
  ATTRIBUTE("@"),
  DELETE("-"),
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
