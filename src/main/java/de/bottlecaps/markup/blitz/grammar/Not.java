// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.grammar;

import java.util.Arrays;
import java.util.stream.Collectors;

import de.bottlecaps.markup.blitz.codepoints.RangeSet;
import de.bottlecaps.markup.blitz.transform.Visitor;

/**
 * A negative lookahead, written as {@code !operand}. It matches the empty string,
 * but only if the remaining input does not start with something that matches the
 * operand. The operand is a string, an encoded character, a character set, or a
 * nonterminal whose rule matches a finite set of fixed sequences of characters.
 */
public class Not extends Term {
  private final Term operand;
  /**
   * The operand, resolved to the set of character sequences that it matches. Each
   * sequence is an array of character sets, one for each character. Calculated by
   * {@link de.bottlecaps.markup.blitz.transform.ResolveNot}.
   */
  private RangeSet[][] sequences;

  public Not(Term operand) {
    this(operand, null);
  }

  private Not(Term operand, RangeSet[][] sequences) {
    this.operand = operand;
    this.sequences = sequences;
  }

  public Term getOperand() {
    return operand;
  }

  public RangeSet[][] getSequences() {
    return sequences;
  }

  public void setSequences(RangeSet[][] sequences) {
    this.sequences = sequences;
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Not copy() {
    return new Not(operand.copy(), sequences);
  }

  @Override
  public String toString() {
    return "!" + operand;
  }

  /**
   * Render the resolved sequences, for diagnostic output.
   *
   * @return the sequences as a string
   */
  public String sequencesToString() {
    if (sequences == null)
      return toString();
    return Arrays.stream(sequences)
        .map(s -> Arrays.stream(s).map(RangeSet::shortName).collect(Collectors.joining(" ")))
        .collect(Collectors.joining(" | ", "!(", ")"));
  }

  @Override
  public int hashCode() {
    return 31 * 7 + operand.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (! (obj instanceof Not))
      return false;
    return operand.equals(((Not) obj).operand);
  }
}
