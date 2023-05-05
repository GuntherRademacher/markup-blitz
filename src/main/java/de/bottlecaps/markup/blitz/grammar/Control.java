package de.bottlecaps.markup.blitz.grammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import de.bottlecaps.markup.blitz.transform.PostProcess;
import de.bottlecaps.markup.blitz.transform.Visitor;

public class Control extends Term {
  private final Occurrence occurrence;
  private final Term term;
  private final Term separator;
  private String listBnfRuleName;

  public Control(Occurrence occurrence, Term term, Term separator) {
    this.occurrence = occurrence;
    this.term = term;
    this.separator = separator;
  }

  public Occurrence getOccurrence() {
    return occurrence;
  }

  public Term getTerm() {
    return term;
  }

  public Term getSeparator() {
    return separator;
  }

  public void setListBnfRuleName(String listBnfRuleName) {
    this.listBnfRuleName = listBnfRuleName;
  }

  public String getListBnfRuleName() {
    return listBnfRuleName;
  }

  @Override
  public Node[] toBnf() {
    Node[] termBnf = term.toBnf();
    termBnf[0].accept(new PostProcess(getGrammar()));
    List<Node> rules = new ArrayList<>();
    Grammar names = getRule().getGrammar();
    switch (occurrence) {
    case ONE_OR_MORE:
      if (separator == null) {
        // e* ==> x where -x: e; x, e.
        String name = "__x";
        Alts alts = new Alts();
        Rule rule = new Rule(Mark.DELETED, name, alts);
        alts.addAlt(new Alt()
            .mergeTerm((Term) termBnf[0].toBnf()[0], rules, names));
        alts.addAlt(new Alt()
            .addNonterminal(Mark.NONE, name)
            .mergeTerm((Term) termBnf[0].toBnf()[0], rules, names));
        return Stream.concat(Stream.concat(Stream.concat(
              Stream.of(new Nonterminal(Mark.NONE, name)),
              Arrays.stream(termBnf).skip(1)),
              Stream.of(rule)),
              rules.stream())
            .toArray(Node[]::new);
      }
      else {
        // e* ==> x where -x: e; x, s, e.
        String name = "__x";
        Node[] separatorBnf = separator == null ? new Node[] {} : separator.toBnf();
        Alts alts = new Alts();
        Rule rule = new Rule(Mark.DELETED, name, alts);
        alts.addAlt(new Alt()
            .mergeTerm((Term) termBnf[0].toBnf()[0], rules, names));
        alts.addAlt(new Alt()
            .addNonterminal(Mark.NONE, name)
            .mergeTerm((Term) separatorBnf[0].toBnf()[0], rules, names)
            .mergeTerm((Term) termBnf[0].toBnf()[0], rules, names));
        return Stream.concat(Stream.concat(Stream.concat(Stream.concat(
              Stream.of(new Nonterminal(Mark.NONE, name)),
              Arrays.stream(termBnf).skip(1)),
              Arrays.stream(separatorBnf).skip(1)),
              Stream.of(rule)),
              rules.stream())
            .toArray(Node[]::new);
      }
    case ZERO_OR_MORE:
      if (separator == null) {
        // e* ==> x where -x: ; x, e.
        String name = "__x";
        Alts alts = new Alts();
        Rule rule = new Rule(Mark.DELETED, name, alts);
        alts.addAlt(new Alt());
        alts.addAlt(new Alt()
            .addNonterminal(Mark.NONE, name)
            .mergeTerm((Term) termBnf[0].toBnf()[0], rules, names));
        return Stream.concat(Stream.concat(Stream.concat(
              Stream.of(new Nonterminal(Mark.NONE, name)),
              Arrays.stream(termBnf).skip(1)),
              Stream.of(rule)),
              rules.stream())
            .toArray(Node[]::new);
      }
      else {
        // e* ==> x where -x: ; x, y. -y: e; y, s, e.
        String name1 = "__x";
        String name2 = "__y";
        Node[] separatorBnf = separator == null ? new Node[] {} : separator.toBnf();
        Alts alts1 = new Alts();
        Rule rule1 = new Rule(Mark.DELETED, name1, alts1);
        alts1.addAlt(new Alt());
        alts1.addAlt(new Alt()
            .addNonterminal(Mark.NONE, name2));
        Alts alts2 = new Alts();
        Rule rule2 = new Rule(Mark.DELETED, name2, alts2);
        alts2.addAlt(new Alt()
            .mergeTerm((Term) termBnf[0].toBnf()[0], rules, names));
        alts2.addAlt(new Alt()
            .addNonterminal(Mark.NONE, name2)
            .mergeTerm((Term) separatorBnf[0].toBnf()[0], rules, names)
            .mergeTerm((Term) termBnf[0].toBnf()[0], rules, names));
        return Stream.concat(Stream.concat(Stream.concat(Stream.concat(Stream.concat(
              Stream.of(new Nonterminal(Mark.NONE, name1)),
              Arrays.stream(termBnf).skip(1)),
              Arrays.stream(separatorBnf).skip(1)),
              Stream.of(rule1)),
              Stream.of(rule2)),
              rules.stream())
            .toArray(Node[]::new);
      }
    case ZERO_OR_ONE:
      // e? ==> x where -x: ; e.
      String name = "__x";
      Alts alts = new Alts();
      Rule rule = new Rule(Mark.DELETED, name, alts);
      alts.addAlt(new Alt());
      alts.addAlt(new Alt().mergeTerm((Term) termBnf[0].toBnf()[0], rules, names));
      return Stream.concat(Stream.concat(Stream.concat(
            Stream.of(new Nonterminal(Mark.NONE, name)),
            Arrays.stream(termBnf).skip(1)),
            Stream.of(rule)),
            rules.stream())
          .toArray(Node[]::new);
    default:
      throw new IllegalArgumentException();
    }
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  @Override
  public String toString() {
    return term.toString() + occurrence.toString()
         + (separator == null
           ? ""
           : occurrence.toString() + separator.toString());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((occurrence == null) ? 0 : occurrence.hashCode());
    result = prime * result + ((separator == null) ? 0 : separator.hashCode());
    result = prime * result + ((term == null) ? 0 : term.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof Control))
      return false;
    Control other = (Control) obj;
    if (occurrence != other.occurrence)
      return false;
    if (separator == null) {
      if (other.separator != null)
        return false;
    }
    else if (!separator.equals(other.separator))
      return false;
    if (term == null) {
      if (other.term != null)
        return false;
    }
    else if (!term.equals(other.term))
      return false;
    return true;
  }
}