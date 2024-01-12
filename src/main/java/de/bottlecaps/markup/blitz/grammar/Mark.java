// Copyright (c) 2023-2024 Gunther Rademacher. Provided under the Apache 2 License.

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
