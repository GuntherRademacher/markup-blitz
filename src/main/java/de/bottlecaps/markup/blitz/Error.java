package de.bottlecaps.markup.blitz;

public enum Error {
  S01("Two rules are not separated by at least one whitespace character or comment."),
  S02("A nonterminal name is not defined by a rule in the grammar."),
  S03("The grammar contains more than one rule for a given nonterminal name."),
  S06("A hex encoding uses any characters not allowed in hexadecimal."),
  S07("The hexadecimal value is not within the Unicode code-point range."),
  S08("An encoded character denotes a Unicode noncharacter or surrogate code point."),
  S09("The first character in a range has a code point value greater than the second character in the range."),
  S10("A Unicode character category is not defined in the Unicode specification."),
  S11("A string contains a line break."),
  S12("The grammar does not conform to the implied or declared version."),

  D01("The parse tree produced by a grammar cannot be represented as well-formed XML."),
  D02("Two or more attributes with the same name would be serialized on the same element."),
  D03("The name of any element or attribute is not a valid XML name."),
  D04("Attempt to serialize as XML any characters that are not permitted in XML."),
  D05("Attempt to serialize an attribute as the root node of an XML document."),
  D06("The parse tree does not contain exactly one top-level element."),
  D07("An attribute named \"xmlns\" appears on an element."),
  ;

  Error(String string) {
    // TODO Auto-generated constructor stub
  }
}
