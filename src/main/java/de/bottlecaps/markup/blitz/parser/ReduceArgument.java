// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.parser;

import java.util.Arrays;

import de.bottlecaps.markup.blitz.codepoints.RangeSet;
import de.bottlecaps.markup.blitz.grammar.Mark;

public class ReduceArgument {
  /** Disposition of removed stack entries. */
  private final Mark[] marks;
  /** Name codes of removed stack entries. */
  private final int[] aliases;
  /** Text to add by the end. */
  private final int[] insertion;
  /** Nonterminal to create. */
  private final int nonterminalId;
  /**
   * Negative lookahead: the reduction fails if the remaining input starts with any of
   * these sequences of character sets, null if there is no such lookahead.
   */
  private final RangeSet[][] not;

  public ReduceArgument(Mark[] marks, int[] aliases, int[] insertion, int nonterminalCode) {
    this(marks, aliases, insertion, nonterminalCode, null);
  }

  public ReduceArgument(Mark[] marks, int[] aliases, int[] insertion, int nonterminalCode, RangeSet[][] not) {
    this.marks = marks;
    this.aliases = aliases;
    this.insertion = insertion;
    this.nonterminalId = nonterminalCode;
    this.not = not;
  }

  public RangeSet[][] getNot() {
    return not;
  }

  public Mark[] getMarks() {
    return marks;
  }

  public int[] getAliases() {
    return aliases;
  }

  public int[] getInsertion() {
    return insertion;
  }

  public int getNonterminalId() {
    return nonterminalId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(insertion);
    result = prime * result + Arrays.hashCode(marks);
    result = prime * result + Arrays.hashCode(aliases);
    result = prime * result + nonterminalId;
    result = prime * result + Arrays.deepHashCode(not);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof ReduceArgument))
      return false;
    ReduceArgument other = (ReduceArgument) obj;
    if (!Arrays.equals(insertion, other.insertion))
      return false;
    if (!Arrays.equals(marks, other.marks))
      return false;
    if (!Arrays.equals(aliases, other.aliases))
      return false;
    if (nonterminalId != other.nonterminalId)
      return false;
    if (!Arrays.deepEquals(not, other.not))
      return false;
    return true;
  }

}
