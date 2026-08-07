package de.bottlecaps.markup.blitz;

/**
 * Receives a parse result as serialization-style callbacks.
 *
 * <p>The default {@link Parser#parse(String, de.bottlecaps.markup.blitz.Parser.Option...)} method
 * serializes the parse tree to an XML string. By supplying an implementation of this interface to
 * {@link Parser#parse(String, ResultHandler, de.bottlecaps.markup.blitz.Parser.Option...)}, a caller
 * can instead build a result representation of its choice (for example a tree in a host language)
 * directly from the parse tree, without an intervening serialized form.</p>
 *
 * <p>An element's attributes are reported by {@link #attribute(String, int[], int)},
 * after its {@link #startElement(String)} and before any child content.</p>
 *
 * @author Gunther Rademacher
 */
public interface ResultHandler {
  /**
   * Reports the start of an element.
   * @param name the element name
   */
  void startElement(String name);

  /**
   * Reports the end of an element.
   * @param name the element name
   */
  void endElement(String name);

  /**
   * Reports an attribute, with its value given as codepoints.
   * @param name the attribute name
   * @param codepoints the codepoints holding the attribute value
   * @param length the number of codepoints in the value
   */
  void attribute(String name, int[] codepoints, int length);

  /**
   * Reports a run of codepoints of element content.
   * @param codepoints the Unicode code points
   * @param length the number of codepoints to report
   */
  void text(int[] codepoints, int length);
}
