// Copyright (c) 2023-2025 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.transform;

import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Alts;
import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.Control;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Insertion;
import de.bottlecaps.markup.blitz.grammar.Literal;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.Term;

public abstract class Visitor {
  public void visit(Alt a) {
    for (Term term : a.getTerms())
      term.accept(this);
  }

  public void visit(Alts a) {
    for (Alt alt : a.getAlts())
      alt.accept(this);
  }

  public void visit(Charset c) {
  }

  public void visit(Control c) {
    c.getTerm().accept(this);
    if (c.getSeparator() != null)
      c.getSeparator().accept(this);
  }

  public void visit(Grammar g) {
    for (Rule rule: g.getRules().values())
      rule.accept(this);
  }

  public void visit(Insertion i) {
  }

  public void visit(Literal l) {
  }

  public void visit(Nonterminal n) {
  }

  public void visit(Rule r) {
    r.getAlts().accept(this);
  }
}
