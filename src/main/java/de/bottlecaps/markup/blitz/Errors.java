// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz;

import de.bottlecaps.markup.BlitzIxmlException;

/**
 * Invisible XML error codes.
 */
public enum Errors {
  /** S01. */ S01("Two rules are not separated by at least one whitespace character or comment."),
  /** S02. */ S02("A nonterminal name is not defined by a rule in the grammar."),
  /** S03. */ S03("The grammar contains more than one rule for a given nonterminal name."),
  /** S06. */ S06("A hex encoding uses any characters not allowed in hexadecimal."),
  /** S07. */ S07("The hexadecimal value is not within the Unicode code-point range."),
  /** S08. */ S08("An encoded character denotes a Unicode noncharacter or surrogate code point."),
  /** S09. */ S09("The first character in a range has a code point value greater than the second character in the range."),
  /** S10. */ S10("A Unicode character category is not defined in the Unicode specification."),
  /** S11. */ S11("A string contains a C0 or C1 control character, e.g. a line break."),
  /** S12. */ S12("The grammar does not conform to the implied or declared version."),

  /** D01. */ D01("The parse tree produced by a grammar cannot be represented as well-formed XML."),
  /** D02. */ D02("Two or more attributes with the same name would be serialized on the same element."),
  /** D03. */ D03("The name of any element or attribute is not a valid XML name."),
  /** D04. */ D04("Attempt to serialize as XML any characters that are not permitted in XML."),
  /** D05. */ D05("Attempt to serialize an attribute as the root node of an XML document."),
  /** D06. */ D06("The parse tree does not contain exactly one top-level element."),
  /** D07. */ D07("An attribute named \"xmlns\" appears on an element."),
  ;

  /** Error message text. */
  private String text;

  /**
   * Constructor.
   * @param text error message text
   */
  Errors(String text) {
    this.text = text;
  }

  /** 
   * Throw error message with arguments.
   * @param args arguments
   */
  public void thro(String... args) {
    String msg = args.length == 0
        ? text
        : text.replaceFirst("\\.$", ": ") + String.join(", ", args) + ".";
    throw new BlitzIxmlException(this, "[" + name() + "] " + msg);
  }
}
