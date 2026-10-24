// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A {@link ResultHandler} that forwards a parse result as namespace-aware SAX
 * {@link ContentHandler} events, so that the result can be consumed by any standard
 * XML machinery (DOM or JDOM/dom4j construction, XSLT, validation, serialization, ...).
 *
 * <p>Namespace declarations (attributes named {@code xmlns} or {@code xmlns:*}) are
 * consumed and reported as {@link ContentHandler#startPrefixMapping(String, String)} /
 * {@link ContentHandler#endPrefixMapping(String)} events rather than as attributes;
 * element and attribute names are resolved to their namespace URI and local name.
 * A single {@link ContentHandler#startDocument()} / {@link ContentHandler#endDocument()}
 * pair brackets the one document element.</p>
 *
 * <p>Because attributes are reported by {@link ResultHandler} after the enclosing
 * {@link #startElement(String)} but SAX expects them with the start tag, the start tag is
 * deferred until the first following event that is not an attribute.</p>
 */
public class SaxAdapter implements ResultHandler {
  private static final String XMLNS = "xmlns";
  private static final String XMLNS_PREFIX = "xmlns:";

  private final ContentHandler contentHandler;
  private final NamespaceSupport namespaceSupport = new NamespaceSupport();
  private final List<String> pendingAttributes = new ArrayList<>();
  private int depth;
  private boolean startPending;
  private String pendingName;

  /**
   * Create an adapter forwarding to the given content handler.
   *
   * @param contentHandler the SAX content handler receiving the events
   */
  public SaxAdapter(ContentHandler contentHandler) {
    this.contentHandler = contentHandler;
  }

  @Override
  public void startElement(String name) {
    try {
      flushStart();
      if (depth++ == 0)
        contentHandler.startDocument();
      pendingName = name;
      pendingAttributes.clear();
      startPending = true;
    }
    catch (SAXException e) {
      throw new SaxAdapterException(e);
    }
  }

  @Override
  public void attribute(String name, int[] codepoints, int length) {
    pendingAttributes.add(name);
    pendingAttributes.add(new String(codepoints, 0, length));
  }

  @Override
  public void text(int[] codepoints, int length) {
    try {
      flushStart();
      char[] chars = new String(codepoints, 0, length).toCharArray();
      contentHandler.characters(chars, 0, chars.length);
    }
    catch (SAXException e) {
      throw new SaxAdapterException(e);
    }
  }

  @Override
  public void endElement(String name) {
    try {
      flushStart();
      String[] parts = processName(name, false);
      contentHandler.endElement(parts[0], parts[1], parts[2]);
      for (Enumeration<String> e = namespaceSupport.getDeclaredPrefixes(); e.hasMoreElements(); )
        contentHandler.endPrefixMapping(e.nextElement());
      namespaceSupport.popContext();
      if (--depth == 0)
        contentHandler.endDocument();
    }
    catch (SAXException e) {
      throw new SaxAdapterException(e);
    }
  }

  private void flushStart() throws SAXException {
    if (! startPending)
      return;
    startPending = false;
    namespaceSupport.pushContext();
    // pass 1: declare the namespaces introduced by this element
    for (int i = 0; i < pendingAttributes.size(); i += 2) {
      String name = pendingAttributes.get(i);
      if (name.equals(XMLNS))
        namespaceSupport.declarePrefix("", pendingAttributes.get(i + 1));
      else if (name.startsWith(XMLNS_PREFIX))
        namespaceSupport.declarePrefix(name.substring(XMLNS_PREFIX.length()), pendingAttributes.get(i + 1));
    }
    for (Enumeration<String> e = namespaceSupport.getDeclaredPrefixes(); e.hasMoreElements(); ) {
      String prefix = e.nextElement();
      contentHandler.startPrefixMapping(prefix, namespaceSupport.getURI(prefix));
    }
    // pass 2: resolve the remaining attributes, now that all prefixes are in scope
    AttributesImpl attributes = new AttributesImpl();
    for (int i = 0; i < pendingAttributes.size(); i += 2) {
      String name = pendingAttributes.get(i);
      if (name.equals(XMLNS) || name.startsWith(XMLNS_PREFIX))
        continue;
      String[] parts = processName(name, true);
      attributes.addAttribute(parts[0], parts[1], parts[2], "CDATA", pendingAttributes.get(i + 1));
    }
    String[] parts = processName(pendingName, false);
    contentHandler.startElement(parts[0], parts[1], parts[2], attributes);
  }

  private String[] processName(String qName, boolean isAttribute) {
    String[] parts = namespaceSupport.processName(qName, new String[3], isAttribute);
    if (parts != null)
      return parts;
    // undeclared prefix: keep the qualified name, without a namespace
    int colon = qName.indexOf(':');
    return new String[] {"", colon < 0 ? qName : qName.substring(colon + 1), qName};
  }
}
