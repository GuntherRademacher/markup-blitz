// Copyright (c) 2023-2024 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.transform;

import java.util.ArrayList;
import java.util.List;

import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Alts;
import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.Control;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Insertion;
import de.bottlecaps.markup.blitz.grammar.Literal;
import de.bottlecaps.markup.blitz.grammar.Node;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.Term;

public class PostProcess extends Visitor {
  private Grammar grammar;
  private Rule rule;
  private Node parent;

  private PostProcess() {
  }

  public static void process(final Grammar grammar) {
    PostProcess pp = new PostProcess();
    pp.grammar = grammar;
    pp.parent = grammar;
    pp.visit(grammar);
  }

  @Override
  public void visit(Rule r) {
    this.rule = r;
    visitPreOrder(r);
    super.visit(r);
    visitPostOrder(r);
  }

  @Override
  public void visit(Alts a) {
    visitPreOrder(a);
    List<Alt> flattenedAlts = new ArrayList<>();
    for (Alt alt : a.getAlts()) {
      if (alt.getTerms().size() != 1 || ! (alt.getTerms().get(0) instanceof Alts)) {
        visit(alt);
        flattenedAlts.add(alt);
      }
      else {
        Alts nestedAlts = (Alts) alt.getTerms().get(0);
        visit(nestedAlts);
        for (Alt nestedAlt : nestedAlts.getAlts()) {
          nestedAlt.setParent(a);
          flattenedAlts.add(nestedAlt);
        }
      }
    }
    a.getAlts().clear();
    a.getAlts().addAll(flattenedAlts);
    visitPostOrder(a);
  }

  @Override
  public void visit(Alt a) {
    visitPreOrder(a);
    List<Term> flattenedTerms = new ArrayList<>();
    for (Term term : a.getTerms()) {
      if (! (term instanceof Alts) || ((Alts) term).getAlts().size() > 1) {
        term.accept(this);
        flattenedTerms.add(term);
      }
      else {
        Alt alt = ((Alts) term).getAlts().get(0);
        visit(alt);
        for (Term t : alt.getTerms()) {
          t.setParent(a);
          flattenedTerms.add(t);
        }
      }
    }
    a.getTerms().clear();
    Term last = null;
    for (Term term : flattenedTerms) {
      if (last != null)
        last.setNext(term);
      last = term;
      a.getTerms().add(term);
    }
    visitPostOrder(a);
  }

  @Override
  public void visit(Charset c) {
    visitPreOrder(c);
    visitPostOrder(c);
  }

  @Override
  public void visit(Control c) {
    visitPreOrder(c);
    c.getTerm().accept(this);
    if (c.getSeparator() != null)
      c.getSeparator().accept(this);
    visitPostOrder(c);
  }

  @Override
  public void visit(Grammar g) {
    visitPreOrder(g);
    for (Rule rule: g.getRules().values())
      rule.accept(this);
    visitPostOrder(g);
  }

  @Override
  public void visit(Insertion i) {
    visitPreOrder(i);
    visitPostOrder(i);
  }

  @Override
  public void visit(Literal l) {
    visitPreOrder(l);
    visitPostOrder(l);
  }

  @Override
  public void visit(Nonterminal n) {
    visitPreOrder(n);
    visitPostOrder(n);
  }

  private void visitPreOrder(Node node) {
    node.setGrammar(grammar);
    node.setRule(rule);
    node.setParent(parent);
    parent = node;
  }

  private void visitPostOrder(Node node) {
    parent = node.getParent();
  }
}
