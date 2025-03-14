// Copyright (c) 2023-2025 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.grammar;

import de.bottlecaps.markup.blitz.transform.Visitor;

public class Control extends Term {
  private final Occurrence occurrence;
  private final Term term;
  private final Term separator;

  public Control(Occurrence occurrence, Term term, Term separator) {
    this.occurrence = occurrence;
    this.term = term;
    this.separator = separator;
  }

  public Occurrence getOccurrence() {
    return occurrence;
  }

  public Term getTerm() {
    return term;
  }

  public Term getSeparator() {
    return separator;
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Control copy() {
    Term s = separator == null
        ? null
        : separator.copy();
    return new Control(occurrence, term.copy(), s);
  }

  @Override
  public String toString() {
    return term.toString() + occurrence.toString()
         + (separator == null
           ? ""
           : occurrence.toString() + separator.toString());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((occurrence == null) ? 0 : occurrence.hashCode());
    result = prime * result + ((separator == null) ? 0 : separator.hashCode());
    result = prime * result + ((term == null) ? 0 : term.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof Control))
      return false;
    Control other = (Control) obj;
    if (occurrence != other.occurrence)
      return false;
    if (separator == null) {
      if (other.separator != null)
        return false;
    }
    else if (!separator.equals(other.separator))
      return false;
    if (term == null) {
      if (other.term != null)
        return false;
    }
    else if (!term.equals(other.term))
      return false;
    return true;
  }
}