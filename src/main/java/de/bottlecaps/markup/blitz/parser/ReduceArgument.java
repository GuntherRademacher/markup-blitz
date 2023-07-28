package de.bottlecaps.markup.blitz.parser;

import java.util.Arrays;

import de.bottlecaps.markup.blitz.grammar.Mark;

public class ReduceArgument {
  /** Disposition of removed stack entries. */
  private final Mark marks[];
  /** Text to add by the end. */
  private final int[] insertion;
  /** Nonterminal to create. */
  private final int nonterminalId;

  public ReduceArgument(Mark[] mark, int[] insertion, int nonterminalCode) {
    this.marks = mark;
    this.insertion = insertion;
    this.nonterminalId = nonterminalCode;
  }

  public Mark[] getMarks() {
    return marks;
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
    result = prime * result + ((insertion == null) ? 0 : insertion.hashCode());
    result = prime * result + Arrays.hashCode(marks);
    result = prime * result + nonterminalId;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof ReduceArgument))
      return false;
    ReduceArgument other = (ReduceArgument) obj;
    if (insertion == null) {
      if (other.insertion != null)
        return false;
    }
    else if (!insertion.equals(other.insertion))
      return false;
    if (!Arrays.equals(marks, other.marks))
      return false;
    if (nonterminalId != other.nonterminalId)
      return false;
    return true;
  }

}
