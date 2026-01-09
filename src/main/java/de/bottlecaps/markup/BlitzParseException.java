// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup;

public class BlitzParseException extends BlitzException {
  private static final long serialVersionUID = 1L;

  private final String offendingToken;
  private final String[] expectedTokens;
  private final int line;
  private final int column;

  public BlitzParseException(String message, String offendingToken, String[] expectedTokens, int line, int column) {
    super(message);
    this.offendingToken = offendingToken;
    this.expectedTokens = expectedTokens;
    this.line = line;
    this.column = column;
  }

  public String getOffendingToken() {
    return offendingToken;
  }

  public String[] getExpectedTokens() {
    return expectedTokens;
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

}
