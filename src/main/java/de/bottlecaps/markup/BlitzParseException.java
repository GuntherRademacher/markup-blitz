// Copyright (c) 2023-2024 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup;

public class BlitzParseException extends BlitzException {
  private static final long serialVersionUID = 1L;

  private final String offendingToken;
  private final int line;
  private final int column;

  public BlitzParseException(String message, String offendingToken, int line, int column) {
    super(message);
    this.offendingToken = offendingToken;
    this.line = line;
    this.column = column;
  }

  public String getOffendingToken() {
    return offendingToken;
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

}
