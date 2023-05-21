package de.bottlecaps.markup.blitz.transform;

import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Alts;
import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.ClassMember;
import de.bottlecaps.markup.blitz.grammar.Control;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Insertion;
import de.bottlecaps.markup.blitz.grammar.Literal;
import de.bottlecaps.markup.blitz.grammar.Member;
import de.bottlecaps.markup.blitz.grammar.Node;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.RangeMember;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.StringMember;
import de.bottlecaps.markup.blitz.grammar.Term;

public abstract class Visitor {
  public void visit(Alt a) {
    visitPreOrder(a);
    for (Term term : a.getTerms())
      term.accept(this);
    visitPostOrder(a);
  }

  public void visit(Alts a) {
    visitPreOrder(a);
    for (Alt alt : a.getAlts())
      alt.accept(this);
    visitPostOrder(a);
  }

  public void visit(Charset c) {
    visitPreOrder(c);
    for (Member member : c.getMembers())
      member.accept(this);
    visitPostOrder(c);
  }

  public void visit(ClassMember c) {
    visitPreOrder(c);
    visitPostOrder(c);
  }

  public void visit(Control c) {
    visitPreOrder(c);
    c.getTerm().accept(this);
    if (c.getSeparator() != null)
      c.getSeparator().accept(this);
    visitPostOrder(c);
  }

  public void visit(Grammar g) {
    visitPreOrder(g);
    for (Rule rule: g.getRules().values())
      rule.accept(this);
    visitPostOrder(g);
  }

  public void visit(Insertion i) {
    visitPreOrder(i);
    visitPostOrder(i);
  }

  public void visit(Literal l) {
    visitPreOrder(l);
    visitPostOrder(l);
  }

  public void visit(Nonterminal n) {
    visitPreOrder(n);
    visitPostOrder(n);
  }

  public void visit(RangeMember r) {
    visitPreOrder(r);
    visitPostOrder(r);
  }

  public void visit(Rule r) {
    visitPreOrder(r);
    r.getAlts().accept(this);
    visitPostOrder(r);
  }

  public void visit(StringMember s) {
    visitPreOrder(s);
    visitPostOrder(s);
  }

  public void visitPreOrder(Node node) {
  }

  public void visitPostOrder(Node node) {
  }
}
