package de.bottlecaps.markup.blitz.grammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.bottlecaps.markup.blitz.transform.PostProcess;
import de.bottlecaps.markup.blitz.transform.Visitor;

public class Alt extends Node {
  private final List<Term> terms;

  public Alt() {
    terms = new ArrayList<>();
  }

  public List<Term> getTerms() {
    return terms;
  }

  public Term removeLast() {
    return terms.remove(terms.size() - 1);
  }

  public Alt addNonterminal(Mark mark, String name) {
    terms.add(new Nonterminal(mark, name));
    return this;
  }

  public void addString(boolean deleted, String value) {
    terms.add(new Literal(deleted, value, false));
  }

  public void addCodePoint(boolean deleted, String value) {
    terms.add(new Literal(deleted, value, true));
  }

  public void addCharSet(CharSet charSet) {
    terms.add(charSet);
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
  public Node[] toBnf() {
    Alt alt = new Alt();
    List<Node> rules = new ArrayList<>();
    terms.forEach(a -> {
      Node[] bnf = a.toBnf();
      bnf[0].accept(new PostProcess(getGrammar()));
      alt.mergeTerm((Term) bnf[0], rules, getGrammar());
      Arrays.stream(bnf)
        .skip(1)
        .forEach(r -> rules.add(r));
    });
    return Stream.concat(Stream.of(alt), rules.stream()).toArray(Node[]::new);
  }

  Alt mergeTerm(Term term, List<Node> rules, Grammar names) {
    if (! (term instanceof Alts)) {
      terms.add(term);
    }
    else if (((Alts) term).alts.size() == 1) {
      for (Term t : ((Alts) term).alts.get(0).terms)
        terms.add(t);
    }
    else {
      // (e1; e2; ...; en) ==> x where -x: e1; e2; ...; en.
      String name = "__x";
      terms.add(new Nonterminal(Mark.NONE, name));
      Alts alts = new Alts();
      for (Alt a : ((Alts) term).alts)
        alts.addAlt(a);
      rules.add(new Rule(Mark.DELETED, name, alts));
    }
    return this;
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
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
