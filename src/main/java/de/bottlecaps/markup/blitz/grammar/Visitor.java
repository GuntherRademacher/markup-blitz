package de.bottlecaps.markup.blitz.grammar;

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

  public void visit(CharSet c) {
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
