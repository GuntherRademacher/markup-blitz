package de.bottlecaps.markup.blitz.transform;

import java.util.List;

import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Alts;
import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.ClassMember;
import de.bottlecaps.markup.blitz.grammar.Control;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Insertion;
import de.bottlecaps.markup.blitz.grammar.Literal;
import de.bottlecaps.markup.blitz.grammar.Member;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.RangeMember;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.StringMember;
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
    List<Member> members = c.getMembers();
    if (members != null)
      for (Member member : members)
        member.accept(this);
  }

  public void visit(ClassMember c) {
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

  public void visit(RangeMember r) {
  }

  public void visit(Rule r) {
    r.getAlts().accept(this);
  }

  public void visit(StringMember s) {
  }
}
