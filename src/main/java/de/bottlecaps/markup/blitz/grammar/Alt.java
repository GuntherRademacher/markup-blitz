// Copyright (c) 2023-2024 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.bottlecaps.markup.blitz.transform.Visitor;

public class Alt extends Node {
  private final List<Term> terms;
  // metadata
  private int reductionId;

  public Alt() {
    terms = new ArrayList<>();
  }

  public List<Term> getTerms() {
    return terms;
  }

  public void setReductionId(int reductionId) {
    this.reductionId = reductionId;
  }

  public int getReductionId() {
    return reductionId;
  }

  public Term removeLast() {
    return terms.remove(terms.size() - 1);
  }

  public Alt addNonterminal(Mark mark, String alias, String name) {
    terms.add(new Nonterminal(mark, alias, name));
    return this;
  }

  public void addString(boolean deleted, String value) {
    terms.add(new Literal(deleted, value, false));
  }

  public void addCodepoint(boolean deleted, String value) {
    terms.add(new Literal(deleted, value, true));
  }

  public Alt addCharset(Charset charset) {
    terms.add(charset);
    return this;
  }

  public void addAlts(Alts alts) {
      terms.add(alts);
  }

  public void addControl(Occurrence occurrence, Term term, Term separator) {
    terms.add(new Control(occurrence, term, separator));
  }

  public void addStringInsertion(String string) {
    terms.add(new Insertion(string, false));
  }

  public void addHexInsertion(String hex) {
    terms.add(new Insertion(hex, true));
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Alt copy() {
    Alt alt = new Alt();
    for (Term term : terms)
      alt.getTerms().add(term.copy());
    return alt;
  }

  @Override
  public String toString() {
    return terms.stream().map(Term::toString).collect(Collectors.joining(", "));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((terms == null) ? 0 : terms.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof Alt))
      return false;
    Alt other = (Alt) obj;
    if (terms == null) {
      if (other.terms != null)
        return false;
    }
    else if (!terms.equals(other.terms))
      return false;
    return true;
  }
}
