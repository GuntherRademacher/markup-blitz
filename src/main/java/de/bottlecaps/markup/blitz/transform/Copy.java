package de.bottlecaps.markup.blitz.transform;

import java.util.List;
import java.util.Stack;

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

public class Copy extends Visitor {
  protected List<Member> members;
  protected final Stack<Alts> alts = new Stack<>();
  protected final Grammar copy = new Grammar();

  protected Copy() {
  }

  public static Grammar process(Grammar g) {
    Copy c = new Copy();
    c.visit(g);
    PostProcess.process(c.copy);
    return  c.copy;
  }

  @Override
  public void visit(Rule r) {
    super.visit(r);
    copy.addRule(new Rule(r.getMark(), r.getName(), alts.pop()));
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
    final Charset set;
    List<Member> cMembers = c.getMembers();
    if (cMembers == null) {
      set = new Charset(c.isDeleted(), c.getRangeSet());
    }
    else {
      set = new Charset(c.isDeleted(), c.isExclusion());
      members = set.getMembers();
      for (Member member : cMembers)
        member.accept(this);
    }
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
