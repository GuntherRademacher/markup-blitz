package de.bottlecaps.markup.blitz;

public class BlitzException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public BlitzException(String message, Throwable throwable) {
    super(message, throwable);
  }

  public BlitzException(String message) {
    super(message);
  }

  public BlitzException(Throwable throwable) {
    super(
        "Caught " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage(),
        throwable);
  }
}
