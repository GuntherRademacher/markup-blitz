package de.bottlecaps.markup.blitz.grammar;

public enum Occurrence {
  ZERO_OR_ONE("?"),
  ZERO_OR_MORE("*"),
  ONE_OR_MORE("+");

  private String string;

  private Occurrence(String string) {
    this.string = string;
  }

  @Override
  public String toString() {
    return string;
  }
}
