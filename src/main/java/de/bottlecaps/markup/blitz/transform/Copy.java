// Copyright (c) 2023-2025 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.transform;

import java.util.Stack;

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

/**
 * Copy a grammar.
 */
public class Copy extends Visitor {
  /** Stack for holding items during copying process. */
  protected final Stack<Alts> alts = new Stack<>();
  /** Copied grammar. */
  protected final Grammar copy;

  /**
   *  Constructor.
   * @param g grammar to copy
   */
  protected Copy(Grammar g) {
    this.copy = g;
  }

  /***
   * Copy a grammar.
   * 
   * @param g grammar to copy
   * @return copied grammar
   */
  public static Grammar process(Grammar g) {
    Copy c = new Copy(new Grammar(g));
    c.visit(g);
    PostProcess.process(c.copy);
    return  c.copy;
  }

  @Override
  public void visit(Rule r) {
    super.visit(r);
    copy.addRule(new Rule(r.getMark(), r.getAlias(), r.getName(), alts.pop()));
  }

  @Override
  public void visit(Alts a) {
    boolean topLevel = alts.isEmpty();
    alts.push(new Alts());
    super.visit(a);
    // if not rule level, integrate into enclosing term
    if (! topLevel) {
      Alts nested = alts.pop();
      alts.peek().last().addAlts(nested);
    }
  }

  @Override
  public void visit(Alt a) {
    alts.peek().addAlt(new Alt());
    super.visit(a);
  }

  @Override
  public void visit(Nonterminal n) {
    alts.peek().last().getTerms().add(n.copy());
  }

  @Override
  public void visit(Literal l) {
    alts.peek().last().getTerms().add(l.copy());
  }

  @Override
  public void visit(Insertion i) {
    alts.peek().last().getTerms().add(i.copy());
  }

  @Override
  public void visit(Control c) {
    super.visit(c);
    Term separator = c.getSeparator() == null
        ? null
        : alts.peek().last().removeLast();
    Term term = alts.peek().last().removeLast();
    alts.peek().last().getTerms().add(new Control(c.getOccurrence(), term, separator));
  }

  @Override
  public void visit(Charset c) {
    alts.peek().last().getTerms().add(c);
  }
}
