package de.bottlecaps.markup;

import de.bottlecaps.markup.blitz.Errors;

public class BlitzIxmlException extends BlitzException {
  private static final long serialVersionUID = 1L;

  private Errors error;

  public BlitzIxmlException(Errors error, String message) {
    super(message);
    this.error = error;
  }

  public Errors getError() {
    return error;
  }
}