// Copyright (c) 2023-2025 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup;

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
