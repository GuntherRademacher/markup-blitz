// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz;

import org.xml.sax.SAXException;

/**
 * Thrown when a SAX {@link org.xml.sax.ContentHandler} driven through a {@link SaxAdapter}
 * raises a {@link SAXException}. It is intentionally not a
 * {@link de.bottlecaps.markup.BlitzException}, so that a downstream handler failure is not
 * mistaken for an ixml parse error and turned into an error document by {@link Parser#parse}.
 *
 * @author Gunther Rademacher
 */
public class SaxAdapterException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Create an exception wrapping the given SAX exception.
   *
   * @param cause the SAX exception raised by the content handler
   */
  public SaxAdapterException(SAXException cause) {
    super("Caught " + cause.getClass().getSimpleName() + ": " + cause.getMessage(), cause);
  }
}
