package de.bottlecaps.markup.blitz.transform;

import java.util.List;
import java.util.Stack;

import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Alts;
import de.bottlecaps.markup.blitz.grammar.CharSet;
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

public class Copy extends Visitor {
  protected List<Member> members;
  protected Stack<Alts> alts = new Stack<>();
  protected Grammar copy;

  public Copy(Grammar g) {
    visit(g);
  }

  public Grammar get() {
    return copy;
  }

  @Override
  public void visit(Grammar g) {
    copy = new Grammar();
    super.visit(g);
    new PostProcess(copy).visit(copy);
  }

  @Override
  public void visit(Rule r) {
    super.visit(r);
    copy.addRule(new Rule(r.getMark(), r.getName(), alts.pop()));
  }

  @Override
  public void visit(Alts a) {
    alts.push(new Alts());
    super.visit(a);
    if (alts.size() != 1) {
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
  public void visit(CharSet c) {
    CharSet set = new CharSet(c.isDeleted(), c.isExclusion());
    members = set.getMembers();
    for (Member member : c.getMembers())
      member.accept(this);
    alts.peek().last().getTerms().add(set);
  }

  @Override
  public void visit(StringMember s) {
    members.add(s.copy());
  }

  @Override
  public void visit(RangeMember r) {
    members.add(r.copy());
  }

  @Override
  public void visit(ClassMember c) {
    members.add(c.copy());
  }
}
